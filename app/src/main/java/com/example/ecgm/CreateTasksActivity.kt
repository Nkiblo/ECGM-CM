package com.example.ecgm

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ecgm.EditTaskActivity.Companion.TAG
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CreateTasksActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var projectId: String
    private lateinit var listViewUsers: ListView
    private lateinit var userList: MutableList<User>
    private lateinit var assignedUsers: MutableList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_tasks)

        db = FirebaseFirestore.getInstance()

        assignedUsers = intent.getStringArrayListExtra("assignedUsers") ?: mutableListOf()
        projectId = intent.getStringExtra("projectId") ?: ""

        val buttonCreateTask = findViewById<Button>(R.id.buttonCreateTask)
        val editTextTaskName = findViewById<EditText>(R.id.editTextTaskName)
        val editTextTaskDescription = findViewById<EditText>(R.id.editTextTaskDescription)
        listViewUsers = findViewById(R.id.listViewUsers)

        userList = mutableListOf()

        fetchUsers()

        buttonCreateTask.setOnClickListener {
            val taskName = editTextTaskName.text.toString()
            val taskDescription = editTextTaskDescription.text.toString()

            if (taskName.isNotEmpty()) {
                createTask(taskName, taskDescription)
            } else {
                Toast.makeText(this, "Task name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchUsers() {
        // Fetch project details to get projectUsers
        db.collection("projects")
            .whereEqualTo("id", projectId)
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

    private fun createTask(name: String, description: String) {
        val selectedUsers = mutableListOf<String>()
        for (i in 0 until listViewUsers.count) {
            if (listViewUsers.isItemChecked(i)) {
                selectedUsers.add(userList[i].id ?: "")
            }
        }

        val task = hashMapOf(
            "projectId" to projectId,
            "name" to name,
            "description" to description,
            "completionPercentage" to 0,
            "assignedUsers" to selectedUsers
        )

        db.collection("tasks")
            .add(task)
            .addOnSuccessListener { documentReference ->
                val taskId = documentReference.id
                documentReference.update("id", taskId)
                    .addOnSuccessListener {
                        updateProjectWithTask(taskId)
                    }
                    .addOnFailureListener { e ->
                        Log.e("CreateTasksActivity", "Error updating task with id", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("CreateTasksActivity", "Error creating task", e)
            }
    }

    private fun updateProjectWithTask(taskId: String) {
        db.collection("projects")
            .whereEqualTo("id", projectId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val projectRef = db.collection("projects").document(document.id)
                    projectRef.update("tasks", FieldValue.arrayUnion(taskId))
                        .addOnSuccessListener {
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e("CreateTasksActivity", "Error updating project with task", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("CreateTasksActivity", "Error fetching project", e)
            }
    }
}
