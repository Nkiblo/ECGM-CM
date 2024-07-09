package com.example.ecgm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

// Adapter for displaying observations in a RecyclerView
class ObservationsAdapter(private var observations: List<Observation>) :
    RecyclerView.Adapter<ObservationsAdapter.ObservationViewHolder>() {

    // ViewHolder for each observation item
    class ObservationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val descriptionTextView: TextView = itemView.findViewById(R.id.textViewObservationDescription)
        val imageView: ImageView = itemView.findViewById(R.id.imageViewObservation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObservationViewHolder {
        // Inflate the layout for each observation item
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_observations, parent, false)
        return ObservationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ObservationViewHolder, position: Int) {
        // Bind data to the ViewHolder
        val observation = observations[position]
        holder.descriptionTextView.text = observation.description

        // Load image using Glide if image URI is available, hide ImageView otherwise
        if (observation.imageUri != null) {
            Glide.with(holder.itemView.context)
                .load(observation.imageUri)
                .into(holder.imageView)
            holder.imageView.visibility = View.VISIBLE
        } else {
            holder.imageView.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        // Return the number of observations
        return observations.size
    }

    // Update the list of observations and notify the adapter of the change
    fun updateObservations(newObservations: List<Observation>) {
        observations = newObservations
        notifyDataSetChanged()
    }
}
