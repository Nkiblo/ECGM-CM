package com.example.ecgm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase Authentication and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        nameEditText = findViewById(R.id.nameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        registerButton = findViewById(R.id.registerButton)

        // Set click listener for register button
        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Validate email format
            if (!isEmailValid(email)) {
                emailEditText.error = "Invalid email"
                return@setOnClickListener
            }

            // Attempt to register the user
            registerUser(name, email, password)
        }
    }

    // Function to validate email format using Patterns.EMAIL_ADDRESS matcher
    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Function to register a new user with Firebase Authentication
    private fun registerUser(name: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registration successful, proceed to save user information in Firestore
                    val user = auth.currentUser
                    if (user != null) {
                        assignUserIdAndSaveUser(user.uid, name, email)
                    }
                } else {
                    // Registration failed, display error message
                    Toast.makeText(baseContext, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    Log.e("RegisterActivity", "Error registering user", task.exception)
                }
            }
    }

    // Function to save user information in Firestore
    private fun assignUserIdAndSaveUser(uid: String, name: String, email: String) {
        val userProfile = hashMapOf(
            "name" to name,
            "email" to email,
            "role" to "user", // Default role for newly registered users
            "id" to uid       // Use 'uid' as the 'id' field in Firestore
        )

        // Set document with 'uid' as the document ID in 'users' collection
        db.collection("users").document(uid)
            .set(userProfile)
            .addOnSuccessListener {
                // User information saved successfully, navigate to LoginActivity
                Toast.makeText(baseContext, "Registration successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                // Failed to save user information, log error
                Toast.makeText(baseContext, "Failed to save user information: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("CreateUserActivity", "Error saving user information", e)
            }
    }
}
