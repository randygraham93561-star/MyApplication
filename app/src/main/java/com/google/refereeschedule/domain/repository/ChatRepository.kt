package com.google.refereeschedule.domain.repository

import com.google.refereeschedule.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(channelId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(message: ChatMessage)
}
