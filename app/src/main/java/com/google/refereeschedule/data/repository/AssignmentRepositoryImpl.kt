package com.google.refereeschedule.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.refereeschedule.data.local.dao.AssignmentDao
import com.google.refereeschedule.data.local.entity.toDomain
import com.google.refereeschedule.data.local.entity.toEntity
import com.google.refereeschedule.domain.model.Assignment
import com.google.refereeschedule.domain.repository.AssignmentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AssignmentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val assignmentDao: AssignmentDao
) : AssignmentRepository {

    private val assignmentsCollection = firestore.collection("assignments")
    private val externalScope = CoroutineScope(Dispatchers.IO)
    private var syncJob: kotlinx.coroutines.Job? = null

    override fun getAllAssignmentsFlow(): Flow<List<Assignment>> {
        assignmentsCollection.snapshots().onEach { snapshot ->
            try {
                val assignments = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Assignment::class.java)?.copy(id = doc.id)
                }
                assignmentDao.insertAssignments(assignments.map { it.toEntity() })
            } catch (e: Exception) {
            }
        }.launchIn(externalScope)

        return assignmentDao.getAllAssignments().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAssignmentsForOrganizationFlow(organizationId: String): Flow<List<Assignment>> {
        return assignmentDao.getAssignmentsForOrganization(organizationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAssignmentsForGame(gameId: String): Flow<List<Assignment>> {
        return assignmentsCollection.whereEqualTo("gameId", gameId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Assignment::class.java)?.copy(id = doc.id)
                }
            }
    }

    override fun getAssignmentsForReferee(refereeId: String): Flow<List<Assignment>> {
        if (refereeId.isEmpty()) return flowOf(emptyList())
        if (syncJob == null || syncJob?.isActive == false) {
            syncJob = assignmentsCollection.whereEqualTo("refereeId", refereeId)
                .snapshots()
                .onEach { snapshot ->
                    try {
                        val assignments = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Assignment::class.java)?.copy(id = doc.id)
                        }
                        assignmentDao.insertAssignments(assignments.map { it.toEntity() })
                    } catch (e: Exception) {
                    }
                }.launchIn(externalScope)
        }

        return assignmentDao.getAssignmentsForReferee(refereeId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAssignment(id: String): Assignment? {
        return try {
            assignmentsCollection.document(id).get().await().toObject(Assignment::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveAssignment(assignment: Assignment) {
        val assignmentToSave = if (assignment.id.isEmpty()) {
            val docRef = assignmentsCollection.document()
            assignment.copy(id = docRef.id)
        } else {
            assignment
        }
        
        try {
            assignmentsCollection.document(assignmentToSave.id).set(assignmentToSave).await()
        } catch (e: Exception) {
        }
        assignmentDao.insertAssignments(listOf(assignmentToSave.toEntity()))
    }

    override suspend fun deleteAssignment(id: String) {
        if (id.isEmpty()) return
        try {
            assignmentsCollection.document(id).delete().await()
        } catch (e: Exception) {
        }
        assignmentDao.deleteAssignment(id)
    }
}
