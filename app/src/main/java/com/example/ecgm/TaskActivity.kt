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
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class TaskActivity : AppCompatActivity() {

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

    private lateinit var taskId: String
    private lateinit var taskName: String
    private lateinit var taskDescription: String
    private var taskCompletionPercentage: Int = 0
    private lateinit var assignedUsers: ArrayList<String>
    private var manager = false
    private lateinit var managerId: String

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var project: Project? = null
    private lateinit var projectId: String
    private var userRole: String? = null

    private val REQUEST_CODE_PERMISSIONS = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)

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
        fabOptions = findViewById(R.id.fabOptions)

        // Set up RecyclerView with LinearLayoutManager for users
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

        // Set up RecyclerView with LinearLayoutManager for observations
        recyclerViewObservations = findViewById(R.id.recyclerViewObservations)
        recyclerViewObservations.layoutManager = LinearLayoutManager(this)
        observationsAdapter = ObservationsAdapter(emptyList())
        recyclerViewObservations.adapter = observationsAdapter

        // Fetch additional task details from Firestore
        fetchAdditionalTaskDetails()

        // Update the adapter with assigned users
        userAdapter.updateUsers(assignedUsers)

        // Fetch project details and user role from Firestore
        fetchProjectDetails()
        fetchUserRole()

        // Setup FAB click listener to show popup menu
        fabOptions.setOnClickListener {
            showPopupMenu()
        }
    }

    // Fetch additional task details like last modified date, location, time spent, and observations from Firestore
    private fun fetchAdditionalTaskDetails() {
        db.collection("tasks").document(taskId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val lastModifiedDate = document.getDate("lastModifiedDate")
                    val location = document.getString("location")
                    val timeSpent = document.getDouble("timeSpent")

                    textViewLastModifiedDate.text = formatDate(lastModifiedDate)
                    textViewLocation.text = location ?: "N/A"
                    textViewTimeSpent.text = timeSpent?.toString() ?: "N/A"

                    // Fetch observations associated with the task
                    fetchObservations()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching additional task details", e)
            }
    }

    // Fetch observations associated with the task from Firestore
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

    // Format date to display in a readable format
    private fun formatDate(date: Date?): String {
        return if (date != null) {
            val format = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
            format.format(date)
        } else {
            "N/A"
        }
    }

    // Fetch project details from Firestore
    private fun fetchProjectDetails() {
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

    // Determine if the current user is a manager of the project
    private fun determineManagerStatus() {
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

    // Fetch user role from Firestore
    private fun fetchUserRole() {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    userRole = document.getString("role")
                    invalidateOptionsMenu()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching user document", e)
                }
        }
    }

    // Navigate to EditTaskActivity to edit the task details
    private fun navigateToEditTask() {
        val intent = Intent(this, EditTaskActivity::class.java)
        intent.putExtra("taskId", taskId)
        intent.putExtra("taskName", taskName)
        intent.putExtra("taskDescription", taskDescription)
        intent.putExtra("taskCompletionPercentage", taskCompletionPercentage)
        intent.putStringArrayListExtra("assignedUsers", assignedUsers)
        intent.putExtra("projectId", projectId)
        startActivityForResult(intent, EDIT_TASK_REQUEST)
    }

    // Show popup menu with options based on user role and manager status
    private fun showPopupMenu() {
        val popupMenu = PopupMenu(this, fabOptions)
        popupMenu.inflate(R.menu.task_menu)

        // Remove edit and export options if user is not admin or manager
        if (userRole != "admin" && !manager) {
            popupMenu.menu.removeItem(R.id.editTask)
            popupMenu.menu.removeItem(R.id.exportTaskStats)
        }

        // Handle menu item clicks
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.editTask -> {
                    navigateToEditTask()
                    true
                }
                R.id.continueTask -> {
                    navigateToContinueTask()
                    true
                }
                R.id.validateTask -> {
                    showValidationConfirmationDialog()
                    true
                }
                R.id.exportTaskStats -> {
                    requestStoragePermissionAndExportStats()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    // Navigate to ContinueTaskActivity to continue working on the task
    private fun navigateToContinueTask() {
        val intent = Intent(this, ContinueTaskActivity::class.java)
        intent.putExtra("taskId", taskId)
        intent.putExtra("taskCompletionPercentage", taskCompletionPercentage)
        startActivityForResult(intent, CONTINUE_TASK_REQUEST)
    }

    // Export task statistics to a text file
    private fun exportTaskStats() {
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

    // Request external storage permission and export task statistics
    private fun requestStoragePermissionAndExportStats() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_PERMISSIONS)
            } else {
                exportTaskStats()
            }
        } else {
            exportTaskStats()
        }
    }

    // Handle permission request result for external storage
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportTaskStats()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Handle activity result after editing or continuing the task
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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

    // Validate the task and move it to finishedTasks collection in Firestore
    private fun validateTask() {
        val taskDocument = db.collection("tasks").document(taskId)
        val finishedTaskDocument = db.collection("finishedTasks").document(taskId)

        taskDocument.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val taskData = document.data
                if (taskData != null) {
                    finishedTaskDocument.set(taskData)
                        .addOnSuccessListener {
                            taskDocument.delete()
                                .addOnSuccessListener {
                                    copyObservations(taskDocument, finishedTaskDocument)
                                    updateProjectDocument(taskId)
                                    setResult(Activity.RESULT_OK)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error deleting task", e)
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error copying task to finishedTasks collection", e)
                        }
                } else {
                    Log.e(TAG, "Task data is null")
                }
            } else {
                Log.e(TAG, "Task document does not exist")
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error fetching task document", e)
        }
    }

    // Copy observations associated with the task to finishedTasks/observations collection in Firestore
    private fun copyObservations(sourceTaskDocument: DocumentReference, destinationTaskDocument: DocumentReference) {
        sourceTaskDocument.collection("observations")
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    val observationData = doc.data
                    if (observationData != null) {
                        val destinationObservationDocument = destinationTaskDocument.collection("observations").document(doc.id)
                        batch.set(destinationObservationDocument, observationData)
                    }
                }
                batch.commit()
                    .addOnSuccessListener {
                        Log.d(TAG, "Observations copied to finishedTasks/observations")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error copying observations to finishedTasks/observations", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching observations", e)
            }
    }

    // Update the project document to remove the task from tasks array in Firestore
    private fun updateProjectDocument(taskId: String) {
        projectId = intent.getStringExtra("projectId") ?: ""
        if (projectId.isNotEmpty()) {
            val projectDocument = db.collection("projects").document(projectId)
            projectDocument.update("tasks", FieldValue.arrayRemove(taskId))
                .addOnSuccessListener {
                    Log.d(TAG, "Task ID removed from tasks array in project document")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error removing task ID from tasks array in project document", e)
                }
        } else {
            Log.e(TAG, "Project ID is empty or null")
        }
    }

    // Show confirmation dialog to validate the task
    private fun showValidationConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Validation")
        builder.setMessage("Are you sure you want to validate the task?")

        builder.setPositiveButton("Yes") { dialog, which ->
            validateTask()
        }

        builder.setNegativeButton("No") { dialog, which ->
            dialog.dismiss()
        }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    companion object {
        private const val TAG = "TaskActivity"
        private const val EDIT_TASK_REQUEST = 1
        private const val CONTINUE_TASK_REQUEST = 2
    }
}
