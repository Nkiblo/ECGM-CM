package com.example.ecgm

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class CreateUserActivity : AppCompatActivity() {
    private lateinit var nameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var selectProfileImageButton: Button
    private lateinit var profileImageView: ImageView
    private lateinit var registerButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var selectedImageUri: Uri? = null
    private var currentUser: FirebaseUser? = null


    private val adminEmail = "nkiblo.gm@gmail.com"
    private val adminPassword = "Derrar123!"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_user)

        // Initialize Firebase Authentication and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        nameEditText = findViewById(R.id.nameEditText)
        usernameEditText = findViewById(R.id.usernameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        selectProfileImageButton = findViewById(R.id.selectProfileImageButton)
        profileImageView = findViewById(R.id.profileImageView)
        registerButton = findViewById(R.id.registerButton)

        // Check if there is a current user
        currentUser = auth.currentUser
        Log.d("CreateUserActivity", "Current user: ${currentUser?.email}")

        selectProfileImageButton.setOnClickListener {
            selectImage()
        }

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Validate email and other inputs
            if (!isEmailValid(email)) {
                emailEditText.error = "Invalid email"
                return@setOnClickListener
            }

            if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Register user with Firebase Authentication
            registerUserWithEmailPassword(name, username, email, password)
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        resultLauncher.launch(intent)
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                Glide.with(this)
                    .load(selectedImageUri)
                    .circleCrop()
                    .into(profileImageView)
            }
        }
    }

    private fun registerUserWithEmailPassword(name: String, username: String, email: String, password: String) {
        // Store current user's session data
        val currentUserId = currentUser?.uid
        val currentUserEmail = currentUser?.email

        // Attempt registration
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        // Sign out the current user to maintain the existing session
                        auth.signOut()
                        // Upload image to Firebase Storage and save user data to Firestore
                        uploadImageAndSaveUserData(user.uid, name, username, email)
                    }
                } else {
                    Toast.makeText(baseContext, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    Log.e("CreateUserActivity", "Error registering user", task.exception)
                    // Restore the previous user's session if there was one
                    if (currentUserId != null && currentUserEmail != null) {
                        restoreSession(currentUserId, adminEmail, adminPassword)
                    }
                }
            }
    }

    private fun uploadImageAndSaveUserData(uid: String, name: String, username: String, email: String) {
        // Check if an image is selected
        if (selectedImageUri != null) {
            val storageRef = FirebaseStorage.getInstance().reference.child("profileImages").child(uid)
            val uploadTask = storageRef.putFile(selectedImageUri!!)

            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                storageRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    saveUserDataToFirestore(uid, name, username, email, downloadUri.toString())
                } else {
                    // Handle failures
                    Toast.makeText(baseContext, "Failed to upload image: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    Log.e("CreateUserActivity", "Error uploading image", task.exception)
                    // Restore the previous user's session if there was one
                    restoreSession(uid, adminEmail, adminPassword)
                }
            }
        } else {
            // If no image is selected, save data without image URL
            saveUserDataToFirestore(uid, name, username, email, "")
        }
    }



    private fun saveUserDataToFirestore(uid: String, name: String, username: String, email: String, profileImageUrl: String) {
        val metadataRef = db.collection("metadata").document("user_metadata")

        db.runTransaction { transaction ->
            val snapshot = transaction.get(metadataRef)
            val lastUserId = snapshot.getLong("last_user_id") ?: 0
            val newUserId = lastUserId + 1

            transaction.update(metadataRef, "last_user_id", newUserId)

            val userProfile = hashMapOf(
                "name" to name,
                "username" to username,
                "email" to email,
                "profileImageUrl" to profileImageUrl,
                "role" to "user",
                "id" to uid
            )

            transaction.set(db.collection("users").document(uid), userProfile)
        }.addOnSuccessListener {
            Toast.makeText(baseContext, "Registration successful", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }.addOnFailureListener { e ->
            Toast.makeText(baseContext, "Failed to save user information: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("CreateUserActivity", "Error saving user information", e)
            // Restore the previous user's session if there was one
            restoreSession(uid, adminEmail, adminPassword)
        }
    }


    private fun restoreSession(uid: String, email: String, password: String) {
        // Re-authenticate the previous user with email and password
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Optionally handle re-authentication success
                    Log.d("CreateUserActivity", "Session restored successfully")
                } else {
                    // Handle re-authentication failure
                    Toast.makeText(baseContext, "Failed to restore session: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    Log.e("CreateUserActivity", "Failed to restore session", task.exception)
                }
            }
    }
}
