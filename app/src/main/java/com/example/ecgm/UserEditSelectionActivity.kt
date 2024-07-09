package com.example.ecgm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

// Activity for selecting and editing users
class UserEditSelectionActivity : AppCompatActivity(), UserAdapter.OnUserClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_selection)

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance()

        // Initialize RecyclerView and set up layout manager
        recyclerView = findViewById(R.id.userRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize and set up adapter for user list
        userAdapter = UserAdapter(ArrayList(), this)
        recyclerView.adapter = userAdapter

        // Load users from Firestore database
        loadUsersFromFirestore()
    }

    // Fetch users from Firestore database
    private fun loadUsersFromFirestore() {
        val userList = ArrayList<User>()

        db.collection("users")
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val user = document.toObject(User::class.java)
                    user?.let {
                        userList.add(it)
                    }
                }
                userAdapter.userList = userList
                userAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("UserEditSelectionActivity", "Error fetching users", e)
                // Handle error appropriately (display a Toast or message to the user)
            }
    }

    // Handle click on a user item in the RecyclerView
    override fun onUserClick(user: User) {
        // Navigate to EditUserActivity and pass user ID or necessary data
        val intent = Intent(this, EditUserActivity::class.java)
        intent.putExtra("userId", user.id)
        startActivity(intent)
    }

    // Handle click on the "Create New User" option in the RecyclerView
    override fun onCreateNewUserClick() {
        // Navigate to CreateUserActivity to create a new user
        val intent = Intent(this, CreateUserActivity::class.java)
        startActivity(intent)
    }
}
