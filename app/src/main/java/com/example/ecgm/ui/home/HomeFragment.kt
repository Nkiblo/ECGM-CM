package com.example.ecgm.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecgm.Project
import com.example.ecgm.ProjectActivity
import com.example.ecgm.ProjectsAdapter
import com.example.ecgm.databinding.FragmentHomeBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var projectsAdapter: ProjectsAdapter
    private lateinit var db: FirebaseFirestore
    private var projectsListener: ListenerRegistration? = null
    private var userId: String? = null
    private var userRole: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Retrieve user information from arguments
        userId = arguments?.getString("userId")
        userRole = arguments?.getString("userRole")

        // Log the retrieved user info for debugging
        Log.d("HomeFragment", "User ID: $userId")
        Log.d("HomeFragment", "User Role: $userRole")

        // Initialize Firestore instance
        db = FirebaseFirestore.getInstance()

        // Setup RecyclerView with ProjectsAdapter
        setupRecyclerView()

        // Load projects based on the user role
        loadProjects()

        return root
    }

    private fun setupRecyclerView() {
        projectsAdapter = ProjectsAdapter(mutableListOf())
        binding.recyclerViewProjects.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = projectsAdapter
        }

        // Set item click listener for the adapter
        projectsAdapter.setOnItemClickListener(object : ProjectsAdapter.OnItemClickListener {
            override fun onItemClick(project: Project) {
                val intent = Intent(activity, ProjectActivity::class.java)
                intent.putExtra("projectId", project.id)
                startActivity(intent)
            }
        })
    }

    private fun loadProjects() {
        // Query Firestore based on user role
        val query = if (userRole == "admin") {
            db.collection("projects")
        } else {
            db.collection("projects").whereArrayContains("projectUsers", userId ?: "")
        }

        // Set up snapshot listener for real-time updates
        projectsListener = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("HomeFragment", "Listen failed.", e)
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
