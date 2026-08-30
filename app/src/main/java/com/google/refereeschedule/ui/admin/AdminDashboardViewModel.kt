package com.google.refereeschedule.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.domain.model.Assignment
import com.google.refereeschedule.domain.model.AssignmentStatus
import com.google.refereeschedule.domain.model.Game
import com.google.refereeschedule.domain.model.GameStatus
import com.google.refereeschedule.domain.model.PointAward
import com.google.refereeschedule.domain.model.RefereeProfile
import com.google.refereeschedule.domain.model.Team
import com.google.refereeschedule.domain.model.Season
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.repository.AssignmentRepository
import com.google.refereeschedule.domain.repository.GameRepository
import com.google.refereeschedule.domain.repository.ProfileRepository
import com.google.refereeschedule.domain.repository.SeasonRepository
import com.google.refereeschedule.domain.repository.TeamRepository
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminDashboardUiState(
    val totalGames: Int = 0,
    val fullyCovered: Int = 0,
    val missingRefs: Int = 0,
    val pendingApprovals: Int = 0,
    val gamesWithAssignments: List<Pair<Game, List<Assignment>>> = emptyList(),
    val pendingAssignments: List<Assignment> = emptyList(),
    val seasons: List<Season> = emptyList(),
    val refereeProfiles: List<RefereeProfile> = emptyList(),
    val teams: List<Team> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val gameRepository: GameRepository,
    private val seasonRepository: SeasonRepository,
    private val assignmentRepository: AssignmentRepository,
    private val profileRepository: ProfileRepository,
    private val teamRepository: TeamRepository
) : ViewModel() {

    private val currentUserId = authRepository.currentUser?.uid

    val uiState = flow {
        val uid = currentUserId
        if (uid != null) {
            val user = userRepository.getUser(uid)
            val orgId = user?.organizationId
            if (orgId != null) {
                // Check if admin has a profile and if it has the orgId
                val profile = profileRepository.getProfile(uid)
                if (profile != null && profile.organizationId != orgId) {
                    profileRepository.saveProfile(profile.copy(organizationId = orgId))
                } else if (profile == null) {
                    profileRepository.saveProfile(RefereeProfile(
                        id = uid,
                        name = user.email.substringBefore("@"),
                        badgeLevel = "Regional",
                        organizationId = orgId
                    ))
                }
                emit(orgId)
            }
        }
    }.flatMapLatest { orgId ->
        combine(
            gameRepository.getGamesForOrganizationFlow(orgId),
            assignmentRepository.getAssignmentsForOrganizationFlow(orgId),
            seasonRepository.getSeasonsForOrganizationFlow(orgId),
            profileRepository.getProfilesForOrganizationFlow(orgId),
            teamRepository.getTeamsForOrganizationFlow(orgId)
        ) { games, assignments, seasons, profiles, teams ->
            val gamesWithAssignments = games.map { game ->
                game to assignments.filter { it.gameId == game.id }
            }

            val totalGames = games.size
            val fullyCovered = gamesWithAssignments.count { (game, crew) ->
                crew.size >= game.requiredCrewSize && crew.all { it.status == AssignmentStatus.Confirmed }
            }
            val missingRefs = gamesWithAssignments.sumOf { (game, crew) ->
                (game.requiredCrewSize - crew.size).coerceAtLeast(0)
            }
            val pendingApprovals = assignments.count { it.status == AssignmentStatus.Pending }

            AdminDashboardUiState(
                totalGames = totalGames,
                fullyCovered = fullyCovered,
                missingRefs = missingRefs,
                pendingApprovals = pendingApprovals,
                gamesWithAssignments = gamesWithAssignments,
                pendingAssignments = assignments.filter { it.status == AssignmentStatus.Pending },
                seasons = seasons,
                refereeProfiles = profiles,
                teams = teams
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminDashboardUiState())

    fun createSeason(season: Season) {
        viewModelScope.launch {
            val uid = currentUserId ?: return@launch
            val user = userRepository.getUser(uid)
            val orgId = user?.organizationId ?: return@launch

            seasonRepository.saveSeason(season.copy(organizationId = orgId, isActive = true))
        }
    }

    fun updateSeason(season: Season) {
        viewModelScope.launch {
            seasonRepository.saveSeason(season)
        }
    }

    fun updateRefereeProfile(profile: RefereeProfile) {
        viewModelScope.launch {
            profileRepository.saveProfile(profile)
        }
    }

    fun approveAssignment(assignment: Assignment) {
        viewModelScope.launch {
            assignmentRepository.saveAssignment(assignment.copy(status = AssignmentStatus.Confirmed))
        }
    }

    fun approveReport(game: Game) {
        viewModelScope.launch {
            try {
                // Fetch fresh game data from Firestore to ensure we have all report details
                val freshGame = gameRepository.getGame(game.id) ?: game
                
                // 1. Mark Game as ReportApproved (Pending Finalization)
                gameRepository.saveGame(freshGame.copy(status = GameStatus.ReportApproved))
            } catch (e: Exception) {
                android.util.Log.e("PointDistribution", "Error in approveReport", e)
            }
        }
    }

    fun finalizeGameDay(date: java.util.Date) {
        viewModelScope.launch {
            try {
                val uid = currentUserId ?: return@launch
                val user = userRepository.getUser(uid)
                val orgId = user?.organizationId ?: return@launch

                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(date)

                val games = gameRepository.getGamesForOrganizationFlow(orgId).first()
                val dayGames = games.filter { 
                    val dStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }.format(it.date)
                    dStr == dateStr
                }

                // Check if ALL games for that day are either Completed or ReportApproved
                // (Matches with no reports pending or in draft)
                val allReviewed = dayGames.all { 
                    it.status == GameStatus.ReportApproved || it.status == GameStatus.Completed 
                }

                if (!allReviewed) {
                    android.util.Log.w("PointDistribution", "Cannot finalize: Not all games for the day are reviewed.")
                    return@launch
                }

                // Process points for all ReportApproved games
                dayGames.filter { it.status == GameStatus.ReportApproved }.forEach { game ->
                    processGamePoints(game)
                }
            } catch (e: Exception) {
                android.util.Log.e("PointDistribution", "Error finalizing game day", e)
            }
        }
    }

    private suspend fun processGamePoints(game: Game) {
        // 1. Award Points based on verification
        val verifications = game.refereeVerifications
        if (verifications.isEmpty()) {
            val assignments = assignmentRepository.getAssignmentsForGame(game.id).first()
            assignments.forEach { assignment ->
                awardPointsToRefereeAndTeams(assignment.refereeId, assignment.position, game)
            }
        } else {
            verifications.forEach { verification ->
                val actualRefereeId = verification.actualRefereeId ?: return@forEach
                val actualRole = verification.actualRole ?: return@forEach
                awardPointsToRefereeAndTeams(actualRefereeId, actualRole, game)
            }
        }

        // 2. Mark Game as Fully Completed
        gameRepository.saveGame(game.copy(status = GameStatus.Completed))
    }

    private suspend fun awardPointsToRefereeAndTeams(refereeId: String, role: com.google.refereeschedule.domain.model.AssignmentPosition, game: Game) {
        val points = if (role == com.google.refereeschedule.domain.model.AssignmentPosition.HeadReferee) 2 else 1
        
        // Referee Personal Points & Game Counters
        val profile = profileRepository.getProfile(refereeId)
        if (profile != null) {
            val updatedProfile = if (role == com.google.refereeschedule.domain.model.AssignmentPosition.HeadReferee) {
                profile.copy(
                    totalPoints = profile.totalPoints + points,
                    headRefereeGamesCount = profile.headRefereeGamesCount + 1
                )
            } else {
                profile.copy(
                    totalPoints = profile.totalPoints + points,
                    assistantRefereeGamesCount = profile.assistantRefereeGamesCount + 1
                )
            }
            
            // Repair profile orgId if missing
            val finalProfile = if (updatedProfile.organizationId.isEmpty() && game.organizationId.isNotEmpty()) {
                updatedProfile.copy(organizationId = game.organizationId)
            } else updatedProfile

            profileRepository.saveProfile(finalProfile)
            
            // Team Points based on Distribution Mode
            distributePoints(finalProfile, game, points)
        }
    }

    private suspend fun distributePoints(profile: RefereeProfile, game: Game, points: Int) {
        val teams = profile.teamIdsForPoints
        val distributionMode = profile.distributionMode
        // Use game orgId, fallback to profile orgId
        val orgId = game.organizationId.ifEmpty { profile.organizationId }
        
        if (orgId.isEmpty()) {
            android.util.Log.e("PointDistribution", "ABORT: No Organization ID found for distribution")
            return
        }

        android.util.Log.d("PointDistribution", "Distributing $points pts for ${profile.name} (Mode: $distributionMode, Org: $orgId)")

        val baseAward = PointAward(
            gameId = game.id,
            refereeId = profile.id,
            organizationId = orgId,
            seasonId = game.seasonId,
            points = points,
            timestamp = game.date
        )

        val orgTeams = teamRepository.getTeamsForOrganizationFlow(orgId).first()
        
        when (distributionMode) {
            com.google.refereeschedule.domain.model.PointDistributionMode.Even -> {
                if (teams.isNotEmpty()) {
                    teams.forEach { teamId ->
                        android.util.Log.d("PointDistribution", "Awarding EVEN to team $teamId")
                        teamRepository.awardPoints(baseAward.copy(teamId = teamId))
                    }
                } else {
                    android.util.Log.d("PointDistribution", "No teams selected, awarding to UNASSIGNED")
                    teamRepository.awardPoints(baseAward.copy(teamId = "unassigned"))
                }
            }
            com.google.refereeschedule.domain.model.PointDistributionMode.Manual -> {
                val targetId = if (profile.id == game.reportSubmittedBy) game.selectedTargetTeamId else ""
                val teamId = if (targetId.isNotEmpty()) targetId else teams.firstOrNull()
                
                if (teamId != null) {
                    android.util.Log.d("PointDistribution", "Awarding MANUAL to team $teamId")
                    teamRepository.awardPoints(baseAward.copy(teamId = teamId))
                } else {
                    android.util.Log.d("PointDistribution", "No target team found, awarding to UNASSIGNED")
                    teamRepository.awardPoints(baseAward.copy(teamId = "unassigned"))
                }
            }
            com.google.refereeschedule.domain.model.PointDistributionMode.NeedsBased -> {
                val season = seasonRepository.getAllSeasons().find { it.id == game.seasonId }
                val maxWeekend = season?.maxPointsPerWeekend ?: 10

                // 1. Prioritize Referee's Own Teams
                val myTeams = orgTeams.filter { it.id in teams }
                val myTeamNeeds = mutableListOf<Pair<com.google.refereeschedule.domain.model.Team, Int>>()
                
                for (team in myTeams) {
                    val wPoints = teamRepository.getPointsForTeamOnWeekend(team.id, game.seasonId, game.date)
                    if (wPoints < maxWeekend) {
                        myTeamNeeds.add(team to wPoints)
                    }
                }
                
                val myTarget = myTeamNeeds.minByOrNull { it.second }?.first

                if (myTarget != null) {
                    android.util.Log.d("PointDistribution", "Awarding NEEDS-BASED to personal team ${myTarget.name}")
                    teamRepository.awardPoints(baseAward.copy(teamId = myTarget.id))
                } else {
                    // 2. If own teams are full, find any org team with lowest points below quota
                    val allTeamNeeds = mutableListOf<Pair<com.google.refereeschedule.domain.model.Team, Int>>()
                    for (team in orgTeams) {
                        val wPoints = teamRepository.getPointsForTeamOnWeekend(team.id, game.seasonId, game.date)
                        if (wPoints < maxWeekend) {
                            allTeamNeeds.add(team to wPoints)
                        }
                    }
                    
                    val anyTarget = allTeamNeeds.minByOrNull { it.second }?.first
                    if (anyTarget != null) {
                        android.util.Log.d("PointDistribution", "Awarding NEEDS-BASED to organization team ${anyTarget.name}")
                        teamRepository.awardPoints(baseAward.copy(teamId = anyTarget.id))
                    } else {
                        android.util.Log.d("PointDistribution", "All teams full, awarding to UNASSIGNED")
                        teamRepository.awardPoints(baseAward.copy(teamId = "unassigned"))
                    }
                }
            }
        }
    }

    fun denyAssignment(assignment: Assignment) {
        viewModelScope.launch {
            assignmentRepository.deleteAssignment(assignment.id)
        }
    }
}
