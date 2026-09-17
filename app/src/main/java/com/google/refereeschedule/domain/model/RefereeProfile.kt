package com.google.refereeschedule.domain.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import java.util.Calendar
import java.util.Date

enum class PointDistributionMode {
    Even, Manual, NeedsBased
}

data class RefereeProfile(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val dateOfBirth: Date? = null,
    val badgeLevel: String = "",
    val headRefereeComfortLevel: Int = 0,
    val assistantRefereeComfortLevel: Int = 0,
    val totalPoints: Int = 0,
    val headRefereeGamesCount: Int = 0,
    val assistantRefereeGamesCount: Int = 0,
    val qualifications: List<String> = emptyList(),
    val currentSeasonId: String = "",
    val teamIdsForPoints: List<String> = emptyList(),
    val distributionMode: PointDistributionMode = PointDistributionMode.Even,
    @PropertyName("active")
    var isActive: Boolean = true,
    val organizationId: String = "",
    @PropertyName("badgeCorrectionRequested")
    val badgeCorrectionRequested: Boolean = false,
    @PropertyName("newReferee")
    val isNewReferee: Boolean = false,
    @PropertyName("hasTakenCourse")
    val hasTakenCourse: Boolean = false,
    @PropertyName("onboardingCompleted")
    val isOnboardingCompleted: Boolean = false,
    val lastOnboardedSeasonId: String = "",
    val completedSeasonQuizzes: List<String> = emptyList(), // List of season IDs where quiz was passed
    val lastQuizFailTimestamp: Long? = null,
    val fcmToken: String = "",
    @PropertyName("isMentor")
    val isMentor: Boolean = false,
    val yearsExperience: Int = 0,
    val isAdultOverride: Boolean? = null,
    @PropertyName("accountLocked")
    val isAccountLocked: Boolean = false
) {
    @get:Exclude
    val age: Int
        get() {
            val dob = dateOfBirth ?: return 0
            val cal = Calendar.getInstance()
            val now = Calendar.getInstance()
            cal.time = dob
            var age = now.get(Calendar.YEAR) - cal.get(Calendar.YEAR)
            if (now.get(Calendar.DAY_OF_YEAR) < cal.get(Calendar.DAY_OF_YEAR)) age--
            return age
        }

    @get:Exclude
    val isMinor: Boolean
        get() {
            if (isAdultOverride == true) return false
            if (isAdultOverride == false) return true
            return age > 0 && age < 18
        }
}
