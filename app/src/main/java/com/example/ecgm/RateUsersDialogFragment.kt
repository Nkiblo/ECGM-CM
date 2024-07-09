package com.example.ecgm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RateUsersDialogFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_rate_users, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views and setup RecyclerView, Button, etc.
        setupViews(view)
    }

    private fun setupViews(view: View) {
        val recyclerViewUsers: RecyclerView = view.findViewById(R.id.recyclerViewUsers)
        val btnSubmitRatings: Button = view.findViewById(R.id.btnSubmitRatings)

        // Setup RecyclerView and Button click listener
        recyclerViewUsers.layoutManager = LinearLayoutManager(requireContext())
        btnSubmitRatings.setOnClickListener {
            // Handle button click
            dismiss() // Dismiss the dialog when done
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
