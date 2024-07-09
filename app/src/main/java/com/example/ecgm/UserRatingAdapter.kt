package com.example.ecgm

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

// Adapter class for displaying user ratings in a RecyclerView
class UserRatingAdapter(
    private val context: Context, // Context reference to access resources and manage UI
    private val userRatings: List<UserRating> // List of user ratings to display
) : RecyclerView.Adapter<UserRatingAdapter.UserRatingViewHolder>() {

    // ViewHolder class for holding views that represent each item in the RecyclerView
    inner class UserRatingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Views within each item_user_rating layout
        val imageViewUserProfile: ImageView = itemView.findViewById(R.id.imageViewUserProfile) // User profile image
        val textViewUserName: TextView = itemView.findViewById(R.id.textViewUserName) // User name
        val editTextUserScore: EditText = itemView.findViewById(R.id.editTextUserScore) // User's rating score
    }

    // Called when RecyclerView needs a new ViewHolder of the given type to represent an item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserRatingViewHolder {
        // Inflate the item_user_rating layout to create a new view
        val view = LayoutInflater.from(context).inflate(R.layout.item_user_rating, parent, false)
        return UserRatingViewHolder(view) // Return a new UserRatingViewHolder instance with the inflated view
    }

    // Called by RecyclerView to display the data at the specified position
    override fun onBindViewHolder(holder: UserRatingViewHolder, position: Int) {
        val userRating = userRatings[position] // Get the UserRating object at the specified position

        holder.textViewUserName.text = userRating.user.name // Set user's name in the TextView
        holder.editTextUserScore.setText(userRating.rating.toString()) // Set user's rating score in the EditText

        // Load user profile image using Glide library
        Glide.with(context)
            .load(userRating.user.profileImageUrl) // Load image from URL
            .placeholder(R.drawable.ic_launcher_background) // Placeholder image while loading
            .error(R.drawable.side_nav_bar) // Error image if loading fails
            .into(holder.imageViewUserProfile) // Set the loaded image into ImageView

        // Add a text change listener to EditText to update user's rating when text changes
        holder.editTextUserScore.addTextChangedListener { text ->
            val rating = text?.toString()?.toIntOrNull() ?: 0 // Convert text to integer or default to 0 if null
            userRating.rating = rating // Update the rating of the UserRating object
        }
    }

    // Returns the total number of items in the data set held by the adapter
    override fun getItemCount() = userRatings.size
}
