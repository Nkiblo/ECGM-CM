package com.example.ecgm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

// Adapter for displaying users and a "Create New User" option in a RecyclerView
class UserAdapter(
    var userList: List<User>,
    private val listener: OnUserClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Interface for handling user click events
    interface OnUserClickListener {
        fun onUserClick(user: User)
        fun onCreateNewUserClick()
    }

    private val VIEW_TYPE_USER = 1
    private val VIEW_TYPE_CREATE = 2

    // Inflate item layout and create ViewHolder based on the view type
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_USER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_create_user, parent, false)
            CreateViewHolder(view)
        }
    }

    // Bind data to ViewHolder and set click listener
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is UserViewHolder) {
            val userPosition = position
            holder.bind(userList[userPosition])
        } else if (holder is CreateViewHolder) {
            holder.itemView.setOnClickListener {
                listener.onCreateNewUserClick()
            }
        }
    }

    // Return the size of the user list plus one for the "Create New User" option
    override fun getItemCount(): Int {
        return userList.size + 1
    }

    // Determine view type based on position
    override fun getItemViewType(position: Int): Int {
        return if (position < userList.size) VIEW_TYPE_USER else VIEW_TYPE_CREATE
    }

    // ViewHolder class to hold and bind user data to the item view
    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val userNameTextView: TextView = itemView.findViewById(R.id.userName)
        private val userEmailTextView: TextView = itemView.findViewById(R.id.userEmail)
        private val profileImageView: ImageView = itemView.findViewById(R.id.userImage)

        init {
            itemView.setOnClickListener(this)
        }

        // Bind user data to the view elements
        fun bind(user: User) {
            userNameTextView.text = user.name
            userEmailTextView.text = user.email

            // Load profile image using Glide
            Glide.with(itemView.context)
                .load(user.profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_launcher_background) // Replace with your placeholder image
                .into(profileImageView)
        }

        // Handle item click events
        override fun onClick(v: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val clickedUser = userList[position]
                listener.onUserClick(clickedUser)
            }
        }
    }

    // ViewHolder for the "Create New User" option
    inner class CreateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
