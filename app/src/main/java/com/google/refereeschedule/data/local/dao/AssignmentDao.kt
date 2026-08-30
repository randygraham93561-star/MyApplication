package com.google.refereeschedule.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.google.refereeschedule.data.local.entity.AssignmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentDao {
    @Query("SELECT * FROM assignments")
    fun getAllAssignments(): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE organizationId = :organizationId")
    fun getAssignmentsForOrganization(organizationId: String): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE refereeId = :refereeId")
    fun getAssignmentsForReferee(refereeId: String): Flow<List<AssignmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignments(assignments: List<AssignmentEntity>)

    @Query("DELETE FROM assignments")
    suspend fun clearAll()

    @Query("DELETE FROM assignments WHERE id = :id")
    suspend fun deleteAssignment(id: String)
}
