package com.example.ecgm

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class ContinueTaskActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var taskId: String
    private lateinit var projectId: String

    // UI elements
    private lateinit var textViewModificationDateTime: TextView
    private lateinit var buttonChooseDateTime: Button
    private lateinit var editTextLocation: EditText
    private lateinit var editTextTimeSpent: EditText
    private lateinit var textViewObservations: TextView
    private lateinit var buttonSaveContinueTask: Button
    private lateinit var buttonAddObservation: Button
    private lateinit var observationsLayout: LinearLayout
    private lateinit var editTextCompletionPercentage: EditText

    private val observationsList = mutableListOf<Observation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_continue_task)

        // Initialize Firestore and Firebase Storage
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize UI elements
        textViewModificationDateTime = findViewById(R.id.textViewModificationDate)
        buttonChooseDateTime = findViewById(R.id.buttonChooseDate)
        editTextLocation = findViewById(R.id.editTextLocation)
        editTextTimeSpent = findViewById(R.id.editTextTimeSpent)
        textViewObservations = findViewById(R.id.textViewObservations)
        buttonSaveContinueTask = findViewById(R.id.buttonSaveContinueTask)
        buttonAddObservation = findViewById(R.id.buttonAddObservation)
        observationsLayout = findViewById(R.id.observationsLayout)
        editTextCompletionPercentage = findViewById(R.id.editTextCompletionPercentage)

        // Set click listeners for buttons
        buttonChooseDateTime.setOnClickListener {
            showDateTimePicker()
        }

        buttonAddObservation.setOnClickListener {
            addObservation()
        }

        buttonSaveContinueTask.setOnClickListener {
            saveContinueTask()
        }

        // Retrieve task details from intent
        taskId = intent.getStringExtra("taskId") ?: ""
        projectId = intent.getStringExtra("projectId") ?: ""

        // Optional: You can set default values for editText fields if needed
        editTextTimeSpent.setText("")
    }

    // Show date and time picker dialog
    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(this, { _, year, month, day ->
            calendar.set(year, month, day)
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                calendar.set(Calendar.MINUTE, selectedMinute)
                updateDateTime(calendar.time)
            }, hour, minute, true)
            timePickerDialog.show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    // Update the displayed date and time
    private fun updateDateTime(dateTime: Date) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        textViewModificationDateTime.text = dateFormat.format(dateTime)
    }

    // Add an observation to the task
    private fun addObservation() {
        val observationId = UUID.randomUUID().toString()
        val observation = Observation(observationId, "", "") // Provide initial empty description and imageUri
        observationsList.add(observation)
        renderObservation(observation)
    }

    // Render observation UI
    private fun renderObservation(observation: Observation) {
        val observationView = layoutInflater.inflate(R.layout.item_observation, null)
        val editTextObservationDesc = observationView.findViewById<EditText>(R.id.editTextObservationDesc)
        val imageViewObservation = observationView.findViewById<ImageView>(R.id.imageViewObservation)
        val buttonChooseImage = observationView.findViewById<Button>(R.id.buttonChooseImage)
        val buttonRemoveObservation = observationView.findViewById<Button>(R.id.buttonRemoveObservation)

        editTextObservationDesc.setText(observation.description)
        if (observation.imageUri?.isNotEmpty() == true) {
            imageViewObservation.visibility = View.VISIBLE
            Glide.with(this).load(observation.imageUri).into(imageViewObservation)
        }

        buttonChooseImage.setOnClickListener {
            chooseImageForObservation(observation)
        }
        buttonRemoveObservation.setOnClickListener {
            observationsList.remove(observation)
            observationsLayout.removeView(observationView)
        }

        // Update the description in the Observation object when text changes
        editTextObservationDesc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                observation.description = s.toString() // Update the description in the Observation object
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        observationsLayout.addView(observationView)
        Log.d(TAG, "Observation ${observation.id}: Description = ${observation.description}")
    }

    // Choose an image for the observation
    private fun chooseImageForObservation(observation: Observation) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    // Handle the result from image picker
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri = data.data
            selectedImageUri?.let {
                uploadImageToFirebaseStorage(it)
            }
        }
    }

    // Upload the selected image to Firebase Storage
    private fun uploadImageToFirebaseStorage(imageUri: Uri) {
        val storageRef = storage.reference
        val imageRef = storageRef.child("images/${UUID.randomUUID()}.jpg")
        val uploadTask = imageRef.putFile(imageUri)

        uploadTask.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                handleImageSelected(downloadUrl.toString())
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error uploading image", e)
            showToast("Failed to upload image")
        }
    }

    // Handle the selected image and associate it with the last observation
    private fun handleImageSelected(imageUrl: String) {
        // Find the index of the last observation
        val lastIndex = observationsList.lastIndex

        if (lastIndex >= 0) {
            val lastObservation = observationsList[lastIndex]
            lastObservation.imageUri = imageUrl

            // Update the ImageView in the corresponding observation view
            val observationView = observationsLayout.getChildAt(lastIndex)
            val imageViewObservation = observationView.findViewById<ImageView>(R.id.imageViewObservation)
            imageViewObservation.visibility = View.VISIBLE
            Glide.with(this).load(imageUrl).into(imageViewObservation)
        }
    }

    // Save the task continuation details
    private fun saveContinueTask() {
        val location = editTextLocation.text.toString().trim()
        val newTimeSpent = editTextTimeSpent.text.toString().toDoubleOrNull() ?: 0.0

        // Retrieve completion percentage from EditText
        val completionPercentageInput = editTextCompletionPercentage.text.toString().toDoubleOrNull() ?: 0.0

        // Ensure completionPercentage is within valid range (0-100)
        val completionPercentage = when {
            completionPercentageInput < 0 -> 0.0
            completionPercentageInput > 100 -> 100.0
            else -> completionPercentageInput
        }

        // Reference to the task document in Firestore
        val taskRef = db.collection("tasks").document(taskId)

        // Create a batch to execute multiple writes atomically
        val batch = db.batch()

        // Fetch the current task document to get existing data
        taskRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Retrieve existing data
                    val currentSpent = documentSnapshot.getDouble("timeSpent") ?: 0.0

                    // Calculate new total timeSpent
                    val totalTimeSpent = currentSpent + newTimeSpent

                    // Update main task document fields in the batch
                    batch.update(taskRef, "lastModifiedDate", Calendar.getInstance().time)
                    batch.update(taskRef, "location", location)
                    batch.update(taskRef, "timeSpent", totalTimeSpent)
                    batch.update(taskRef, "completionPercentage", completionPercentage)

                    // Save each observation in the observations subcollection
                    observationsList.forEachIndexed { index, observation ->
                        val observationData = hashMapOf(
                            "id" to observation.id,
                            "description" to observation.description,
                            "imageUri" to observation.imageUri
                        )

                        // Reference to a new observation document in the subcollection
                        val observationRef = taskRef.collection("observations").document(observation.id)

                        // Set the observation data within the batch
                        batch.set(observationRef, observationData)
                    }

                    // Commit the batch
                    batch.commit()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Task details and observations updated successfully", Toast.LENGTH_SHORT).show()
                            setResult(Activity.RESULT_OK) // Set result indicating success
                            finish() // Finish this activity
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error updating task details and observations", e)
                            Toast.makeText(this, "Failed to update task details and observations", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Log.d(TAG, "Document does not exist")
                    Toast.makeText(this, "Task document does not exist", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching document", e)
                Toast.makeText(this, "Failed to fetch task details", Toast.LENGTH_SHORT).show()
            }
    }

    // Show a toast message
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val TAG = "ContinueTaskActivity"
        const val REQUEST_IMAGE_PICK = 100
    }
}
