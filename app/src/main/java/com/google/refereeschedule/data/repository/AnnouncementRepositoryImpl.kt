package com.google.refereeschedule.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.refereeschedule.domain.model.Announcement
import com.google.refereeschedule.domain.repository.AnnouncementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AnnouncementRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AnnouncementRepository {

    private val announcementsCollection = firestore.collection("announcements")

    override fun getAnnouncementsForOrganization(organizationId: String): Flow<List<Announcement>> {
        // This is a simplified fetch. In production, we'd use a more complex query 
        // to filter by (organizationId == orgId AND (recipientIds contains myId OR recipientIds is empty))
        return announcementsCollection
            .whereEqualTo("organizationId", organizationId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(Announcement::class.java)?.copy(id = it.id) }
            }
    }

    override suspend fun sendAnnouncement(announcement: Announcement) {
        val docRef = announcementsCollection.document()
        val finalAnnouncement = announcement.copy(id = docRef.id)
        docRef.set(finalAnnouncement).await()
        
        // Note: In a real-world production app, a Firebase Cloud Function would be 
        // triggered by this Firestore write to send FCM notifications to all 
        // devices subscribed to the topic: "announcements_${announcement.organizationId}"
    }
}
