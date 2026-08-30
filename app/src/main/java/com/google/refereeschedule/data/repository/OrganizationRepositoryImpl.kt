package com.google.refereeschedule.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.refereeschedule.domain.model.Organization
import com.google.refereeschedule.domain.repository.OrganizationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class OrganizationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : OrganizationRepository {

    private val organizationsCollection = firestore.collection("organizations")

    override fun getOrganizationsFlow(): Flow<List<Organization>> {
        return organizationsCollection.snapshots().map { snapshot ->
            snapshot.toObjects(Organization::class.java)
        }
    }

    override suspend fun getOrganization(id: String): Organization? {
        if (id.isEmpty()) return null
        return try {
            organizationsCollection.document(id).get().await().toObject(Organization::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveOrganization(organization: Organization) {
        val orgToSave = if (organization.id.isEmpty()) {
            val docRef = organizationsCollection.document()
            organization.copy(id = docRef.id)
        } else {
            organization
        }
        organizationsCollection.document(orgToSave.id).set(orgToSave).await()
    }

    override suspend fun deleteOrganization(id: String) {
        if (id.isEmpty()) return
        organizationsCollection.document(id).delete().await()
    }
}
