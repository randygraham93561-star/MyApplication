package com.google.refereeschedule.domain.model

import java.util.Date

enum class AssignmentPosition {
    HeadReferee, AssistantReferee, Mentor
}

enum class AssignmentStatus {
    Pending, Confirmed
}

data class Assignment(
    val id: String = "",
    val gameId: String = "",
    val refereeId: String = "",
    val position: AssignmentPosition = AssignmentPosition.AssistantReferee,
    val status: AssignmentStatus = AssignmentStatus.Pending,
    val checkedIn: Boolean = false,
    val timestamp: Date = Date(),
    val organizationId: String = "",
    val mentorRequested: Boolean = false
)
