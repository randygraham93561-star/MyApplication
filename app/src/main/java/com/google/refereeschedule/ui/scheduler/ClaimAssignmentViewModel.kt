package com.google.refereeschedule.ui.scheduler

import androidx.lifecycle.ViewModel
import com.google.refereeschedule.domain.model.Assignment
import com.google.refereeschedule.domain.model.AssignmentPosition
import com.google.refereeschedule.domain.model.DivisionDifficulty
import com.google.refereeschedule.domain.model.Game
import com.google.refereeschedule.domain.model.RefereeProfile
import com.google.refereeschedule.domain.repository.AssignmentRepository
import com.google.refereeschedule.domain.repository.GameRepository
import com.google.refereeschedule.domain.repository.SeasonRepository
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class ClaimAssignmentViewModel @Inject constructor(
    private val assignmentRepository: AssignmentRepository,
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository,
    private val seasonRepository: SeasonRepository
) : ViewModel() {

    suspend fun validateClaim(
        referee: RefereeProfile,
        game: Game,
        position: AssignmentPosition
    ): ClaimValidationResult {
        val user = userRepository.getUser(referee.id)
        val isAdmin = user?.role == com.google.refereeschedule.domain.model.UserRole.Admin || 
                      user?.role == com.google.refereeschedule.domain.model.UserRole.SystemAdmin

        // 1. Check for Overlap
        val existingAssignments = assignmentRepository.getAssignmentsForReferee(referee.id).first()
        val targetDateStr = formatDate(game.date)
        
        val seasons = seasonRepository.getAllSeasons()
        
        val targetGameStart = parseGameDateTime(game)
        val targetGameDuration = getGameDuration(game, seasons)
        val targetGameEnd = targetGameStart?.let { it + (targetGameDuration * 60 * 1000) }

        for (assignment in existingAssignments) {
            val existingGame = gameRepository.getGame(assignment.gameId)
            if (existingGame != null && existingGame.id != game.id) {
                if (formatDate(existingGame.date) == targetDateStr) {
                    val existingStart = parseGameDateTime(existingGame)
                    val existingDuration = getGameDuration(existingGame, seasons)
                    val existingEnd = existingStart?.let { it + (existingDuration * 60 * 1000) }

                    if (targetGameStart != null && targetGameEnd != null && existingStart != null && existingEnd != null) {
                        // Overlap if one starts before the other ends
                        val overlaps = targetGameStart < existingEnd && existingStart < targetGameEnd
                        if (overlaps) {
                            if (isAdmin) {
                                // Admins are excluded from the hard error, they only get a warning
                                return ClaimValidationResult.Warning("Schedule Conflict: This game overlaps with your assignment at ${existingGame.time}. As an Admin you can proceed.")
                            } else {
                                return ClaimValidationResult.Error("Schedule Conflict: This game overlaps with your assignment at ${existingGame.time}.")
                            }
                        }
                    }
                }
            }
        }
        
        // 2. Check Comfort Level
        val refereeLevel = if (position == AssignmentPosition.HeadReferee) {
            referee.headRefereeComfortLevel
        } else {
            referee.assistantRefereeComfortLevel
        }

        val gameDifficulty = if (game.difficultyLevel == 1 && !game.divisionName.contains("8U Boys", ignoreCase = true)) {
            // Old default was 1, recalculate for safety if it doesn't look like 8U Boys
            com.google.refereeschedule.domain.model.DivisionDifficulty.getLevelForDivision(game.divisionName)
        } else {
            game.difficultyLevel
        }

        val diff = gameDifficulty - refereeLevel

        return when {
            diff <= 0 -> ClaimValidationResult.Ok
            isAdmin -> ClaimValidationResult.Ok // Admins bypass comfort level restrictions
            diff == 1 -> ClaimValidationResult.Warning("This game is one level above your comfort level (${DivisionDifficulty.headRefereeLevels[refereeLevel]}). If you confirm, it will be automatically approved.")
            else -> ClaimValidationResult.RequiresAdmin("This game is more than one level above your comfort level. If you proceed, it will need to be approved by the Referee Admin.")
        }
    }

    private fun formatDate(date: java.util.Date): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }

    private fun parseGameDateTime(game: Game): Long? {
        return try {
            val dateStr = formatDate(game.date)
            val fullStr = "$dateStr ${game.time}"
            val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(fullStr)?.time
        } catch (e: Exception) {
            null
        }
    }

    private fun getGameDuration(game: Game, seasons: List<com.google.refereeschedule.domain.model.Season>): Int {
        val season = seasons.find { it.id == game.seasonId }
        val division = season?.divisions?.find { it.name == game.divisionName }
        
        val halvesMinutes = division?.halfDurationMinutes ?: 45
        val halftimeBreak = 5
        
        return (halvesMinutes * 2) + halftimeBreak
    }

    suspend fun claimAssignment(assignment: Assignment, validationResult: ClaimValidationResult) {
        val status = if (validationResult is ClaimValidationResult.RequiresAdmin) {
            com.google.refereeschedule.domain.model.AssignmentStatus.Pending
        } else {
            com.google.refereeschedule.domain.model.AssignmentStatus.Confirmed
        }
        
        val assignmentWithStatus = assignment.copy(status = status)
        assignmentRepository.saveAssignment(assignmentWithStatus)
        
        // Update game status
        val game = gameRepository.getGame(assignment.gameId)
        if (game != null) {
            val crew = assignmentRepository.getAssignmentsForGame(game.id).first()
            val newStatus = when {
                crew.size >= game.requiredCrewSize -> com.google.refereeschedule.domain.model.GameStatus.Full
                crew.isNotEmpty() -> com.google.refereeschedule.domain.model.GameStatus.PartiallyFilled
                else -> com.google.refereeschedule.domain.model.GameStatus.Open
            }
            if (game.status != newStatus) {
                gameRepository.saveGame(game.copy(status = newStatus))
            }
        }
    }
}

sealed interface ClaimValidationResult {
    data object Ok : ClaimValidationResult
    data class Warning(val message: String) : ClaimValidationResult
    data class Error(val message: String) : ClaimValidationResult
    data class RequiresAdmin(val message: String) : ClaimValidationResult
}
