package com.google.refereeschedule.domain.model

object DivisionDifficulty {
    val headRefereeLevels = listOf(
        "8U Girls",
        "8U Boys",
        "10U Girls",
        "10U Boys",
        "12U Girls",
        "12U Boys",
        "14U Girls",
        "14U Boys",
        "19U Girls",
        "19U Boys"
    )

    // For Assistant Referee, excluding 8U
    val assistantRefereeLevels = headRefereeLevels.drop(2)

    fun getMinLevelForBadge(badgeLevel: String): Int {
        return when (badgeLevel.lowercase()) {
            "intermediate" -> 5 // 12U Boys
            "advanced" -> 7 // 14U Boys
            "national" -> 9 // 19U Boys
            else -> 0 // Regional has no restriction
        }
    }

    fun getLevelForDivision(divisionName: String): Int {
        val sanitized = divisionName.trim().lowercase()
        val index = headRefereeLevels.indexOfFirst { it.lowercase() == sanitized }
        return if (index != -1) index else {
            // Fallback for partial matches
            if (sanitized.contains("8u")) {
                if (sanitized.contains("girl")) 0 else 1
            } else if (sanitized.contains("10u")) {
                if (sanitized.contains("girl")) 2 else 3
            } else if (sanitized.contains("12u")) {
                if (sanitized.contains("girl")) 4 else 5
            } else if (sanitized.contains("14u")) {
                if (sanitized.contains("girl")) 6 else 7
            } else if (sanitized.contains("19u")) {
                if (sanitized.contains("girl")) 8 else 9
            } else 0
        }
    }
}
