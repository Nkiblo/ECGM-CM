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

class UserFinishedTaskActivity : AppCompatActivity(), UserFinishedTaskAdapter.OnItemClickListener {

    private lateinit var recyclerViewFinishedTasks: RecyclerView
    private lateinit var userFinishedTaskAdapter: UserFinishedTaskAdapter
    private val taskList = mutableListOf<Task>()
    private lateinit var db: FirebaseFirestore
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_finished_task)

        // Initialize Firestore and Authentication
        db = FirebaseFirestore.getInstance()
        val auth = Firebase.auth
        userId = auth.currentUser!!.uid

        // Initialize RecyclerView
        recyclerViewFinishedTasks = findViewById(R.id.recyclerViewUserFinishedTasks)
        recyclerViewFinishedTasks.layoutManager = LinearLayoutManager(this)
        userFinishedTaskAdapter = UserFinishedTaskAdapter(taskList, this)
        recyclerViewFinishedTasks.adapter = userFinishedTaskAdapter

        // Fetch finished tasks
        fetchFinishedTasks()
    }

    private fun fetchFinishedTasks() {
        db.collection("finishedTasks")
            .whereArrayContains("assignedUsers", userId)
            .orderBy("name", Query.Direction.ASCENDING) // Example ordering by task name
            .get()
            .addOnSuccessListener { querySnapshot ->
                taskList.clear()
                for (document in querySnapshot.documents) {
                    val task = document.toObject(Task::class.java)
                    if (task != null) {
                        taskList.add(task)
                    }
                }
                userFinishedTaskAdapter.notifyDataSetChanged()
                Log.d("FinishedTasksActivity", "Finished tasks fetched successfully. Count: ${taskList.size}")
            }
            .addOnFailureListener { e ->
                Log.e("FinishedTasksActivity", "Error fetching finished tasks", e)
            }
    }

    override fun onItemClick(task: Task) {
        Log.d("FinishedTasksActivity", "Task clicked: ${task.name}")
        Log.d("FinishedTasksActivity", "Task project id is: ${task.projectId}")

        // Navigate to ClosedTaskActivity with task details
        val intent = Intent(this, ClosedTaskActivity::class.java)
        intent.putExtra("taskId", task.id)
        intent.putExtra("taskName", task.name)
        intent.putExtra("taskDescription", task.description)
        intent.putExtra("taskCompletionPercentage", task.completionPercentage)
        intent.putStringArrayListExtra("assignedUsers", ArrayList(task.assignedUsers))
        intent.putExtra("projectId", task.projectId)
        startActivity(intent)
    }
}
