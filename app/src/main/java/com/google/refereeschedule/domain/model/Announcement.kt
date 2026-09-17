package com.google.refereeschedule.domain.model

import java.util.Date

data class Announcement(
    val id: String = "",
    val organizationId: String = "",
    val senderId: String = "",
    val recipientIds: List<String> = emptyList(), // Empty means organization-wide (Broadcast)
    val title: String = "",
    val message: String = "",
    val timestamp: Date = Date()
)
