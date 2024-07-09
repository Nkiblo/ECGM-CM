package com.example.ecgm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase

class AssignedTasksActivity : AppCompatActivity(), TaskAdapter.OnItemClickListener {

    private lateinit var recyclerViewAssignedTasks: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>()
    private lateinit var db: FirebaseFirestore
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assigned_tasks)

        // Initialize Firestore and Authentication
        db = FirebaseFirestore.getInstance()
        val auth = Firebase.auth
        userId = auth.currentUser!!.uid

        // Initialize RecyclerView
        recyclerViewAssignedTasks = findViewById(R.id.recyclerViewAssignedTasks)
        recyclerViewAssignedTasks.layoutManager = LinearLayoutManager(this)
        taskAdapter = TaskAdapter(taskList, this)
        recyclerViewAssignedTasks.adapter = taskAdapter

        // Fetch assigned tasks
        fetchAssignedTasks()
    }

    private fun fetchAssignedTasks() {
        // Query Firestore for tasks assigned to the current user
        db.collection("tasks")
            .whereArrayContains("assignedUsers", userId)
            .orderBy("name", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                taskList.clear()
                for (document in querySnapshot.documents) {
                    val task = document.toObject(Task::class.java)
                    if (task != null) {
                        taskList.add(task)
                    }
                }
                taskAdapter.notifyDataSetChanged()
                Log.d("AssignedTasksActivity", "Assigned tasks fetched successfully. Count: ${taskList.size}")
            }
            .addOnFailureListener { e ->
                Log.e("AssignedTasksActivity", "Error fetching assigned tasks", e)
            }
    }

    override fun onItemClick(task: Task) {
        Log.d("AssignedTasksActivity", "Task clicked: ${task.name}")
        Log.d("AssignedTasksActivity", "Task project id is: ${task.projectId}")

        // Navigate to TaskActivity with task details
        val intent = Intent(this, TaskActivity::class.java)
        intent.putExtra("taskId", task.id)
        intent.putExtra("taskName", task.name)
        intent.putExtra("taskDescription", task.description)
        intent.putExtra("taskCompletionPercentage", task.completionPercentage)
        intent.putStringArrayListExtra("assignedUsers", ArrayList(task.assignedUsers))
        intent.putExtra("projectId", task.projectId)
        startActivity(intent)
    }
}
