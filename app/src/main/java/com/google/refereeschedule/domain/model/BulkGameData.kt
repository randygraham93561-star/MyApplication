package com.google.refereeschedule.domain.model

data class BulkGameData(
    val homeTeam: Team,
    val awayTeam: Team,
    val time: String,
    val location: String,
    val fieldNumber: String,
    val isFriendly: Boolean
)
