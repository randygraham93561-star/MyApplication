package com.google.refereeschedule.domain.model

import com.google.firebase.firestore.PropertyName
import java.util.Date

enum class GameStatus {
    Open, PartiallyFilled, Full, PendingReview, ReportApproved, Completed
}

data class RefereeVerification(
    val refereeId: String = "", // The originally assigned referee
    val isPresent: Boolean = false,
    val correctRole: Boolean = false,
    val actualRefereeId: String? = null, // The ID of the referee who actually showed up
    val actualRole: AssignmentPosition? = null // The role they actually performed
)

data class Game(
    val id: String = "",
    val gameNumber: Int = 0,
    val date: Date = Date(),
    val time: String = "",
    val location: String = "",
    val fieldNumber: String = "",
    val ageGroup: String = "",
    val divisionName: String = "",
    val homeTeamName: String = "",
    val awayTeamName: String = "",
    val requiredCrewSize: Int = 0,
    val difficultyLevel: Int = 1,
    val seasonId: String = "",
    val status: GameStatus = GameStatus.Open,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val cards: String? = null,
    val notes: String? = null,
    val cardsShown: Boolean = false,
    val cardTypes: String = "", // "Yellow", "Red", "Both"
    val disciplinaryDescription: String = "",
    val reporterSignature: String = "",
    val selectedTargetTeamId: String = "", // Persistent manual team choice for points
    val refereeVerifications: List<RefereeVerification> = emptyList(),
    val reportSubmittedAt: Date? = null,
    val reportSubmittedBy: String? = null,
    val organizationId: String = "",
    @get:PropertyName("mentorsAllowed") @set:PropertyName("mentorsAllowed")
    var mentorsAllowed: Boolean = true,
    @get:PropertyName("friendly") @set:PropertyName("friendly")
    var isFriendly: Boolean = false
)
