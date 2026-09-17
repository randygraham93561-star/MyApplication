package com.google.refereeschedule.domain.model

import java.util.Date

data class ChatMessage(
    val id: String = "",
    val channelId: String = "", // Can be gameId or admin_ref_refereeId
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val timestamp: Date = Date()
)
