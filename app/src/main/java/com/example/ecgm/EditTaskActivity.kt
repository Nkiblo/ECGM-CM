package com.example.ecgm

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class EditTaskActivity : AppCompatActivity() {

    // Declare UI elements and Firestore variables
    private lateinit var taskId: String
    private lateinit var editTextTaskName: EditText
    private lateinit var editTextTaskDescription: EditText
    private lateinit var editTextCompletionPercentage: EditText
    private lateinit var buttonSaveTask: Button
    private lateinit var buttonDeleteTask: Button
    private lateinit var listViewUsers: ListView
    private lateinit var textInputLayoutTaskName: TextInputLayout
    private lateinit var projectId: String

    private lateinit var db: FirebaseFirestore
    private lateinit var userList: MutableList<User>
    private lateinit var assignedUsers: MutableList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_task)

        // Initialize Firestore instance
        db = FirebaseFirestore.getInstance()

        // Initialize UI elements
        editTextTaskName = findViewById(R.id.editTextTaskName)
        editTextTaskDescription = findViewById(R.id.editTextTaskDescription)
        editTextCompletionPercentage = findViewById(R.id.editTextCompletionPercentage)
        buttonSaveTask = findViewById(R.id.buttonSaveTask)
        buttonDeleteTask = findViewById(R.id.buttonDeleteTask)
        textInputLayoutTaskName = findViewById(R.id.textInputLayoutTaskName)
        listViewUsers = findViewById(R.id.listViewUsers)

        // Set up click listeners for save and delete buttons
        buttonSaveTask.setOnClickListener {
            saveTask()
        }

        buttonDeleteTask.setOnClickListener {
            deleteTask()
        }

        // Retrieve task details from the intent
        taskId = intent.getStringExtra("taskId") ?: ""
        val taskName = intent.getStringExtra("taskName") ?: ""
        val taskDescription = intent.getStringExtra("taskDescription") ?: ""
        val taskCompletionPercentage = intent.getIntExtra("taskCompletionPercentage", 0)
        assignedUsers = intent.getStringArrayListExtra("assignedUsers") ?: mutableListOf()

        projectId = intent.getStringExtra("projectId") ?: ""

        // Populate UI with the retrieved task details
        editTextTaskName.setText(taskName)
        editTextTaskDescription.setText(taskDescription)
        editTextCompletionPercentage.setText(taskCompletionPercentage.toString())

        // Initialize user list and fetch users
        userList = mutableListOf()
        fetchUsers()
    }

    private fun fetchUsers() {
        // Fetch project details to get the list of project users
        db.collection("projects")
            .whereEqualTo("id", projectId) // Assuming "id" is the field name of the project ID
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val projectDocument = querySnapshot.documents[0]
                    val projectUsers = projectDocument.get("projectUsers") as List<String>

                    // Fetch all users from Firestore
                    db.collection("users")
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            userList.clear()
                            for (document in querySnapshot) {
                                val user = document.toObject(User::class.java).apply { id = document.id }

                                // Filter userList to include only users who are in projectUsers
                                if (projectUsers.contains(user.id)) {
                                    userList.add(user)
                                }
                            }
                            displayUsers()
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error fetching users", e)
                        }
                } else {
                    Log.e(TAG, "No project found with ID: $projectId")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching project details", e)
            }
    }

    private fun displayUsers() {
        // Map user names to an array for the ListView
        val userNames = userList.map { it.name ?: "Unnamed User" }.toTypedArray()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, userNames)
        listViewUsers.adapter = adapter

        // Pre-select users who are already assigned to the task
        userList.forEachIndexed { index, user ->
            if (assignedUsers.contains(user.id)) {
                listViewUsers.setItemChecked(index, true)
            }
        }

        listViewUsers.choiceMode = ListView.CHOICE_MODE_MULTIPLE
    }

    private fun saveTask() {
        // Retrieve updated task details from the UI
        val updatedTaskName = editTextTaskName.text.toString().trim()
        val updatedTaskDescription = editTextTaskDescription.text.toString().trim()
        val updatedCompletionPercentage = editTextCompletionPercentage.text.toString().toIntOrNull() ?: 0

        // Perform validation on the task name
        if (updatedTaskName.isEmpty()) {
            textInputLayoutTaskName.error = "Task name cannot be empty"
            return
        } else {
            textInputLayoutTaskName.error = null
        }

        // Get selected users from the ListView
        val selectedUsers = mutableListOf<String>()
        for (i in 0 until listViewUsers.count) {
            if (listViewUsers.isItemChecked(i)) {
                selectedUsers.add(userList[i].id ?: "")
            }
        }

        // Update task details in Firestore
        val taskRef = db.collection("tasks").document(taskId)
        taskRef.update(
            mapOf(
                "name" to updatedTaskName,
                "description" to updatedTaskDescription,
                "completionPercentage" to updatedCompletionPercentage,
                "assignedUsers" to selectedUsers
            )
        ).addOnSuccessListener {
            // Successfully updated task details
            Toast.makeText(this, "Task updated successfully", Toast.LENGTH_SHORT).show()

            // Pass back the updated task details to TaskActivity
            val intent = Intent()
            intent.putExtra("updatedTaskName", updatedTaskName)
            intent.putExtra("updatedTaskDescription", updatedTaskDescription)
            intent.putExtra("updatedCompletionPercentage", updatedCompletionPercentage)
            intent.putStringArrayListExtra("updatedAssignedUsers", ArrayList(selectedUsers))
            setResult(Activity.RESULT_OK, intent)
            finish()
        }.addOnFailureListener { e ->
            // Handle error updating task details
            Log.e(TAG, "Error updating task", e)
            Toast.makeText(this, "Failed to update task", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
        // Redirect to MainActivity after saving task
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("projectId", projectId)
        startActivity(intent)
    }

    private fun deleteTask() {
        // Delete the task document from Firestore
        val taskRef = db.collection("tasks").document(taskId)
        taskRef.delete().addOnSuccessListener {
            // Remove task reference from the project document
            val projectRef = db.collection("projects").document(projectId)
            projectRef.update("tasks", FieldValue.arrayRemove(taskId)).addOnSuccessListener {
                // Successfully deleted task and removed reference from project
                Toast.makeText(this, "Task deleted successfully", Toast.LENGTH_SHORT).show()

                // Close the current activity
                finish()

                // Redirect to MainActivity
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("projectId", projectId)
                startActivity(intent)
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error removing task reference from project", e)
                Toast.makeText(this, "Failed to delete task reference from project", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error deleting task", e)
            Toast.makeText(this, "Failed to delete task", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val TAG = "EditTaskActivity"
    }
}
