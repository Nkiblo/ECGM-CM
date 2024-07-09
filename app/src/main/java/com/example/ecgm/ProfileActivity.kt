package com.example.ecgm

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class ProfileActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextUsername: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var buttonSave: Button
    private lateinit var profileImageView: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize UI components
        editTextName = findViewById(R.id.editTextName)
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextEmail = findViewById(R.id.editTextEmail)
        buttonSave = findViewById(R.id.buttonSave)
        profileImageView = findViewById(R.id.profileImageView)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        // Load user profile information
        loadUserProfile()

        // Set click listener for save button
        buttonSave.setOnClickListener {
            saveUserProfile()
        }

        // Set click listener for profile image view to choose a photo
        profileImageView.setOnClickListener {
            choosePhoto()
        }
    }

    // Load user profile data from Firestore and display it
    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            editTextEmail.setText(user.email)
            db.collection("users").document(user.uid).get().addOnSuccessListener { document ->
                if (document != null) {
                    editTextName.setText(document.getString("name"))
                    editTextUsername.setText(document.getString("username"))
                    val profileImageUrl = document.getString("profileImageUrl")

                    // Load profile image using Glide
                    if (!profileImageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(profileImageUrl)
                            .apply(RequestOptions().circleCrop())
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(profileImageView)
                    } else {
                        // Set default image if no profile image is set
                        profileImageView.setImageResource(R.mipmap.ic_launcher_round)
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to load user profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Save updated user profile data to Firestore
    private fun saveUserProfile() {
        val name = editTextName.text.toString()
        val username = editTextUsername.text.toString()

        val user = auth.currentUser
        if (user != null) {
            val userProfileUpdates = hashMapOf<String, Any>(
                "name" to name,
                "username" to username
            )

            db.collection("users").document(user.uid)
                .update(userProfileUpdates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                    // Pass updated name back to MainActivity
                    val resultIntent = Intent()
                    resultIntent.putExtra("updatedName", name)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error updating profile", e)
                }
        }
    }

    // Launch intent to choose a photo from the gallery
    private fun choosePhoto() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val imageUri = data.data
            Log.d(TAG, "Image URI selected: $imageUri")

            // Upload image to Firebase Storage
            val profileImageRef = storageRef.child("profileImages/${auth.currentUser!!.uid}")
            profileImageRef.putFile(imageUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    profileImageRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        Log.d(TAG, "Image URL from Storage: $imageUrl")

                        // Save image URL to Firestore
                        saveProfileImageUrl(imageUrl)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Failed to upload image: ${e.message}")
                }
        }
    }

    // Save profile image URL to Firestore
    private fun saveProfileImageUrl(imageUrl: String) {
        val user = auth.currentUser
        if (user != null) {
            val userProfileUpdates = hashMapOf<String, Any>(
                "profileImageUrl" to imageUrl
            )

            db.collection("users").document(user.uid)
                .update(userProfileUpdates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile image updated", Toast.LENGTH_SHORT).show()
                    // Reload profile to display the new image
                    loadUserProfile()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update profile image", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error updating profile image", e)
                }
        }
    }

    companion object {
        const val PICK_IMAGE_REQUEST = 1
    }
}
