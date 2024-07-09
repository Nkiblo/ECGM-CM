package com.example.ecgm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

// Adapter class for displaying a list of users in a RecyclerView
class UserListAdapter(private var userList: List<String>) : RecyclerView.Adapter<UserListAdapter.UserViewHolder>() {

    // Firestore instance for database operations
    private val db = FirebaseFirestore.getInstance()

    // Called when RecyclerView needs a new ViewHolder of the given type to represent an item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        // Inflate the user_list_item layout to create a new view
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_list_item, parent, false)
        return UserViewHolder(view) // Return a new UserViewHolder instance with the inflated view
    }

    // Called by RecyclerView to display the data at the specified position
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val userId = userList[position] // Get the user ID at the specified position in the list
        holder.bind(userId) // Bind the user data to the ViewHolder
    }

    // Returns the total number of items in the data set held by the adapter
    override fun getItemCount(): Int {
        return userList.size
    }

    // Updates the adapter's user list with a new list and notifies any registered observers
    fun updateUsers(newList: List<String>) {
        userList = newList
        notifyDataSetChanged()
    }

    // ViewHolder class for holding the views that represent each item in the RecyclerView
    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Views within each user_list_item layout
        private val textViewUserName: TextView = itemView.findViewById(R.id.textViewUserName)
        private val textViewUserEmail: TextView = itemView.findViewById(R.id.textViewUserEmail)
        private val imageViewUserProfile: ImageView = itemView.findViewById(R.id.imageViewUserProfile)

        // Binds user data to the views
        fun bind(userId: String) {
            // Fetch user details from Firestore based on userId
            db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name") ?: "" // Get user's name
                        val email = document.getString("email") ?: "" // Get user's email
                        val profileImageUrl = document.getString("profileImageUrl") ?: "" // Get user's profile image URL

                        // Set user's name and email in the respective TextViews
                        textViewUserName.text = name
                        textViewUserEmail.text = email

                        // Load and display user's profile image using Glide library
                        Glide.with(itemView.context)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_launcher_background) // Placeholder image while loading
                            .error(R.drawable.ic_launcher_background) // Error image if loading fails
                            .into(imageViewUserProfile)
                    }
                }
                .addOnFailureListener { e ->
                    // Handle any errors that occur during the fetch operation
                    // For example, log the error or display a toast message
                }
        }
    }
}
