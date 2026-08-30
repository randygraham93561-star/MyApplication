package com.google.refereeschedule.domain.model

data class Team(
    val id: String = "",
    val name: String = "",
    val seasonId: String = "",
    val divisionName: String = "",
    val totalPoints: Int = 0,
    val organizationId: String = ""
)
