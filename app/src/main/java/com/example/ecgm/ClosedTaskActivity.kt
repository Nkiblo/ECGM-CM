package com.example.ecgm

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ClosedTaskActivity : AppCompatActivity() {

    // UI elements
    private lateinit var textViewTaskName: TextView
    private lateinit var textViewTaskDescription: TextView
    private lateinit var textViewCompletionPercentage: TextView
    private lateinit var textViewLastModifiedDate: TextView
    private lateinit var textViewLocation: TextView
    private lateinit var textViewTimeSpent: TextView
    private lateinit var textViewObservations: TextView
    private lateinit var recyclerViewUsers: RecyclerView
    private lateinit var userAdapter: UserListAdapter
    private lateinit var fabOptions: FloatingActionButton
    private lateinit var recyclerViewObservations: RecyclerView
    private lateinit var observationsAdapter: ObservationsAdapter

    // Task details
    private lateinit var taskId: String
    private lateinit var taskName: String
    private lateinit var taskDescription: String
    private var taskCompletionPercentage: Int = 0
    private lateinit var assignedUsers: ArrayList<String>
    private var manager = false
    private lateinit var managerId: String

    // Firebase instances
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Project details
    private var project: Project? = null
    private lateinit var projectId: String
    private var userRole: String? = null

    private val REQUEST_CODE_PERMISSIONS = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_closed)

        // Initialize Firebase instances
        db = Firebase.firestore
        auth = Firebase.auth

        // Initialize views
        textViewTaskName = findViewById(R.id.textViewTaskName)
        textViewTaskDescription = findViewById(R.id.textViewTaskDescription)
        textViewCompletionPercentage = findViewById(R.id.textViewCompletionPercentage)
        textViewLastModifiedDate = findViewById(R.id.textViewLastModifiedDate)
        textViewLocation = findViewById(R.id.textViewLocation)
        textViewTimeSpent = findViewById(R.id.textViewTimeSpent)
        textViewObservations = findViewById(R.id.textViewObservationsLabel)
        recyclerViewUsers = findViewById(R.id.recyclerViewUsers)

        // Set up RecyclerView with LinearLayoutManager
        recyclerViewUsers.layoutManager = LinearLayoutManager(this)
        userAdapter = UserListAdapter(emptyList())
        recyclerViewUsers.adapter = userAdapter

        // Retrieve task details from intent
        taskId = intent.getStringExtra("taskId") ?: ""
        taskName = intent.getStringExtra("taskName") ?: ""
        taskDescription = intent.getStringExtra("taskDescription") ?: ""
        taskCompletionPercentage = intent.getIntExtra("taskCompletionPercentage", 0)
        assignedUsers = intent.getStringArrayListExtra("assignedUsers") ?: ArrayList()
        managerId = intent.getStringExtra("managerId") ?: ""

        // Set task details to UI
        textViewTaskName.text = taskName
        textViewTaskDescription.text = taskDescription
        textViewCompletionPercentage.text = taskCompletionPercentage.toString()

        // Initialize observations RecyclerView
        recyclerViewObservations = findViewById(R.id.recyclerViewObservations)
        recyclerViewObservations.layoutManager = LinearLayoutManager(this)
        observationsAdapter = ObservationsAdapter(emptyList())
        recyclerViewObservations.adapter = observationsAdapter

        // Fetch additional task details
        fetchAdditionalTaskDetails()

        // Update the adapter with assigned users
        userAdapter.updateUsers(assignedUsers)

        // Fetch project details and user role
        fetchProjectDetails()
        fetchUserRole()
    }

    private fun fetchAdditionalTaskDetails() {
        // Fetch additional details about the finished task from Firestore
        db.collection("finishedTasks").document(taskId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val lastModifiedDate = document.getDate("lastModifiedDate")
                    val location = document.getString("location")
                    val timeSpent = document.getDouble("timeSpent")

                    textViewLastModifiedDate.text = formatDate(lastModifiedDate)
                    textViewLocation.text = location ?: "N/A"
                    textViewTimeSpent.text = timeSpent?.toString() ?: "N/A"

                    fetchObservations()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching additional task details", e)
            }
    }

    private fun fetchObservations() {
        // Fetch observations associated with the task from Firestore
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

    private fun formatDate(date: Date?): String {
        // Format date to a readable string
        return if (date != null) {
            val format = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
            format.format(date)
        } else {
            "N/A"
        }
    }

    private fun fetchProjectDetails() {
        // Fetch project details from Firestore
        projectId = intent.getStringExtra("projectId") ?: ""
        if (projectId.isNotEmpty()) {
            db.collection("projects").document(projectId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        project = document.toObject(Project::class.java)
                        determineManagerStatus()
                    } else {
                        Log.e(TAG, "Project document not found")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching project details", e)
                }
        } else {
            Log.e(TAG, "projectId is empty or null")
        }
    }

    private fun determineManagerStatus() {
        // Determine if the current user is the manager of the project
        val user = Firebase.auth.currentUser
        if (user != null) {
            val userId = user.uid
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val userDocumentId = document.getString("id")
                        if (userDocumentId == managerId) {
                            manager = true
                            invalidateOptionsMenu()
                            Log.d(TAG, "Current user is the manager of the project")
                        } else {
                            manager = false
                            invalidateOptionsMenu()
                            Log.d(TAG, "Current user is not the manager of the project")
                        }
                    } else {
                        manager = false
                        invalidateOptionsMenu()
                        Log.d(TAG, "User document does not exist")
                    }
                }
                .addOnFailureListener { e ->
                    manager = false
                    invalidateOptionsMenu()
                    Log.e(TAG, "Error fetching user document", e)
                }
        } else {
            manager = false
            invalidateOptionsMenu()
            Log.d(TAG, "Current user is null")
        }
    }

    private fun fetchUserRole() {
        // Fetch the role of the current user from Firestore
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            db.collection("users").document(userId).get().addOnSuccessListener { document ->
                userRole = document.getString("role")
                invalidateOptionsMenu()
            }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching user document", e)
                }
        }
    }

    private fun exportTaskStats() {
        // Prepare data to write to file
        val taskStats = """
            Task Name: $taskName
            Task Description: $taskDescription
            Completion Percentage: $taskCompletionPercentage
            Last Modified Date: ${textViewLastModifiedDate.text}
            Location: ${textViewLocation.text}
            Time Spent: ${textViewTimeSpent.text}
            Observations: ${textViewObservations.text}
            Assigned Users: ${assignedUsers.joinToString(", ")}
        """.trimIndent()

        // Create file and write data
        val fileName = "task_stats_${System.currentTimeMillis()}.txt"
        val file = File(getExternalFilesDir(null), fileName)
        try {
            FileOutputStream(file).use { fos ->
                fos.write(taskStats.toByteArray())
            }
            Toast.makeText(this, "Task stats exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting task stats", e)
            Toast.makeText(this, "Failed to export task stats", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        // Handle permission result for exporting task stats
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportTaskStats()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Handle result from editing or continuing the task
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == EDIT_TASK_REQUEST || requestCode == CONTINUE_TASK_REQUEST) {
                taskName = data?.getStringExtra("taskName") ?: taskName
                taskDescription = data?.getStringExtra("taskDescription") ?: taskDescription
                taskCompletionPercentage = data?.getIntExtra("taskCompletionPercentage", taskCompletionPercentage) ?: taskCompletionPercentage
                assignedUsers = data?.getStringArrayListExtra("assignedUsers") ?: assignedUsers

                textViewTaskName.text = taskName
                textViewTaskDescription.text = taskDescription
                textViewCompletionPercentage.text = taskCompletionPercentage.toString()

                fetchAdditionalTaskDetails()
                userAdapter.updateUsers(assignedUsers)
            }
        }
    }

    companion object {
        // Constants for logging and request codes
        private const val TAG = "TaskActivity"
        private const val EDIT_TASK_REQUEST = 1
        private const val CONTINUE_TASK_REQUEST = 2
    }
}
