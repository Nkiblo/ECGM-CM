package com.example.ecgm

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ProjectsAdapter(private var projects: List<Project>) : RecyclerView.Adapter<ProjectsAdapter.ProjectViewHolder>() {

    private var itemClickListener: OnItemClickListener? = null

    // Create View Holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false)
        return ProjectViewHolder(view)
    }

    // Bind data to ViewHolder
    // Inside ProjectsAdapter, where you handle item click
    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = projects[position]
        holder.bind(project)

        // Set click listener for item
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ProjectActivity::class.java)
            intent.putExtra("project", project) // Pass the Project object directly
            holder.itemView.context.startActivity(intent)
        }
    }


    // Return number of items in the list
    override fun getItemCount(): Int {
        return projects.size
    }

    // Update data set
    fun updateProjects(projects: List<Project>) {
        this.projects = projects
        notifyDataSetChanged()
    }

    // Set item click listener
    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.itemClickListener = listener
    }

    // ViewHolder class
    inner class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    itemClickListener?.onItemClick(projects[position])
                }
            }
        }
        private val projectNameTextView: TextView = itemView.findViewById(R.id.projectNameTextView)
        private val projectImageView: ImageView = itemView.findViewById(R.id.imageViewProject)

        // Bind data to views
        fun bind(project: Project) {
            projectNameTextView.text = project.name

            Glide.with(itemView)
                .load(project.imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.side_nav_bar)
                .into(projectImageView)

            // Set click listener for item
            itemView.setOnClickListener {
                itemClickListener?.onItemClick(project)
            }
        }
    }

    // Interface for item click listener
    interface OnItemClickListener {
        fun onItemClick(project: Project)
    }
}
