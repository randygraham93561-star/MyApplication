package com.google.refereeschedule.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.refereeschedule.domain.model.Season
import com.google.refereeschedule.domain.repository.SeasonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SeasonRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SeasonRepository {

    private val seasonsCollection = firestore.collection("seasons")

    override suspend fun getActiveSeason(): Season? {
        return try {
            seasonsCollection.whereEqualTo("active", true)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.let { it.toObject(Season::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getAllSeasons(): List<Season> {
        return seasonsCollection.get().await().documents.mapNotNull { doc ->
            doc.toObject(Season::class.java)?.copy(id = doc.id)
        }
    }

    override fun getSeasonsFlow(): Flow<List<Season>> {
        return seasonsCollection.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Season::class.java)?.copy(id = doc.id)
            }
        }
    }

    override fun getSeasonsForOrganizationFlow(organizationId: String): Flow<List<Season>> {
        return seasonsCollection.whereEqualTo("organizationId", organizationId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Season::class.java)?.copy(id = doc.id)
                }
            }
    }

    override suspend fun saveSeason(season: Season) {
        if (season.id.isEmpty()) {
            val docRef = seasonsCollection.document()
            docRef.set(season.copy(id = docRef.id)).await()
        } else {
            seasonsCollection.document(season.id).set(season).await()
        }
    }

    override suspend fun deleteSeason(id: String) {
        if (id.isEmpty()) return
        seasonsCollection.document(id).delete().await()
    }
}
