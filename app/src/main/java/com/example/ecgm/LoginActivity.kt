package com.example.ecgm

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

// Activity for user login
class LoginActivity : AppCompatActivity() {
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var textView: TextView

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Initialize views
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.registerButton)
        val registerButton = findViewById<Button>(R.id.registerButton)
        textView = findViewById(R.id.registerNow)

        // Set click listener to navigate to RegisterActivity
        textView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        // Set click listener to navigate to RegisterActivity
        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Set click listener to perform login
        loginButton.setOnClickListener {
            // Extract data from EditTexts
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            // Validate email format
            if (!isEmailValid(email)) {
                emailEditText.error = "Email inválido"
                return@setOnClickListener
            }

            // Call login function
            loginUser(email, password)
        }
    }

    // Function to validate email using Patterns.EMAIL_ADDRESS matcher
    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Function to authenticate user with email and password using Firebase Authentication
    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // If login is successful, navigate to MainActivity
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    // If login fails, show error message
                    Toast.makeText(
                        baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}
