package com.example.ecgm

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class EditUserActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var storageRef: StorageReference
    private lateinit var profileImageView: ImageView
    private lateinit var nameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var deleteButton: Button

    private var userId: String? = null
    private var documentId: String? = null
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_user)

        // Initialize Firebase Firestore and Storage references
        db = FirebaseFirestore.getInstance()
        storageRef = FirebaseStorage.getInstance().reference

        // Initialize UI elements
        profileImageView = findViewById(R.id.profileImageView)
        nameEditText = findViewById(R.id.nameEditText)
        usernameEditText = findViewById(R.id.usernameEditText)
        emailEditText = findViewById(R.id.editTextEmail)
        saveButton = findViewById(R.id.saveButton)
        deleteButton = findViewById(R.id.deleteButton)

        // Retrieve user ID passed from intent
        userId = intent.getStringExtra("userId")

        // Load user data if userId is not null
        userId?.let {
            loadUserData(it)
        }

        // Set click listener for selecting profile image
        profileImageView.setOnClickListener {
            choosePhoto()
        }

        // Set click listener for save button
        saveButton.setOnClickListener {
            saveUserData()
        }

        // Set click listener for delete button
        deleteButton.setOnClickListener {
            deleteUserData()
        }
    }

    private fun loadUserData(userId: String) {
        // Load user data from Firestore based on userId
        db.collection("users")
            .whereEqualTo("id", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.documents.isNotEmpty()) {
                    val document = querySnapshot.documents[0]
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        // Populate UI fields with user data
                        nameEditText.setText(user.name)
                        usernameEditText.setText(user.username)
                        emailEditText.setText(user.email)
                        this.documentId = document.id // Store the document ID

                        // Load profile image using Glide if available
                        user.profileImageUrl?.let { url ->
                            Glide.with(this).load(url).into(profileImageView)
                        }
                    }
                } else {
                    Log.e("EditUserActivity", "No user found with id: $userId")
                }
            }
            .addOnFailureListener { e ->
                Log.e("EditUserActivity", "Error loading user data", e)
            }
    }

    private fun saveUserData() {
        // Retrieve user input from UI fields
        val name = nameEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()

        // Validate user input
        if (name.isEmpty() || username.isEmpty() || email.isEmpty()) {
            Log.e("EditUserActivity", "Name, username, or email is empty")
            return
        }

        // Prepare updates for Firestore document
        val userUpdates = mutableMapOf<String, Any>(
            "name" to name,
            "username" to username,
            "email" to email
        )

        // Upload profile image if selectedImageUri is not null
        if (selectedImageUri != null) {
            val profileImageRef = storageRef.child("profile_pictures/${userId}.jpg")
            val uploadTask = profileImageRef.putFile(selectedImageUri!!)

            // Continue with task to get download URL after upload completes
            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                profileImageRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    userUpdates["profileImageUrl"] = downloadUri.toString()

                    // Update Firestore document with new user data including profile image
                    if (documentId != null) {
                        db.collection("users").document(documentId!!)
                            .update(userUpdates)
                            .addOnSuccessListener {
                                Log.d("EditUserActivity", "User data updated successfully")
                                navigateToMainActivity()
                            }
                            .addOnFailureListener { e ->
                                Log.e("EditUserActivity", "Error updating user data", e)
                            }
                    } else {
                        Log.e("EditUserActivity", "Document ID is null")
                    }
                } else {
                    // Handle failures
                    Log.e("EditUserActivity", "Failed to upload profile image")
                }
            }
        } else {
            // Update Firestore document with user data without changing profile image
            if (documentId != null) {
                db.collection("users").document(documentId!!)
                    .update(userUpdates)
                    .addOnSuccessListener {
                        Log.d("EditUserActivity", "User data updated successfully")
                        navigateToMainActivity()
                    }
                    .addOnFailureListener { e ->
                        Log.e("EditUserActivity", "Error updating user data", e)
                    }
            } else {
                Log.e("EditUserActivity", "Document ID is null")
            }
        }
    }

    private fun navigateToMainActivity() {
        // Navigate back to MainActivity
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun choosePhoto() {
        // Open gallery to select a profile image
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Handle result of picking an image from gallery
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            profileImageView.setImageURI(selectedImageUri)
        }
    }

    private fun deleteUserData() {
        // Delete user data from Firestore
        if (documentId != null) {
            db.collection("users").document(documentId!!)
                .delete()
                .addOnSuccessListener {
                    Log.d("EditUserActivity", "User data deleted successfully")
                    finish() // Close the activity
                }
                .addOnFailureListener { e ->
                    Log.e("EditUserActivity", "Error deleting user data", e)
                }
        } else {
            Log.e("EditUserActivity", "Document ID is null")
        }
    }

    companion object {
        private const val REQUEST_CODE_PICK_IMAGE = 1
    }
}
