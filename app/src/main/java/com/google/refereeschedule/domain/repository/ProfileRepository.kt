package com.google.refereeschedule.domain.repository

import com.google.refereeschedule.domain.model.RefereeProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    suspend fun getProfile(id: String): RefereeProfile?
    suspend fun saveProfile(profile: RefereeProfile)
    fun getProfileFlow(id: String): Flow<RefereeProfile?>
    fun getProfilesForOrganizationFlow(organizationId: String): Flow<List<RefereeProfile>>
    suspend fun updatePoints(id: String, pointsToAdd: Int)
}
