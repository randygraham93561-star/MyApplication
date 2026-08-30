package com.google.refereeschedule.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.refereeschedule.domain.model.Assignment
import com.google.refereeschedule.domain.model.AssignmentPosition
import com.google.refereeschedule.domain.model.AssignmentStatus
import java.util.Date

@Entity(tableName = "assignments")
data class AssignmentEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val refereeId: String,
    val position: String,
    val status: String,
    val checkedIn: Boolean,
    val timestamp: Long,
    val organizationId: String,
    val mentorRequested: Boolean
)

fun AssignmentEntity.toDomain() = Assignment(
    id = id,
    gameId = gameId,
    refereeId = refereeId,
    position = try { AssignmentPosition.valueOf(position) } catch (e: Exception) { AssignmentPosition.AssistantReferee },
    status = try { AssignmentStatus.valueOf(status) } catch (e: Exception) { AssignmentStatus.Pending },
    checkedIn = checkedIn,
    timestamp = Date(timestamp),
    organizationId = organizationId,
    mentorRequested = mentorRequested
)

fun Assignment.toEntity() = AssignmentEntity(
    id = id,
    gameId = gameId,
    refereeId = refereeId,
    position = position.name,
    status = status.name,
    checkedIn = checkedIn,
    timestamp = timestamp.time,
    organizationId = organizationId,
    mentorRequested = mentorRequested
)
