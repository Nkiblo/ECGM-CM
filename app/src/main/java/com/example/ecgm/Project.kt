package com.example.ecgm
import java.io.Serializable

data class Project(
    var description: String = "",
    var id: String = "",
    var imageUrl: String = "",
    var managerId: String = "",
    var name: String = "",
    var projectUsers: List<String> = mutableListOf(managerId),
    var tasks: List<String> = mutableListOf(id)
): Serializable {

}
