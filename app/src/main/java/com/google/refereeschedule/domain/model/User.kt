package com.google.refereeschedule.domain.model

enum class UserRole {
    Referee, Admin, Mentor, SystemAdmin
}

data class User(
    val id: String = "",
    val email: String = "",
    val role: UserRole = UserRole.Referee,
    val organizationId: String? = null
)
