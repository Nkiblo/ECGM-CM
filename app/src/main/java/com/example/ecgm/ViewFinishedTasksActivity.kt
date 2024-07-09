package com.example.ecgm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ViewFinishedTasksActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var projectId: String
    private lateinit var finishedTasksAdapter: FinishedTasksAdapter

    private var assignedUsers: ArrayList<String>? = null // Declare as nullable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_finished_tasks)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Get projectId from intent extras
        projectId = intent.getStringExtra("projectId") ?: ""
        assignedUsers = intent.getStringArrayListExtra("assignedUsers")

        // Check if projectId is blank
        if (projectId.isBlank()) {
            Log.e(TAG, "projectId is blank or null")
            // Handle error or fallback gracefully
            finish() // Close activity if projectId is not valid
            return
        }

        // Initialize RecyclerView and adapter
        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewFinishedTasks)
        recyclerView.layoutManager = LinearLayoutManager(this)
        finishedTasksAdapter = FinishedTasksAdapter(emptyList(), object : FinishedTasksAdapter.OnItemClickListener {
            override fun onItemClick(task: Task) {
                navigateToFinishedTask(task)
            }
        })
        recyclerView.adapter = finishedTasksAdapter

        // Fetch and display finished tasks
        fetchFinishedTasks()
        Log.d(TAG, "THIS IS IT MY BOYYYYYYYYYYY")
    }

    private fun navigateToFinishedTask(task: Task) {
        val intent = Intent(this, FinishedTaskActivity::class.java)
        intent.putExtra("taskId", task.id)
        intent.putExtra("taskName", task.name)
        intent.putExtra("taskDescription", task.description)
        intent.putExtra("taskCompletionPercentage", task.completionPercentage)
        intent.putStringArrayListExtra("assignedUsers", ArrayList(task.assignedUsers))
        startActivity(intent)
    }

    private fun fetchFinishedTasks() {
        // Query finished tasks for the current project
        Log.d(TAG, "Project ID is: $projectId")
        db.collection("finishedTasks")
            .whereEqualTo("projectId", projectId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val finishedTasks = mutableListOf<Task>()
                for (document in querySnapshot.documents) {
                    val task = document.toObject(Task::class.java)
                    task?.let {
                        finishedTasks.add(it)
                    }
                }
                // Update adapter with fetched data
                finishedTasksAdapter.updateData(finishedTasks)

                Log.d(TAG, "Finished tasks fetched successfully. Count: ${finishedTasks.size}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching finished tasks", e)
            }
    }

    companion object {
        const val TAG = "ViewFinishedTasksActivity"
    }
}
