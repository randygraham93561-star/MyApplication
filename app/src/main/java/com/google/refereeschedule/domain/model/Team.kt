package com.google.refereeschedule.domain.model

data class Team(
    val id: String = "",
    val name: String = "", // Used as the generated Team ID (e.g. 12UB-05)
    val seasonId: String = "",
    val divisionName: String = "",
    val gender: String = "Boys", // Boys, Girls, Coed
    val subDivision: String? = null,
    val coachEmail: String? = null,
    val totalPoints: Int = 0,
    val organizationId: String = ""
)
