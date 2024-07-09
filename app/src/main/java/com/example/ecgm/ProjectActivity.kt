package com.example.ecgm


import com.example.ecgm.UserRatingAdapter
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ecgm.FinishedProjectActivity.Companion
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.io.File
import java.io.FileOutputStream
import android.Manifest
import com.google.firebase.firestore.QuerySnapshot


class ProjectActivity : AppCompatActivity(), TaskAdapter.OnItemClickListener {

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
    private lateinit var alertDialog: AlertDialog
    private val REQUEST_CODE_PERMISSIONS = 1001

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
        Log.d(FinishedProjectActivity.TAG, "THIS IS NOT THE FINISHEDFRAGMNET DUDEEEEEEEE")
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


// Assuming task.assignedUsers is a list of user IDs assigned to the task.


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

    private fun showDeleteConfirmationDialog(projectId: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Project")
            .setMessage("Are you sure you want to delete this project?")
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Delete") { _, _ ->
                deleteProject(projectId)
            }
            .show()
    }

    private fun deleteProject(projectId: String) {
        db.collection("projects")
            .whereEqualTo("id", projectId) // Assuming "id" is the field name in Firestore
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot) {
                    document.reference.delete()
                        .addOnSuccessListener {
                            Log.d(TAG, "DocumentSnapshot successfully deleted!")
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error deleting document", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error getting documents", e)
            }
    }

    private fun showPopupMenu() {
        val popupMenu = PopupMenu(this, findViewById(R.id.fabOptions))
        popupMenu.inflate(R.menu.project_menu)

        // Remove edit and remove options from popup menu if userRole is not admin
        if (userRole != "admin") {
            popupMenu.menu.removeItem(R.id.editProject)
            popupMenu.menu.removeItem(R.id.removeProject)
        }

        // Remove add user option from popup menu if userRole is not admin or manager
        if (userRole != "admin" && manager != 1) {
            popupMenu.menu.removeItem(R.id.addUserToProject)
            popupMenu.menu.removeItem(R.id.addTaskToProject)
            popupMenu.menu.removeItem(R.id.exportProjectStats)
        }

        // Check if tasks array is empty for the current project
        db.collection("projects").document(projectId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val project = documentSnapshot.toObject(Project::class.java)
                    if (project != null && project.tasks.isEmpty()) {
                        // Enable "Validate and Finish Project" menu item
                        popupMenu.menu.findItem(R.id.validateAndFinishProject)?.isEnabled = true
                    } else {
                        // Disable "Validate and Finish Project" menu item
                        popupMenu.menu.findItem(R.id.validateAndFinishProject)?.isEnabled = false
                    }
                } else {
                    Log.e(TAG, "Project document does not exist")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking tasks for project", e)
            }

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.editProject -> {
                    // Allow edit project action only if user is admin or manager
                    if (userRole == "admin" || userRole == "manager") {
                        Log.d(TAG, "Edit Project clicked")
                        val intent = Intent(this, EditProjectActivity::class.java)
                        intent.putExtra("project", project)
                        startActivityForResult(intent, EDIT_PROJECT_REQUEST)
                    } else {
                        Log.d(TAG, "User does not have permission to edit project")
                        // Handle user not authorized (optional)
                    }
                    true
                }
                R.id.removeProject -> {
                    // Allow remove project action only if user is admin or manager
                    if (userRole == "admin" || userRole == "manager") {
                        Log.d(TAG, "Remove Project clicked")
                        showDeleteConfirmationDialog(projectId)
                    } else {
                        Log.d(TAG, "User does not have permission to remove project")
                        // Handle user not authorized (optional)
                    }
                    true
                }
                R.id.addUserToProject -> {
                    // Allow add user to project action if user is admin or manager
                    if (userRole == "admin" || manager == 1) {
                        Log.d(TAG, "Add User to Project clicked")
                        showUserListDialog()
                    } else {
                        Log.d(TAG, "User does not have permission to add user to project")
                        // Handle user not authorized (optional)
                    }
                    true
                }
                R.id.addTaskToProject -> {
                    // Allow add task to project action if user is admin or manager
                    if (userRole == "admin" || manager == 1) {
                        Log.d(TAG, "Add Task to Project clicked")
                        addTaskToProject(projectId)
                    } else {
                        Log.d(TAG, "User does not have permission to add task to project")
                        // Handle user not authorized (optional)
                    }
                    true
                }
                R.id.viewFinishedTasks -> {
                    // Handle view finished tasks
                    Log.d(TAG, "View Finished Tasks clicked")
                    val intent = Intent(this, ViewFinishedTasksActivity::class.java)
                    intent.putExtra("projectId", project.id)
                    startActivity(intent)
                    true
                }
                R.id.exportProjectStats -> {
                    requestStoragePermissionAndExportStats()
                    true
                }
                R.id.exportUserStats -> {
                    showUserSelectionDialogForExport()
                    true
                }
                R.id.validateAndFinishProject -> {
                    // Validate and finish project
                    if (userRole == "admin" || manager == 1) {
                        showRatingDialog()
                    } else {
                        Log.d(TAG, "User does not have permission to validate and finish project")
                    }
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun exportProjectStats() {
        // Prepare data to write to file
        val projectStats = StringBuilder()
        projectStats.append("Project Name: ${project.name}\n")
        projectStats.append("Project Description: ${project.description}\n")
        projectStats.append("Manager ID: ${project.managerId}\n")
        projectStats.append("Number of Tasks: ${taskList.size}\n")
        projectStats.append("\nTasks:\n")
        taskList.forEachIndexed { index, task ->
            projectStats.append("${index + 1}. ${task.name}\n")
        }

        // Save to a text file in the ECGM directory of internal storage
        val folderName = "ECGM"
        val fileName = "${project.name}_stats.txt"
        val fileContents = projectStats.toString()
        val folder = File(getExternalFilesDir(null), folderName)

        // Create the directory if it doesn't exist
        if (!folder.exists()) {
            folder.mkdirs()
        }

        val file = File(folder, fileName)

        try {
            FileOutputStream(file).use {
                it.write(fileContents.toByteArray())
            }
            Log.d(TAG, "Project stats exported to ${file.absolutePath}")
            showToast("Project stats exported to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting project stats", e)
            showToast("Error exporting project stats")
        }
    }



    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun validateAndFinishProject(projectId: String) {
        // Move project to finishedProjects collection
        db.collection("projects")
            .document(projectId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val projectData = documentSnapshot.toObject(Project::class.java)
                    if (projectData != null) {
                        // Copy project data to finishedProjects collection
                        db.collection("finishedProjects")
                            .add(projectData)
                            .addOnSuccessListener { documentReference ->
                                Log.d(TAG, "Project validated and finished successfully: ${documentReference.id}")
                                // Delete project from projects collection
                                deleteProject(projectId)
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error copying project to finishedProjects collection", e)
                            }
                    } else {
                        Log.d(TAG, "Project data is null")
                    }
                } else {
                    Log.d(TAG, "Project document does not exist")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching project document", e)
            }
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


    private fun showUserListDialog() {
        // Fetch list of users and show them in a dialog
        db.collection("users")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val userList = mutableListOf<User>()
                for (document in querySnapshot.documents) {
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        userList.add(user)
                    }
                }

                // Display dialog to select user
                showUserSelectionDialogAdd(userList, true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching user list", e)
                // Handle error fetching user list (optional)
            }
    }

    private fun showUserSelectionDialogForExport() {
        // Fetch list of assigned users
        val assignedUsers = project.projectUsers
        if (assignedUsers.isNullOrEmpty()) {
            showToast("No users assigned to this project.")
            return
        }

        // Fetch user details from Firestore
        db.collection("users")
            .whereIn("id", assignedUsers)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val userList = mutableListOf<User>()
                for (document in querySnapshot.documents) {
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        userList.add(user)
                    }
                }
                // Display dialog to select user for exporting stats
                showUserSelectionDialog(userList, true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching assigned users", e)
            }
    }

    private fun showUserSelectionDialogAdd(userList: List<User>, forExport: Boolean) {
        val userNames = userList.map { it.name }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Select User")
            .setItems(userNames) { dialog, which ->
                val selectedUserId = userList[which].id
                if (selectedUserId != null) {
                    if (forExport) {
                        addUserToProject(projectId, selectedUserId)
                    } else {

                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showUserSelectionDialog(userList: List<User>, forExport: Boolean) {
        val userNames = userList.map { it.name }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Select User")
            .setItems(userNames) { dialog, which ->
                val selectedUserId = userList[which].id
                if (selectedUserId != null) {
                    if (forExport) {
                        exportUserStatsForUser(selectedUserId)
                    } else {
                        addUserToProject(projectId, selectedUserId)
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun exportUserStatsForUser(userId: String) {
        // Fetch user details
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        // Fetch tasks assigned to the user
                        db.collection("tasks")
                            .whereEqualTo("projectId", projectId)
                            .whereArrayContains("assignedUsers", userId)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                // Fetch finished tasks assigned to the user
                                db.collection("finishedTasks")
                                    .whereEqualTo("projectId", projectId)
                                    .whereArrayContains("assignedUsers", userId)
                                    .get()
                                    .addOnSuccessListener { finishedTasksSnapshot ->
                                        // Create file content
                                        val fileName = "${user.name}_stats.txt"
                                        val fileContents = buildUserStatsContent(user, querySnapshot, finishedTasksSnapshot)

                                        // Save to a text file in the ECGM directory of internal storage
                                        val folderName = "ECGM"
                                        val folder = File(getExternalFilesDir(null), folderName)
                                        if (!folder.exists()) {
                                            folder.mkdirs()
                                        }
                                        val file = File(folder, fileName)

                                        try {
                                            FileOutputStream(file).use {
                                                it.write(fileContents.toByteArray())
                                            }
                                            showToast("User stats exported to ${file.absolutePath}")
                                            Log.d(TAG, "User stats exported to ${file.absolutePath}")
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error exporting user stats", e)
                                            showToast("Error exporting user stats")
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Error fetching finished tasks", e)
                                    }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error fetching tasks", e)
                            }
                    }
                } else {
                    Log.d(TAG, "User document does not exist")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching user document", e)
            }
    }

    // Function to build user stats content
    private fun buildUserStatsContent(user: User, tasksSnapshot: QuerySnapshot, finishedTasksSnapshot: QuerySnapshot): String {
        val stringBuilder = StringBuilder()

        stringBuilder.append("User Name: ${user.name}\n")
        stringBuilder.append("Username: ${user.username}\n")
        stringBuilder.append("Email: ${user.email}\n")
        stringBuilder.append("Role: ${user.role}\n")

        val assignedTasks = tasksSnapshot.size()
        val completedTasks = finishedTasksSnapshot.size()

        stringBuilder.append("Number of tasks to complete: $assignedTasks\n")
        stringBuilder.append("Number of tasks completed: $completedTasks\n\n")

        stringBuilder.append("Tasks to complete:\n")
        tasksSnapshot.documents.forEachIndexed { index, taskDoc ->
            val taskName = taskDoc.getString("name")
            stringBuilder.append("${index + 1}. $taskName\n")
        }

        stringBuilder.append("\nTasks completed:\n")
        finishedTasksSnapshot.documents.forEachIndexed { index, taskDoc ->
            val taskName = taskDoc.getString("name")
            stringBuilder.append("${index + 1}. $taskName\n")
        }

        return stringBuilder.toString()
    }

    private fun addUserToProject(projectId: String, userId: String) {
        // Validate projectId and userId
        if (projectId.isNotBlank() && userId.isNotBlank()) {
            // Query for the document that contains the specific project ID
            db.collection("projects")
                .whereEqualTo("id", projectId) // Assuming "id" is the field name in Firestore
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        // Add userId to projectUsers array in Firestore project document
                        db.collection("projects")
                            .document(document.id) // Use the document ID to update existing document
                            .update("projectUsers", FieldValue.arrayUnion(userId))
                            .addOnSuccessListener {
                                Log.d(TAG, "User added to project successfully")
                                // You can update UI or perform any other action here
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error adding user to project", e)
                                // Handle error adding user to project (optional)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    // Handle error fetching document
                    Log.e(TAG, "Error getting project document", e)
                }
        } else {
            Log.e(TAG, "projectId or userId is blank or invalid")
            // Handle case where projectId or userId is not valid (optional)
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


    private fun addTaskToProject(projectId: String) {
        val intent = Intent(this, CreateTasksActivity::class.java)
        intent.putExtra("projectId", projectId)
        startActivity(intent)
    }


    private fun fetchTasks() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            userId = currentUser.uid
            Log.d(TAG, "Current role: $userRole")
            Log.d(TAG, "Current manager status: $manager")
            Log.d(TAG, "THIS IS THE not FINISHEDFRAGMNET DUDEEEEEEEE")
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




    private fun showRatingDialog() {
        // Fetch assigned users
        val assignedUsers = project.projectUsers
        if (assignedUsers.isNullOrEmpty()) {
            Log.d(TAG, "No users assigned to this project.")
            return
        }

        // Inflate dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_rate_users, null)
        val recyclerViewUsers: RecyclerView = dialogView.findViewById(R.id.recyclerViewUsers)
        val btnSubmitRatings: Button = dialogView.findViewById(R.id.btnSubmitRatings)

        // Create user rating list
        val userRatings = mutableListOf<UserRating>()

        // Fetch user details from Firestore
        db.collection("users")
            .whereIn("id", assignedUsers)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        userRatings.add(UserRating(user, 0))
                    }
                }

                // Set up RecyclerView
                recyclerViewUsers.layoutManager = LinearLayoutManager(this)
                val adapter = UserRatingAdapter(this, userRatings)
                recyclerViewUsers.adapter = adapter

                // Show dialog
                alertDialog = MaterialAlertDialogBuilder(this)
                    .setView(dialogView)
                    .setCancelable(false)
                    .create()
                alertDialog.show()

                // Handle submit ratings button
                btnSubmitRatings.setOnClickListener {
                    saveRatings(userRatings)
                    alertDialog.dismiss()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching assigned users", e)
            }
    }



    private fun saveRatings(userRatings: List<UserRating>) {
        val ratingsCollection = db.collection("projectRatings")

        val batch = db.batch()

        userRatings.forEach { userRating ->
            val ratingData = hashMapOf(
                "userId" to userRating.user.id,
                "projectId" to projectId,
                "rating" to userRating.rating,
                "timestamp" to FieldValue.serverTimestamp()
            )
            val ratingDoc = ratingsCollection.document()
            batch.set(ratingDoc, ratingData)
        }

        batch.commit()
            .addOnSuccessListener {
                Log.d(TAG, "Ratings saved successfully.")
                validateAndFinishProject(projectId)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving ratings", e)
            }
    }


    private fun requestStoragePermissionAndExportStats() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_CODE_PERMISSIONS)
            } else {
                exportProjectStats()
            }
        } else {
            exportProjectStats()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportProjectStats()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }



    companion object {
        const val EDIT_PROJECT_REQUEST = 1001
        const val TAG = "ProjectActivity"
    }
}
