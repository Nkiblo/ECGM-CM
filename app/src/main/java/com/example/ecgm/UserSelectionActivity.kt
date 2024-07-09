package com.example.ecgm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

// Activity for selecting a user from a list retrieved from Firestore
class UserSelectionActivity : AppCompatActivity(), UserAdapter.OnUserClickListener {

    private lateinit var recyclerView: RecyclerView // RecyclerView for displaying users
    private lateinit var userAdapter: UserAdapter // Adapter for populating users in RecyclerView
    private lateinit var db: FirebaseFirestore // Firestore instance for database operations

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_selection)

        db = FirebaseFirestore.getInstance() // Initialize Firestore instance
        recyclerView = findViewById(R.id.userRecyclerView) // Find RecyclerView from layout
        recyclerView.layoutManager = LinearLayoutManager(this) // Set LinearLayoutManager to RecyclerView
        userAdapter = UserAdapter(ArrayList(), this) // Initialize UserAdapter with an empty list and click listener
        recyclerView.adapter = userAdapter // Set UserAdapter to RecyclerView

        loadUsersFromFirestore() // Load users from Firestore database
    }

    // Function to load users from Firestore database
    private fun loadUsersFromFirestore() {
        val userList = ArrayList<User>() // Create an empty list to hold users

        // Fetch all documents from 'users' collection in Firestore
        db.collection("users")
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val user = document.toObject(User::class.java) // Convert Firestore document to User object
                    user?.let {
                        userList.add(it) // Add user to the list if conversion is successful
                    }
                }
                userAdapter.userList = userList // Update UserAdapter's userList with fetched users
                userAdapter.notifyDataSetChanged() // Notify RecyclerView that data set has changed
            }
            .addOnFailureListener { e ->
                Log.e("UserSelectionActivity", "Error fetching users", e) // Log error message if fetching fails
                // Handle error appropriately (e.g., display a Toast or message to the user)
            }
    }

    // Listener method invoked when a user item is clicked in RecyclerView
    override fun onUserClick(user: User) {
        // Create an intent to return the selected user's data back to the previous activity
        val intent = Intent()
        intent.putExtra("selectedUserId", user.id) // Put selected user's ID in intent
        intent.putExtra("selectedUserName", user.name) // Put selected user's name in intent
        intent.putExtra("selectedUserProfileImageUrl", user.profileImageUrl) // Put selected user's profile image URL in intent
        intent.putExtra("selectedManagerId", user.id) // Example: Put selected user's manager ID in intent
        intent.putExtra("selectedManagerName", user.name) // Example: Put selected user's manager name in intent
        intent.putExtra("selectedManagerProfileImageUrl", user.profileImageUrl) // Example: Put selected user's manager's profile image URL in intent
        setResult(RESULT_OK, intent) // Set result OK and include intent with data
        finish() // Finish current activity and return to previous activity
    }

    // Listener method invoked when "Create New User" action is clicked
    override fun onCreateNewUserClick() {
        // Handle click event for creating a new user
        val intent = Intent(this, CreateUserActivity::class.java) // Create intent to start CreateUserActivity
        startActivity(intent) // Start CreateUserActivity to create a new user
    }
}
