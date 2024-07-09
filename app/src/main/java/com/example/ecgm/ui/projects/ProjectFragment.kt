package com.example.ecgm.ui.projects

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecgm.FinishedProjectActivity
import com.example.ecgm.FinishedProjectActivity.Companion.TAG
import com.example.ecgm.FinishedProjectsAdapter
import com.example.ecgm.Project
import com.example.ecgm.databinding.FragmentProjectsBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ProjectFragment : Fragment() {

    private var _binding: FragmentProjectsBinding? = null
    private val binding get() = _binding!!

    private lateinit var projectsAdapter: FinishedProjectsAdapter
    private lateinit var db: FirebaseFirestore
    private var projectsListener: ListenerRegistration? = null
    private var userId: String? = null
    private var userRole: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Retrieve user information from arguments
        userId = arguments?.getString("userId")
        userRole = arguments?.getString("userRole")

        // Initialize Firestore instance
        db = FirebaseFirestore.getInstance()

        // Setup RecyclerView with FinishedProjectsAdapter
        setupRecyclerView()

        // Load finished projects based on user role
        loadFinishedProjects()
        Log.d(TAG, "THIS IS THE PROJECT FRAGMENT")

        return root
    }

    private fun setupRecyclerView() {
        projectsAdapter = FinishedProjectsAdapter(mutableListOf())
        binding.recyclerViewFinishedProjects.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = projectsAdapter
        }

        // Set item click listener for the adapter
        projectsAdapter.setOnItemClickListener(object : FinishedProjectsAdapter.OnItemClickListener {
            override fun onItemClick(project: Project) {
                val intent = Intent(activity, FinishedProjectActivity::class.java)
                intent.putExtra("projectId", project.id)
                startActivity(intent)
            }
        })
    }

    private fun loadFinishedProjects() {
        // Query Firestore based on user role
        val query = if (userRole == "admin") {
            db.collection("finishedProjects")
        } else {
            db.collection("finishedProjects").whereArrayContains("projectUsers", userId ?: "")
        }

        // Set up snapshot listener for real-time updates
        projectsListener = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("ProjectFragment", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val projects = snapshot.documents.mapNotNull { it.toObject(Project::class.java) }
                projectsAdapter.updateProjects(projects)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove Firestore listener when view is destroyed
        projectsListener?.remove()
        _binding = null
    }
}
