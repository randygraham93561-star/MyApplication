package com.google.refereeschedule.domain.repository

import com.google.refereeschedule.domain.model.Announcement
import kotlinx.coroutines.flow.Flow

interface AnnouncementRepository {
    fun getAnnouncementsForOrganization(organizationId: String): Flow<List<Announcement>>
    suspend fun sendAnnouncement(announcement: Announcement)
}
