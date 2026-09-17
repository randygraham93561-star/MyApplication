package com.google.refereeschedule.domain.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

enum class UserRole {
    Referee, Admin, Mentor, SystemAdmin, CoachAdmin
}

enum class SubscriptionTier {
    Free, Base, Standard, Pro
}

data class User(
    val id: String = "",
    val email: String = "",
    
    // Map of app-name to role (e.g., {"referee": "Admin", "teams": "Coach"})
    @get:PropertyName("roles") @set:PropertyName("roles")
    var roles: Map<String, String> = emptyMap(),
    
    // Fallback for the old single-string role field
    @get:PropertyName("role") @set:PropertyName("role")
    var legacyRole: String? = null,
    
    val organizationId: String? = null,
    val adminOrgIds: List<String> = emptyList(), // Organizations where user is an Admin
    val coachOrgIds: List<String> = emptyList(), // Organizations where user is a Coach Admin
    val isAccountLocked: Boolean = false
) {
    @get:Exclude
    val userRole: UserRole
        get() {
            // 1. Try "referee" key in roles map
            // 2. Try "role" key in roles map (common manual entry typo)
            // 3. Try legacy string field
            val rawRole = roles["referee"] ?: roles["role"] ?: legacyRole ?: "Referee"
            
            return when (rawRole.lowercase().trim()) {
                "admin", "refereeadmin", "referee_admin" -> UserRole.Admin
                "systemadmin", "system_admin", "superadmin" -> UserRole.SystemAdmin
                "mentor" -> UserRole.Mentor
                "coachadmin", "coach_admin" -> UserRole.CoachAdmin
                else -> UserRole.Referee
            }
        }
}
