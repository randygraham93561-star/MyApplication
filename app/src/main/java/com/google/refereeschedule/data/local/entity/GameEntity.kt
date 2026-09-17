package com.google.refereeschedule.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.refereeschedule.domain.model.Game
import com.google.refereeschedule.domain.model.GameStatus
import com.google.refereeschedule.domain.model.RefereeVerification
import java.util.Date

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val gameNumber: Int,
    val date: Long,
    val time: String,
    val location: String,
    val fieldNumber: String,
    val ageGroup: String,
    val divisionName: String,
    val homeTeamName: String,
    val awayTeamName: String,
    val requiredCrewSize: Int,
    val difficultyLevel: Int,
    val seasonId: String,
    val status: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val cards: String?,
    val notes: String?,
    val cardsShown: Boolean,
    val cardTypes: String,
    val disciplinaryDescription: String,
    val reporterSignature: String,
    val selectedTargetTeamId: String,
    val refereeVerificationsJson: String, // Store as JSON string
    val reportSubmittedAt: Long?,
    val reportSubmittedBy: String?,
    val organizationId: String,
    val mentorsAllowed: Boolean,
    val isFriendly: Boolean
)

fun GameEntity.toDomain() = Game(
    id = id,
    gameNumber = gameNumber,
    date = Date(date),
    time = time,
    location = location,
    fieldNumber = fieldNumber,
    ageGroup = ageGroup,
    divisionName = divisionName,
    homeTeamName = homeTeamName,
    awayTeamName = awayTeamName,
    requiredCrewSize = requiredCrewSize,
    difficultyLevel = difficultyLevel,
    seasonId = seasonId,
    status = try { GameStatus.valueOf(status) } catch (e: Exception) { GameStatus.Open },
    homeScore = homeScore,
    awayScore = awayScore,
    cards = cards,
    notes = notes,
    cardsShown = cardsShown,
    cardTypes = cardTypes,
    disciplinaryDescription = disciplinaryDescription,
    reporterSignature = reporterSignature,
    selectedTargetTeamId = selectedTargetTeamId,
    refereeVerifications = deserializeVerifications(refereeVerificationsJson),
    reportSubmittedAt = reportSubmittedAt?.let { Date(it) },
    reportSubmittedBy = reportSubmittedBy,
    organizationId = organizationId,
    mentorsAllowed = mentorsAllowed,
    isFriendly = isFriendly
)

fun Game.toEntity() = GameEntity(
    id = id,
    gameNumber = gameNumber,
    date = date?.time ?: 0L,
    time = time,
    location = location,
    fieldNumber = fieldNumber,
    ageGroup = ageGroup,
    divisionName = divisionName,
    homeTeamName = homeTeamName,
    awayTeamName = awayTeamName,
    requiredCrewSize = requiredCrewSize,
    difficultyLevel = difficultyLevel,
    seasonId = seasonId,
    status = status.name,
    homeScore = homeScore,
    awayScore = awayScore,
    cards = cards,
    notes = notes,
    cardsShown = cardsShown,
    cardTypes = cardTypes,
    disciplinaryDescription = disciplinaryDescription,
    reporterSignature = reporterSignature,
    selectedTargetTeamId = selectedTargetTeamId,
    refereeVerificationsJson = serializeVerifications(refereeVerifications),
    reportSubmittedAt = reportSubmittedAt?.time,
    reportSubmittedBy = reportSubmittedBy,
    organizationId = organizationId,
    mentorsAllowed = mentorsAllowed,
    isFriendly = isFriendly
)

// Simple JSON serialization helpers (In a real app, use Gson/Moshi/Kotlinx.Serialization)
private fun serializeVerifications(list: List<RefereeVerification>): String {
    return list.joinToString(";") { "${it.refereeId},${it.isPresent},${it.correctRole},${it.actualRefereeId ?: ""},${it.actualRole?.name ?: ""}" }
}

private fun deserializeVerifications(json: String): List<RefereeVerification> {
    if (json.isBlank()) return emptyList()
    return json.split(";").map { 
        val parts = it.split(",")
        RefereeVerification(
            refereeId = parts.getOrNull(0) ?: "",
            isPresent = parts.getOrNull(1)?.toBoolean() ?: false,
            correctRole = parts.getOrNull(2)?.toBoolean() ?: false,
            actualRefereeId = parts.getOrNull(3)?.takeIf { it.isNotEmpty() },
            actualRole = parts.getOrNull(4)?.takeIf { it.isNotEmpty() }?.let { role ->
                try { com.google.refereeschedule.domain.model.AssignmentPosition.valueOf(role) } catch(e: Exception) { null }
            }
        )
    }
}
