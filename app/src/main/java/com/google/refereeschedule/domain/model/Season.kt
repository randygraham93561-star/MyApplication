package com.google.refereeschedule.domain.model

import com.google.firebase.firestore.PropertyName
import java.util.Date

data class Division(
    val name: String = "",
    val teamsAccumulatePoints: Boolean = true,
    val mentorsAllowed: Boolean = true,
    @get:PropertyName("friendlyByDefault")
    val isFriendlyByDefault: Boolean = false,
    val playersPerTeam: Int = 11,
    val halfDurationMinutes: Int = 45,
    val ballSize: Int = 5,
    val difficultyLevel: Int = 0 // Linked to DivisionDifficulty levels
)

data class Season(
    val id: String = "",
    val name: String = "",
    val startDate: Date = Date(),
    val endDate: Date = Date(),
    @get:PropertyName("active")
    val isActive: Boolean = true,
    val organizationId: String = "",
    val collectPoints: Boolean = false,
    val maxPointsPerWeekend: Int = 0,
    val centerRefereePoints: Int = 0,
    val assistantRefereePoints: Int = 0,
    val divisions: List<Division> = emptyList()
)
