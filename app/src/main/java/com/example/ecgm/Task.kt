package com.example.ecgm

data class Task(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val completionPercentage: Int = 0,
    val projectId: String ="",
    val assignedUsers: List<String> = emptyList()
) {
}
