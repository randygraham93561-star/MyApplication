package com.google.refereeschedule.domain.model

import java.util.Date

data class PointAward(
    val id: String = "",
    val gameId: String = "",
    val refereeId: String = "",
    val teamId: String = "",
    val points: Int = 0,
    val timestamp: Date = Date(),
    val organizationId: String = "",
    val seasonId: String = ""
)
