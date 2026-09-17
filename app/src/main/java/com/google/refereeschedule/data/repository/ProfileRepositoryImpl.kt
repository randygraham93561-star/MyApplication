package com.google.refereeschedule.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.refereeschedule.domain.model.RefereeProfile
import com.google.refereeschedule.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ProfileRepository {

    private val profilesCollection = firestore.collection("profiles")

    override suspend fun getProfile(id: String): RefereeProfile? {
        if (id.isEmpty()) return null
        return try {
            profilesCollection.document(id).get().await().toObject(RefereeProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveProfile(profile: RefereeProfile) {
        if (profile.id.isEmpty()) return
        profilesCollection.document(profile.id).set(profile).await()
    }

    override fun getProfileFlow(id: String): Flow<RefereeProfile?> {
        if (id.isEmpty()) return flowOf(null)
        return profilesCollection.document(id).snapshots().map { snapshot ->
            snapshot.toObject(RefereeProfile::class.java)
        }
    }

    override fun getProfilesForOrganizationFlow(organizationId: String): Flow<List<RefereeProfile>> {
        return profilesCollection.whereEqualTo("organizationId", organizationId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(RefereeProfile::class.java)?.copy(id = doc.id)
                }
            }
    }

    override fun getAllProfilesFlow(): Flow<List<RefereeProfile>> {
        return profilesCollection.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(RefereeProfile::class.java)?.copy(id = doc.id)
            }
        }
    }

    override suspend fun updatePoints(id: String, pointsToAdd: Int) {
        val profile = getProfile(id)
        if (profile != null) {
            saveProfile(profile.copy(totalPoints = profile.totalPoints + pointsToAdd))
        }
    }
}
