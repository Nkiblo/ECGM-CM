package com.example.ecgm

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class FinishedTaskActivity : AppCompatActivity() {

    private lateinit var textViewTaskName: TextView
    private lateinit var textViewTaskDescription: TextView
    private lateinit var textViewCompletionPercentage: TextView
    private lateinit var recyclerViewUsers: RecyclerView
    private lateinit var userAdapter: UserListAdapter
    private lateinit var recyclerViewObservations: RecyclerView
    private lateinit var observationsAdapter: ObservationsAdapter

    private lateinit var taskId: String
    private lateinit var taskName: String
    private lateinit var taskDescription: String
    private var taskCompletionPercentage: Int = 0
    private lateinit var assignedUsers: ArrayList<String>

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finished_task)

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance()

        // Initialize views
        textViewTaskName = findViewById(R.id.textViewTaskName)
        textViewTaskDescription = findViewById(R.id.textViewTaskDescription)
        textViewCompletionPercentage = findViewById(R.id.textViewCompletionPercentage)
        recyclerViewUsers = findViewById(R.id.recyclerViewUsers)
        recyclerViewObservations = findViewById(R.id.recyclerViewObservations)

        // Set up RecyclerViews with LinearLayoutManager
        recyclerViewUsers.layoutManager = LinearLayoutManager(this)
        userAdapter = UserListAdapter(emptyList())
        recyclerViewUsers.adapter = userAdapter

        recyclerViewObservations.layoutManager = LinearLayoutManager(this)
        observationsAdapter = ObservationsAdapter(emptyList())
        recyclerViewObservations.adapter = observationsAdapter

        // Retrieve task details from intent
        taskId = intent.getStringExtra("taskId") ?: ""
        taskName = intent.getStringExtra("taskName") ?: ""
        taskDescription = intent.getStringExtra("taskDescription") ?: ""
        taskCompletionPercentage = intent.getIntExtra("taskCompletionPercentage", 0)
        assignedUsers = intent.getStringArrayListExtra("assignedUsers") ?: ArrayList()

        // Set task details to UI
        textViewTaskName.text = taskName
        textViewTaskDescription.text = taskDescription
        textViewCompletionPercentage.text = taskCompletionPercentage.toString()

        // Update the adapter with assigned users
        userAdapter.updateUsers(assignedUsers)

        // Fetch observations and update the adapter
        fetchObservations()
    }

    private fun fetchObservations() {
        db.collection("tasks").document(taskId).collection("observations")
            .get()
            .addOnSuccessListener { documents ->
                val observations = documents.map { document ->
                    Observation(
                        id = document.id,
                        description = document.getString("description") ?: "",
                        imageUri = document.getString("imageUri")
                    )
                }
                observationsAdapter.updateObservations(observations)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching observations", e)
            }
    }

    companion object {
        private const val TAG = "FinishedTaskActivity"
    }
}
