package com.example.ecgm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adapter class for displaying a list of finished tasks in a RecyclerView
class UserFinishedTaskAdapter(
    private val taskList: List<Task>, // List of tasks to display
    private val itemClickListener: OnItemClickListener // Click listener interface for handling item clicks
) : RecyclerView.Adapter<UserFinishedTaskAdapter.TaskViewHolder>() {

    // Interface for handling item clicks in the RecyclerView
    interface OnItemClickListener {
        fun onItemClick(task: Task)
    }

    // Called when RecyclerView needs a new ViewHolder of the given type to represent an item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        // Inflate the item_task layout to create a new view
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view) // Return a new TaskViewHolder instance with the inflated view
    }

    // Called by RecyclerView to display the data at the specified position
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position] // Get the task at the specified position in the list
        holder.bind(task) // Bind the task data to the ViewHolder
        holder.itemView.setOnClickListener {
            itemClickListener.onItemClick(task) // Set click listener to handle item clicks
        }
    }

    // Returns the total number of items in the data set held by the adapter
    override fun getItemCount(): Int {
        return taskList.size
    }

    // ViewHolder class for holding the views that represent each item in the RecyclerView
    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Views within each item layout
        private val textViewTaskName: TextView = itemView.findViewById(R.id.textViewTaskName)
        private val textViewTaskDescription: TextView = itemView.findViewById(R.id.textViewTaskDescription)
        private val textViewTaskCompletion: TextView = itemView.findViewById(R.id.textViewTaskCompletion)

        // Binds task data to the views
        fun bind(task: Task) {
            textViewTaskName.text = task.name // Set task name
            textViewTaskDescription.text = task.description // Set task description
            textViewTaskCompletion.text = "Completion: ${task.completionPercentage}%" // Set task completion percentage
        }
    }
}
