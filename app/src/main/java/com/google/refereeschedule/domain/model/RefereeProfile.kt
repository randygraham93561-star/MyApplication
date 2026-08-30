package com.google.refereeschedule.domain.model

import com.google.firebase.firestore.PropertyName

enum class PointDistributionMode {
    Even, Manual, NeedsBased
}

data class RefereeProfile(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
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
    @get:PropertyName("active") @set:PropertyName("active")
    var isActive: Boolean = true,
    val organizationId: String = "",
    val badgeCorrectionRequested: Boolean = false
)
