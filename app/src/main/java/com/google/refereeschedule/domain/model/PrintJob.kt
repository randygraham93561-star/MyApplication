package com.google.refereeschedule.domain.model

import java.util.Date

enum class PrintJobType {
    LunchVoucher,
    MatchSchedule,
    MatchReport
}

enum class PrintJobStatus {
    Pending,
    Processing,
    Completed,
    Failed
}

data class PrintJob(
    val id: String = "",
    val organizationId: String = "",
    val refereeId: String = "",
    val type: String = "LunchVoucher", // Stored as String for database stability
    val templateId: String = "",
    val renderedContent: String = "",
    val status: String = "Pending", // Stored as String to avoid serialization bounces
    val data: Map<String, String> = emptyMap(),
    val timestamp: Date = Date(),
    val errorMessage: String? = null
)
