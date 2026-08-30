package com.google.refereeschedule.domain.repository

import com.google.refereeschedule.domain.model.Assignment
import kotlinx.coroutines.flow.Flow

interface AssignmentRepository {
    fun getAllAssignmentsFlow(): Flow<List<Assignment>>
    fun getAssignmentsForOrganizationFlow(organizationId: String): Flow<List<Assignment>>
    fun getAssignmentsForGame(gameId: String): Flow<List<Assignment>>
    fun getAssignmentsForReferee(refereeId: String): Flow<List<Assignment>>
    suspend fun getAssignment(id: String): Assignment?
    suspend fun saveAssignment(assignment: Assignment)
    suspend fun deleteAssignment(id: String)
}
