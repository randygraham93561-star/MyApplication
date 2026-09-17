package com.google.refereeschedule.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.refereeschedule.domain.model.ChatMessage
import com.google.refereeschedule.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChatRepository {

    private val chatCollection = firestore.collection("match_chats")

    override fun getMessages(channelId: String): Flow<List<ChatMessage>> {
        return chatCollection
            .whereEqualTo("channelId", channelId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(ChatMessage::class.java)?.copy(id = it.id) }
            }
    }

    override suspend fun sendMessage(message: ChatMessage) {
        val docRef = chatCollection.document()
        val finalMessage = message.copy(id = docRef.id)
        docRef.set(finalMessage).await()
    }
}
