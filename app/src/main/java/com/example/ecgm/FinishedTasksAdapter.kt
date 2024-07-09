package com.example.ecgm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adapter for displaying finished tasks in a RecyclerView
class FinishedTasksAdapter(
    private var finishedTasks: List<Task>,
    private val itemClickListener: OnItemClickListener
) : RecyclerView.Adapter<FinishedTasksAdapter.ViewHolder>() {

    // Interface for handling item clicks
    interface OnItemClickListener {
        fun onItemClick(task: Task)
    }

    // ViewHolder class to hold references to the views for each item
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskName: TextView = itemView.findViewById(R.id.textViewTaskName)
        private val taskDescription: TextView = itemView.findViewById(R.id.textViewTaskDescription)
        private val taskCompletionPercentage: TextView = itemView.findViewById(R.id.textViewTaskCompletion)

        // Initialize click listener for the item
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    itemClickListener.onItemClick(finishedTasks[position])
                }
            }
        }

        // Bind task data to the views
        fun bind(task: Task) {
            taskName.text = task.name
            taskDescription.text = task.description
            task.completionPercentage?.let {
                taskCompletionPercentage.text = "${it}%"
            }
        }
    }

    // Inflate the layout for each item and create a ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_finished_task, parent, false)
        return ViewHolder(view)
    }

    // Bind data to the ViewHolder for each item
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(finishedTasks[position])
    }

    // Return the total number of items
    override fun getItemCount(): Int {
        return finishedTasks.size
    }

    // Update the list of tasks and refresh the RecyclerView
    fun updateData(newTasks: List<Task>) {
        finishedTasks = newTasks
        notifyDataSetChanged()
    }
}
