package com.example.ecgm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adapter for displaying tasks in a RecyclerView
class TaskAdapter(private val taskList: List<Task>, private val itemClickListener: OnItemClickListener) :
    RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    // Interface for handling item click events
    interface OnItemClickListener {
        fun onItemClick(task: Task)
    }

    // Inflate item layout and create ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    // Bind data to ViewHolder and set click listener
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        holder.bind(task)
        holder.itemView.setOnClickListener {
            itemClickListener.onItemClick(task)
        }
    }

    // Return the size of the task list
    override fun getItemCount(): Int {
        return taskList.size
    }

    // ViewHolder class to hold and bind task data to the item view
    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewTaskName: TextView = itemView.findViewById(R.id.textViewTaskName)
        private val textViewTaskDescription: TextView = itemView.findViewById(R.id.textViewTaskDescription)
        private val textViewTaskCompletion: TextView = itemView.findViewById(R.id.textViewTaskCompletion)

        // Bind task data to the view elements
        fun bind(task: Task) {
            textViewTaskName.text = task.name
            textViewTaskDescription.text = task.description
            textViewTaskCompletion.text = "Completion: ${task.completionPercentage}%"
        }
    }
}
