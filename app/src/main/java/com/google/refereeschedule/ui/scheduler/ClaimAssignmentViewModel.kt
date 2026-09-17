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
import com.google.refereeschedule.util.TimeUtils
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
    private val seasonRepository: SeasonRepository,
    private val organizationRepository: com.google.refereeschedule.domain.repository.OrganizationRepository
) : ViewModel() {

    suspend fun validateClaim(
        referee: RefereeProfile,
        game: Game,
        position: AssignmentPosition
    ): ClaimValidationResult {
        val user = userRepository.getUser(referee.id)
        val role = user?.userRole ?: com.google.refereeschedule.domain.model.UserRole.Referee
        val isAdmin = role == com.google.refereeschedule.domain.model.UserRole.Admin || 
                      role == com.google.refereeschedule.domain.model.UserRole.SystemAdmin

        // Fetch Organization Tier
        val organization = organizationRepository.getOrganizationsFlow().first().find { it.id == game.organizationId }
        val now = java.util.Date()
        val isExpired = organization?.isCurrentlyExpired ?: true
        val activeTier = if (isExpired) com.google.refereeschedule.domain.model.SubscriptionTier.Free 
                        else (organization?.subscriptionTier ?: com.google.refereeschedule.domain.model.SubscriptionTier.Free)

        val isStrictLeague = activeTier != com.google.refereeschedule.domain.model.SubscriptionTier.Free

        val seasons = seasonRepository.getAllSeasons()
        val currentSeason = seasons.find { it.id == game.seasonId }
        
        if (currentSeason?.live != true && !isAdmin) {
            return ClaimValidationResult.Error("This season is not live or is read-only.")
        }

        // 0. Check for Game Day restriction - ONLY for Base tier or higher
        val todayStr = formatDate(Date())
        val targetDateStr = formatDate(game.date ?: Date())
        if (isStrictLeague && targetDateStr == todayStr && !isAdmin) {
            return ClaimValidationResult.Error("Games cannot be claimed on game day. Please contact your Referee Admin for last-minute assignments.")
        }

        val targetGameStart = parseGameDateTime(game)
        val targetGameDuration = getGameDuration(game, seasons)
        val targetGameEnd = targetGameStart?.let { it + (targetGameDuration * 60 * 1000) }

        // 1. Check for Overlap - BLOCK if Strict, WARN if Free
        val existingAssignments = assignmentRepository.getAssignmentsForReferee(referee.id).first()
        
        for (assignment in existingAssignments) {
            val existingGame = gameRepository.getGame(assignment.gameId)
            if (existingGame != null && existingGame.id != game.id) {
                if (formatDate(existingGame.date ?: Date()) == targetDateStr) {
                    val existingStart = parseGameDateTime(existingGame)
                    val existingDuration = getGameDuration(existingGame, seasons)
                    val existingEnd = existingStart?.let { it + (existingDuration * 60 * 1000) }

                    if (targetGameStart != null && targetGameEnd != null && existingStart != null && existingEnd != null) {
                        val overlaps = targetGameStart < existingEnd && existingStart < targetGameEnd
                        if (overlaps) {
                            if (isAdmin) {
                                return ClaimValidationResult.Warning("Schedule Conflict: This game overlaps with your assignment at ${TimeUtils.formatTo12h(existingGame.time)}.")
                            } else if (isStrictLeague) {
                                return ClaimValidationResult.Error("Schedule Conflict: This game overlaps with your assignment at ${TimeUtils.formatTo12h(existingGame.time)}.")
                            } else {
                                return ClaimValidationResult.Warning("Note: This game overlaps with your assignment at ${TimeUtils.formatTo12h(existingGame.time)}. Since this is a Free league, you can still proceed.")
                            }
                        }
                    }
                }
            }
        }
        
        // 2. Check Comfort Level / Badge Level
        val gameDifficulty = if (game.difficultyLevel == 1 && !game.divisionName.contains("8U", ignoreCase = true)) {
            DivisionDifficulty.getLevelForDivision(game.divisionName, game.gender)
        } else {
            game.difficultyLevel
        }

        // Mentor Logic
        if (position == AssignmentPosition.Mentor) {
            if (!referee.isMentor && !isAdmin) {
                return ClaimValidationResult.Error("Only designated Mentors or Admins can claim mentoring slots.")
            }
            // Mentors must be comfortable with the game level
            if (gameDifficulty > referee.headRefereeComfortLevel && !isAdmin) {
                return ClaimValidationResult.Error("This game level exceeds your mentoring comfort level.")
            }
            return ClaimValidationResult.Ok
        }

        val refereeLevel = if (position == AssignmentPosition.HeadReferee) {
            referee.headRefereeComfortLevel
        } else {
            referee.assistantRefereeComfortLevel
        }

        // 3. Check Point-Earned Team Conflict
        if (isStrictLeague && position == AssignmentPosition.HeadReferee) {
            val refereePointTeamIds = referee.teamIdsForPoints
            if (refereePointTeamIds.contains(game.homeTeamId) || refereePointTeamIds.contains(game.awayTeamId)) {
                return ClaimValidationResult.Error("Conflict of Interest: You cannot serve as Head Referee for a team you are earning points for.")
            }
        }

        // 4. Advanced Restriction Overhaul
        if (!isAdmin) {
            // A. Badge Level Ceilings (For Head Referee only)
            if (position == AssignmentPosition.HeadReferee) {
                val badgeCeiling = when (referee.badgeLevel.lowercase()) {
                    "regional" -> 1 // 10U
                    "intermediate" -> 2 // 12U
                    "advanced" -> 3 // 14U
                    "national" -> 5 // 19U
                    else -> 0
                }
                val gameStep = DivisionDifficulty.getDivisionStep(game.ageGroup)
                if (gameStep > badgeCeiling) {
                    return ClaimValidationResult.Error("Badge Restriction: Your current badge (${referee.badgeLevel}) only allows you to Head Referee up to ${when(badgeCeiling) { 1 -> "10U"; 2 -> "12U"; 3 -> "14U"; else -> "8U" }} games.")
                }
            }

            if (referee.isMinor) {
                // YOUTH RULES: Enforce Division Gap AND Comfort Levels for ALL roles
                val refereeStep = DivisionDifficulty.getDivisionStepForAge(referee.age)
                val gameStep = DivisionDifficulty.getDivisionStep(game.ageGroup)
                
                val gapRequired = if (position == AssignmentPosition.HeadReferee) 
                    organization?.youthRefereeRequirements?.minDivisionGapHead ?: 2
                else 
                    organization?.youthRefereeRequirements?.minDivisionGapAR ?: 1
                
                if (refereeStep - gameStep < gapRequired) {
                    return ClaimValidationResult.Error("Youth Restriction: You must be at least $gapRequired divisions above the players to officiate this role.")
                }
                
                // Youth must also meet comfort level strictly
                if (gameDifficulty > refereeLevel) {
                    return ClaimValidationResult.Error("This game level exceeds your current comfort level.")
                }
            } else {
                // ADULT RULES: No division gap.
                // Restriction: Comfort Level for Head Ref role only. Can AR all games.
                if (position == AssignmentPosition.HeadReferee) {
                    if (gameDifficulty > refereeLevel) {
                        val diff = gameDifficulty - refereeLevel
                        if (diff == 1) {
                            return ClaimValidationResult.Warning("This game is one level above your comfort level.")
                        } else {
                            return ClaimValidationResult.RequiresAdmin("This game is more than one level above your comfort level.")
                        }
                    }
                }
                // Adults can AR anything
            }
        }

        return ClaimValidationResult.Ok
    }

    private fun formatDate(date: java.util.Date): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }

    private fun parseGameDateTime(game: Game): Long? {
        val gameDate = game.date ?: return null
        val dateStr = formatDate(gameDate)
        val fullStr = "$dateStr ${game.time}"
        
        // Try 24h format first
        val sdf24 = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val time24 = try { sdf24.parse(fullStr)?.time } catch (e: Exception) { null }
        if (time24 != null) return time24

        // Fallback to 12h format
        val sdf12 = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
        return try { sdf12.parse(fullStr)?.time } catch (e: Exception) { null }
    }

    private fun getGameDuration(game: Game, seasons: List<com.google.refereeschedule.domain.model.Season>): Int {
        val season = seasons.find { it.id == game.seasonId }
        val division = season?.divisions?.find { it.name == game.divisionName }
        
        val halvesMinutes = division?.halfDurationMinutes ?: 45
        val halftimeBreak = 5
        
        return (halvesMinutes * 2) + halftimeBreak
    }

    suspend fun claimAssignment(assignment: Assignment, validationResult: ClaimValidationResult) {
        if (assignment.position == AssignmentPosition.Mentor) {
            // Mentor is saved directly to the game document
            gameRepository.getGame(assignment.gameId)?.let { game ->
                gameRepository.saveGame(game.copy(mentorId = assignment.refereeId))
            }
            return
        }

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
