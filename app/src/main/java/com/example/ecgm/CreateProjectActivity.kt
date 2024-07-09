package com.example.ecgm

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.UUID

class CreateProjectActivity : AppCompatActivity() {

    // UI elements
    private lateinit var nameEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var selectImageButton: Button
    private lateinit var selectManagerButton: Button
    private lateinit var createButton: Button
    private lateinit var projectImageView: ImageView
    private lateinit var managerImageView: ImageView
    private lateinit var managerTextView: TextView

    // Firebase references
    private lateinit var db: FirebaseFirestore
    private lateinit var storageRef: StorageReference

    // Selected image and manager data
    private var selectedImageUri: Uri? = null
    private var selectedManagerId: String = ""
    private val USER_SELECTION_REQUEST = 1001

    // Activity result launcher for image picker
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_project)

        // Initialize Firebase Firestore and Storage
        db = Firebase.firestore
        storageRef = FirebaseStorage.getInstance().reference

        // Initialize UI elements
        nameEditText = findViewById(R.id.nameEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        selectImageButton = findViewById(R.id.selectImageButton)
        selectManagerButton = findViewById(R.id.selectManagerButton)
        createButton = findViewById(R.id.createButton)
        projectImageView = findViewById(R.id.projectImageView)
        managerImageView = findViewById(R.id.managerImageView)
        managerTextView = findViewById(R.id.managerTextView)

        // Set click listeners
        selectImageButton.setOnClickListener {
            openImagePicker()
        }

        selectManagerButton.setOnClickListener {
            openUserSelection()
        }

        createButton.setOnClickListener {
            Log.d("CreateProjectActivity", "Button clicked")
            val name = nameEditText.text.toString().trim()
            val description = descriptionEditText.text.toString().trim()

            // Check if required fields are filled
            if (name.isEmpty() || description.isEmpty() || selectedImageUri == null || selectedManagerId.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create Project object
            val project = Project(
                name = name,
                description = description,
                imageUrl = "",
                managerId = selectedManagerId,
                tasks = emptyList()
            )

            // Get and save project with sequential ID
            getNextProjectId { nextId ->
                project.id = nextId
                uploadImageToStorage(project)
            }
        }

        // Setup image picker launcher
        setupImagePicker()
    }

    // Setup image picker launcher
    private fun setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageUri = result.data?.data
                Log.d("CreateProjectActivity", "Selected image URI: $selectedImageUri")

                // Display selected image in ImageView
                projectImageView.setImageURI(selectedImageUri)
                projectImageView.visibility = ImageView.VISIBLE
            }
        }
    }

    // Open image picker
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    // Open user selection activity
    private fun openUserSelection() {
        val intent = Intent(this, UserSelectionActivity::class.java)
        startActivityForResult(intent, USER_SELECTION_REQUEST)
    }

    // Handle result from user selection and image picker activities
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == USER_SELECTION_REQUEST && resultCode == RESULT_OK) {
            val selectedUserId = data?.getStringExtra("selectedUserId")
            val selectedUserName = data?.getStringExtra("selectedUserName")
            val selectedUserProfileImageUrl = data?.getStringExtra("selectedUserProfileImageUrl")

            // Update UI with selected user details
            selectedManagerId = selectedUserId ?: ""
            managerTextView.text = selectedUserName ?: ""
            Glide.with(this)
                .load(selectedUserProfileImageUrl)
                .into(managerImageView)
        }
    }

    // Upload selected image to Firebase Storage
    private fun uploadImageToStorage(project: Project) {
        val projectId = UUID.randomUUID().toString() // Unique ID for the project
        val projectImageRef = storageRef.child("project_images/${projectId}")

        projectImageRef.putFile(selectedImageUri!!)
            .addOnSuccessListener {
                projectImageRef.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    Log.d("CreateProjectActivity", "Image URL from Storage: $imageUrl")

                    // Update Project object with image URL
                    project.imageUrl = imageUrl

                    // Save project to Firestore
                    saveProject(project)
                }
            }
            .addOnFailureListener { e ->
                Log.e("CreateProjectActivity", "Failed to upload image: ${e.message}", e)
                Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
    }

    // Get next sequential project ID
    private fun getNextProjectId(callback: (String) -> Unit) {
        val docRef = db.collection("metadata").document("project_counter")

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentId = snapshot.getLong("last_id") ?: 0

            // Initialize document if it doesn't exist
            if (!snapshot.exists() || currentId == 0L) {
                transaction.set(docRef, mapOf("last_id" to 0))
            }

            // Increment current ID
            val nextId = currentId + 1
            transaction.update(docRef, "last_id", nextId)

            nextId.toString() // Return next ID as String
        }.addOnSuccessListener { nextId ->
            callback(nextId.toString())
        }.addOnFailureListener { e ->
            Log.e("CreateProjectActivity", "Error getting next project ID", e)
        }
    }

    // Save project to Firestore
    private fun saveProject(project: Project) {
        db.collection("projects")
            .add(project)
            .addOnSuccessListener { documentReference ->
                val projectId = documentReference.id // Get auto-generated document ID
                Log.d("CreateProjectActivity", "Project saved successfully with ID: $projectId")

                // Update project object with generated ID
                project.id = projectId

                // Update document with generated ID as its name
                db.collection("projects").document(projectId)
                    .set(project)
                    .addOnSuccessListener {
                        Log.d("CreateProjectActivity", "Document updated with ID as name: $projectId")
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e("CreateProjectActivity", "Error updating document with ID as name", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("CreateProjectActivity", "Error saving project", e)
                Toast.makeText(this, "Failed to save project", Toast.LENGTH_SHORT).show()
            }
    }
}
