package com.example.ecgm

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class FinishedProjectActivity : AppCompatActivity(), TaskAdapter.OnItemClickListener {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var projectId: String
    private lateinit var project: Project // Change to lateinit var
    private var userRole: String? = null
    private var manager = 0
    private lateinit var userId: String
    private lateinit var recyclerViewTasks: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project)

        // Initialize Firestore and Authentication
        db = FirebaseFirestore.getInstance()
        auth = Firebase.auth

        // Retrieve project details from Intent
        project = intent.getSerializableExtra("project") as Project // Ensure project is not null
        projectId = project.id

        // Initialize views
        val textViewProjectName: TextView = findViewById(R.id.textViewProjectName)
        val textViewProjectDescription: TextView = findViewById(R.id.textViewProjectDescription)
        val textViewManagerName: TextView = findViewById(R.id.textViewManagerName)
        val imageViewProject: ImageView = findViewById(R.id.imageViewProject)
        val imageViewManager: ImageView = findViewById(R.id.imageViewManager)
        val fab: FloatingActionButton = findViewById(R.id.fabOptions)
        recyclerViewTasks = findViewById(R.id.recyclerViewTasks)

        // Set project details
        textViewProjectName.text = project.name
        textViewProjectDescription.text = project.description

        // Load project image using Glide
        Glide.with(this)
            .load(project.imageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.side_nav_bar)
            .into(imageViewProject)

        // Initialize RecyclerView
        recyclerViewTasks.layoutManager = LinearLayoutManager(this)
        taskAdapter = TaskAdapter(taskList, this) // Pass `this` as the click listener
        recyclerViewTasks.adapter = taskAdapter

        // Fetch user role and display manager details
        fetchUserRole {
            // Callback function called after user role is fetched
            determineManagerStatus {
                // Callback function called after manager status is determined
                fetchManagerDetails(project.managerId, textViewManagerName, imageViewManager)
                fetchTasks()
            }
        }

        // Set click listener for FAB to open popup menu
        fab.setOnClickListener {
            showPopupMenu()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when the activity is resumed
        val textViewManagerName: TextView = findViewById(R.id.textViewManagerName)
        val imageViewManager: ImageView = findViewById(R.id.imageViewManager)
        fetchUserRole {
            // Callback function called after user role is fetched
            determineManagerStatus {
                // Callback function called after manager status is determined
                fetchManagerDetails(project.managerId, textViewManagerName, imageViewManager)
                fetchTasks()
            }
        }
    }

    override fun onItemClick(task: Task) {
        Log.d(TAG, "Task clicked: ${task.name}")

        // Navigate to TaskActivity with task details
        val intent = Intent(this, TaskActivity::class.java)
        intent.putExtra("taskId", task.id)
        intent.putExtra("taskName", task.name)
        intent.putExtra("taskDescription", task.description)
        intent.putExtra("taskCompletionPercentage", task.completionPercentage)
        intent.putStringArrayListExtra("assignedUsers", ArrayList(task.assignedUsers))
        intent.putExtra("projectId", projectId)
        intent.putExtra("managerId", project.managerId) // Make sure you have this line
        startActivity(intent)
    }




    private fun fetchManagerDetails(managerId: String, textViewManagerName: TextView, imageViewManager: ImageView) {
        db.collection("users")
            .whereEqualTo("id", managerId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val managerName = document.getString("name")
                    val managerImageUrl = document.getString("profileImageUrl")

                    // Set manager name
                    textViewManagerName.text = managerName

                    // Load manager image using Glide
                    Glide.with(this)
                        .load(managerImageUrl)
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.side_nav_bar)
                        .into(imageViewManager)
                } else {
                    Log.d(TAG, "No such manager document")
                    textViewManagerName.text = "Unknown Manager"
                    imageViewManager.setImageResource(R.drawable.ic_launcher_background)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching manager details", e)
                textViewManagerName.text = "Unknown Manager"
                imageViewManager.setImageResource(R.drawable.ic_launcher_background)
            }
    }


    private fun showPopupMenu() {
        val popupMenu = PopupMenu(this, findViewById(R.id.fabOptions))
        popupMenu.inflate(R.menu.project_menu)

        // Remove edit and remove options from popup menu if userRole is not admin
        if (userRole != "0") {
            popupMenu.menu.removeItem(R.id.editProject)
            popupMenu.menu.removeItem(R.id.removeProject)
            popupMenu.menu.removeItem(R.id.addTaskToProject)
            popupMenu.menu.removeItem(R.id.addUserToProject)
            popupMenu.menu.removeItem(R.id.validateAndFinishProject)

        }

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {

                R.id.viewFinishedTasks -> {
                    // Handle view finished tasks
                    Log.d(TAG, "View Finished Tasks clicked")
                    val intent = Intent(this, ViewFinishedTasksActivity::class.java)
                    intent.putExtra("projectId", project.id)
                    Log.d(TAG, "Project id = $projectId, ${project.id}")
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    // Function to determine if project manager ID matches user ID
    private fun fetchUserRole(callback: () -> Unit) {
        // Fetch user role from Firestore
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            db.collection("users").document(userId).get().addOnSuccessListener { document ->
                userRole = document.getString("role")
                Log.d("ProjectActivity", "User Role: $userRole")

                // Invalidate options menu to reflect role-based changes
                invalidateOptionsMenu()

                // Invoke callback function after fetching user role
                callback()
            }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching user document", e)
                    // Handle error fetching document
                }
        }
    }

    private fun determineManagerStatus(callback: () -> Unit) {
        val user = Firebase.auth.currentUser
        if (user != null) {
            val userId = user.uid
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val userDocumentId = document.getString("id")

                        // Compare managerId of project with id field of user document
                        if (userDocumentId == project.managerId) {
                            // User is manager of the project
                            manager = 1
                            invalidateOptionsMenu() // Refresh options menu after manager status is determined
                            Log.d(TAG, "Current user is the manager of the project")
                            Log.d(TAG, "MANAGER IS EQUAL TO : $manager")
                        } else {
                            // User is not manager of the project
                            manager = 0
                            invalidateOptionsMenu() // Refresh options menu after manager status is determined
                            Log.d(TAG, "Current user is not the manager of the project")
                            Log.d(TAG, "MANAGER IS EQUAL TO : $manager")
                        }
                    } else {
                        // Handle case where user document does not exist
                        manager = 0
                        invalidateOptionsMenu() // Refresh options menu after manager status is determined
                        Log.d(TAG, "User document does not exist")
                        Log.d(TAG, "MANAGER IS EQUAL TO : $manager")
                    }

                    // Invoke callback function after determining manager status
                    callback()
                }
                .addOnFailureListener { e ->
                    // Handle error fetching user document
                    manager = 0
                    invalidateOptionsMenu() // Refresh options menu after manager status is determined
                    Log.e(TAG, "Error fetching user document", e)
                    Log.d(TAG, "MANAGER IS EQUAL TO : $manager")

                    // Invoke callback function after determining manager status
                    callback()
                }
        } else {
            // Handle case where current user is null
            manager = 0
            invalidateOptionsMenu() // Refresh options menu after manager status is determined
            Log.d(TAG, "Current user is null")
            Log.d(TAG, "MANAGER IS EQUAL TO : $manager")

            // Invoke callback function after determining manager status
            callback()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_PROJECT_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.getSerializableExtra("updatedProject")?.let { updatedProject ->
                project = updatedProject as Project

                // Update the UI with the new project details
                val textViewProjectName: TextView = findViewById(R.id.textViewProjectName)
                val textViewProjectDescription: TextView = findViewById(R.id.textViewProjectDescription)
                val textViewManagerName: TextView = findViewById(R.id.textViewManagerName)
                val imageViewProject: ImageView = findViewById(R.id.imageViewProject)
                val imageViewManager: ImageView = findViewById(R.id.imageViewManager)

                textViewProjectName.text = project.name
                textViewProjectDescription.text = project.description

                // Load the new project image
                Glide.with(this)
                    .load(project.imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.side_nav_bar)
                    .into(imageViewProject)

                // Fetch and display new manager details
                fetchManagerDetails(project.managerId, textViewManagerName, imageViewManager)
            }
        }
    }


    private fun fetchTasks() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            userId = currentUser.uid
            Log.d(TAG, "Current role: $userRole")
            Log.d(TAG, "Current manager status: $manager")

            if (userRole == "admin" || manager == 1) {
                Log.d(TAG, "Fetching all tasks as admin or manager")
                db.collection("tasks")
                    .whereEqualTo("projectId", projectId)
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
                        Log.d(TAG, "Tasks fetched successfully for admin/manager. Count: ${taskList.size}")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error fetching tasks", e)
                    }
            } else {
                Log.d(TAG, "Fetching tasks assigned to current user")
                db.collection("tasks")
                    .whereEqualTo("projectId", projectId)
                    .whereArrayContains("assignedUsers", userId)
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
                        Log.d(TAG, "Tasks fetched successfully for current user. Count: ${taskList.size}")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error fetching tasks", e)
                    }
            }
        } else {
            Log.d(TAG, "Current user is null, cannot fetch tasks")
        }
    }



    companion object {
        const val EDIT_PROJECT_REQUEST = 1001
        const val TAG = "ProjectActivity"
    }
}
