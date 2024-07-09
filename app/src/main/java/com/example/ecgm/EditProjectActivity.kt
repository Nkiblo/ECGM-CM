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
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class EditProjectActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var selectImageButton: Button
    private lateinit var selectManagerButton: Button
    private lateinit var updateButton: Button
    private lateinit var projectImageView: ImageView
    private lateinit var managerImageView: ImageView
    private lateinit var managerTextView: TextView
    private lateinit var db: FirebaseFirestore
    private lateinit var storageRef: StorageReference

    private var selectedImageUri: Uri? = null
    private var selectedManagerId: String = ""
    private var selectedManagerImageUrl: String = ""

    private lateinit var projectId: String
    private lateinit var originalProject: Project

    private val USER_SELECTION_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_project)

        db = FirebaseFirestore.getInstance()
        storageRef = FirebaseStorage.getInstance().reference

        // Initialize views
        nameEditText = findViewById(R.id.nameEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        selectImageButton = findViewById(R.id.selectImageButton)
        selectManagerButton = findViewById(R.id.selectManagerButton)
        updateButton = findViewById(R.id.updateButton)
        projectImageView = findViewById(R.id.projectImageView)
        managerImageView = findViewById(R.id.managerImageView)
        managerTextView = findViewById(R.id.managerTextView)

        // Retrieve project details from Intent
        originalProject = intent.getSerializableExtra("project") as Project
        projectId = originalProject.id // Store the project ID being edited

        // Populate existing project details into UI
        nameEditText.setText(originalProject.name)
        descriptionEditText.setText(originalProject.description)
        Glide.with(this)
            .load(originalProject.imageUrl)
            .into(projectImageView)

        // Initialize selectedManagerId with the current managerId from originalProject
        selectedManagerId = originalProject.managerId

        // Fetch and display current manager details
        fetchManagerDetails(selectedManagerId)

        // Set click listeners
        selectImageButton.setOnClickListener {
            openImagePicker()
        }

        selectManagerButton.setOnClickListener {
            openUserSelection()
        }

        updateButton.setOnClickListener {
            updateProject(originalProject)
        }
    }

    private fun fetchManagerDetails(managerId: String) {
        Log.d("EditProjectActivity", "Fetching manager details for managerId: $managerId")
        // Query the "users" collection to find the document where "id" matches managerId
        db.collection("users")
            .whereEqualTo("id", managerId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0] // Assuming there's only one document
                    val managerName = document.getString("name") ?: "Unknown Manager"
                    val managerImageUrl = document.getString("profileImageUrl")

                    // Display manager details on UI
                    managerTextView.text = managerName
                    if (!managerImageUrl.isNullOrEmpty()) {
                        Log.d("EditProjectActivity", "Manager image URL: $managerImageUrl")
                        Glide.with(this)
                            .load(managerImageUrl)
                            .into(managerImageView)
                        selectedManagerImageUrl = managerImageUrl // Update selectedManagerImageUrl
                    } else {
                        Log.d("EditProjectActivity", "Manager image URL is empty or null")
                        // Load placeholder image if managerImageUrl is null or empty
                        Glide.with(this)
                            .load(R.drawable.ic_launcher_background)
                            .into(managerImageView)
                        selectedManagerImageUrl = "" // Ensure selectedManagerImageUrl is cleared
                    }
                } else {
                    Log.d("EditProjectActivity", "No document found for managerId: $managerId")
                    // Handle case where manager document is not found
                }
            }
            .addOnFailureListener { e ->
                Log.e("EditProjectActivity", "Error fetching manager details", e)
                // Handle error fetching manager details
            }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, USER_SELECTION_REQUEST)
    }

    private fun openUserSelection() {
        // Start UserSelectionActivity to select a new manager
        val intent = Intent(this, UserSelectionActivity::class.java)
        startActivityForResult(intent, USER_SELECTION_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == USER_SELECTION_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.getStringExtra("selectedManagerId")?.let { managerId ->
                selectedManagerId = managerId
                fetchManagerDetails(selectedManagerId) // Update UI with new manager details
            }

            data?.getStringExtra("selectedManagerProfileImageUrl")?.let { imageUrl ->
                selectedManagerImageUrl = imageUrl
                Glide.with(this)
                    .load(selectedManagerImageUrl)
                    .into(managerImageView)
            }

            // Handle result from image picker (selecting a new image)
            if (data != null && data.data != null) {
                selectedImageUri = data.data
                Glide.with(this)
                    .load(selectedImageUri)
                    .into(projectImageView)
            }
        }
    }


    private fun updateProject(project: Project) {
        val updatedName = nameEditText.text.toString().trim()
        val updatedDescription = descriptionEditText.text.toString().trim()

        if (updatedName.isEmpty() || updatedDescription.isEmpty()) {
            // Handle case where required fields are not filled
            Log.e("EditProjectActivity", "Required fields are empty")
            return
        }

        // Update project object with new data
        project.name = updatedName
        project.description = updatedDescription

        // Update managerId with the newly selected manager
        val previousManagerId = project.managerId // Store previous managerId
        project.managerId = selectedManagerId

        // Update projectUsers array to replace the previousManagerId with selectedManagerId
        if (project.projectUsers.isNotEmpty()) {
            // Update the first element of projectUsers array if it's not empty
            project.projectUsers = project.projectUsers.mapIndexed { index, userId ->
                if (index == 0) selectedManagerId else userId
            }
        } else {
            // If projectUsers is empty, initialize it with selectedManagerId
            project.projectUsers = mutableListOf(selectedManagerId)
        }

        // Update UI with new manager details
        fetchManagerDetails(selectedManagerId) // Fetch manager details and update UI

        // If an image is selected, upload it to Firebase Storage
        if (selectedImageUri != null) {
            uploadImageToStorage(project, previousManagerId)
        } else {
            saveUpdatedProject(project)
        }
    }



    private fun uploadImageToStorage(project: Project, previousManagerId: String? = null) {
        // Create a reference to 'projects/projectID.jpg'
        val imageRef = storageRef.child("projects/$projectId.jpg")

        // Upload file to Firebase Storage
        val uploadTask = imageRef.putFile(selectedImageUri!!)

        // Register observers to listen for upload success or failure
        uploadTask.addOnSuccessListener {
            Log.d("EditProjectActivity", "Image uploaded successfully")

            // Get the downloadable URL of the uploaded image
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                project.imageUrl = uri.toString() // Update project image URL

                // Here you can use previousManagerId if needed
                if (previousManagerId != null) {
                    // Do something with previousManagerId
                }

                // Save updated project data
                saveUpdatedProject(project)
            }.addOnFailureListener { e ->
                Log.e("EditProjectActivity", "Error getting image URL", e)
            }
        }.addOnFailureListener { e ->
            // Handle unsuccessful uploads
            Log.e("EditProjectActivity", "Error uploading image", e)
        }
    }


    private fun saveUpdatedProject(project: Project) {
        // Query for the document that contains the specific project ID
        db.collection("projects")
            .whereEqualTo("id", projectId) // Assuming "id" is the field name in Firestore
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    // Update the found document with the new project data
                    db.collection("projects")
                        .document(document.id) // Use the document ID to update existing document
                        .set(project)
                        .addOnSuccessListener {
                            // Successfully updated project
                            Log.d("EditProjectActivity", "Project updated successfully")

                            // Return the updated project data to ProjectActivity
                            val resultIntent = Intent()
                            resultIntent.putExtra("updatedProject", project)
                            setResult(Activity.RESULT_OK, resultIntent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            // Handle error updating project
                            Log.e("EditProjectActivity", "Error updating project", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                // Handle error fetching document
                Log.e("EditProjectActivity", "Error getting documents", e)
            }
    }



}
