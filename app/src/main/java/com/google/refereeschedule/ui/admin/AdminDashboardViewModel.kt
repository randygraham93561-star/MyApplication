package com.google.refereeschedule.ui.admin

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.domain.model.*
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class AdminDashboardUiState(
    val managedOrganizations: List<Organization> = emptyList(),
    val selectedOrgId: String = "",
    val totalGames: Int = 0,
    val fullyCovered: Int = 0,
    val missingRefs: Int = 0,
    val pendingApprovals: Int = 0,
    val gamesWithAssignments: List<Pair<Game, List<Assignment>>> = emptyList(),
    val pendingAssignments: List<Assignment> = emptyList(),
    val seasons: List<Season> = emptyList(),
    val refereeProfiles: List<RefereeProfile> = emptyList(),
    val teams: List<Team> = emptyList(),
    val globalQuizQuestions: List<QuizQuestion> = emptyList(),
    val activeSeasonQuiz: RefresherQuiz? = null,
    val approvedReportsSearchResults: List<Pair<Game, List<Assignment>>> = emptyList(),
    val userRole: UserRole = UserRole.Referee,
    val activeTier: SubscriptionTier = SubscriptionTier.Free,
    val isSubscriptionExpired: Boolean = false
)

data class ApprovedReportSearchQuery(
    val date: Date,
    val division: String? = null,
    val gender: String? = null,
    val teamId: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val gameRepository: GameRepository,
    private val seasonRepository: SeasonRepository,
    private val assignmentRepository: AssignmentRepository,
    private val profileRepository: ProfileRepository,
    private val teamRepository: TeamRepository,
    private val quizRepository: QuizRepository,
    private val organizationRepository: OrganizationRepository,
    private val announcementRepository: AnnouncementRepository,
    private val storageRepository: StorageRepository,
    private val printTemplateRepository: PrintTemplateRepository
) : ViewModel() {

    private val _distributeStatus = MutableStateFlow<String?>(null)
    val distributeStatus: StateFlow<String?> = _distributeStatus.asStateFlow()

    private val _selectedOrgId = MutableStateFlow<String?>(null)

    private val _isUploadingLogo = MutableStateFlow(false)
    val isUploadingLogo = _isUploadingLogo.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError = _uploadError.asStateFlow()

    private val _isProcessingSeason = MutableStateFlow(false)
    val isProcessingSeason = _isProcessingSeason.asStateFlow()

    private val _approvedReportSearchQuery = MutableStateFlow<ApprovedReportSearchQuery?>(null)

    fun clearDistributeStatus() { _distributeStatus.value = null }

    private val currentUserId = authRepository.currentUser?.uid

    init {
        viewModelScope.launch {
            val seasons = seasonRepository.getAllSeasons()
            val twoYearsAgo = Calendar.getInstance().apply {
                add(Calendar.YEAR, -2)
            }.time

            seasons.filter { s -> (s.archivedAt != null) && s.archivedAt.before(twoYearsAgo) }
                .forEach { oldSeason ->
                    Log.d("PointDistribution", "Auto-deleting 2-year old season: ${oldSeason.name}")
                    seasonRepository.deleteSeason(oldSeason.id)
                }
        }
    }

    val uiState: StateFlow<AdminDashboardUiState> = flow {
        val uid = currentUserId
        if (uid != null) {
            val user = userRepository.getUser(uid)
            val email = authRepository.currentUser?.email?.lowercase()?.trim() ?: ""
            
            // Hardcoded override for specific users
            val role = when (email) {
                "randygraham93561@gmail.com" -> UserRole.SystemAdmin
                "randalltruck@gmail.com" -> UserRole.Admin
                else -> user?.userRole ?: UserRole.Referee
            }
            
            val allOrgs = organizationRepository.getOrganizationsFlow().first()
            
            val managedOrgs = if (role == UserRole.SystemAdmin) {
                allOrgs
            } else {
                val adminOrgs = user?.adminOrgIds ?: emptyList()
                val coachOrgs = user?.coachOrgIds ?: emptyList()
                val primaryOrg = if (role == UserRole.Admin || role == UserRole.CoachAdmin) user?.organizationId ?: "" else ""
                val allManagedIds = (adminOrgs + coachOrgs + primaryOrg).filter { it.isNotEmpty() }.distinct()
                
                // If it's randalltruck and they don't have any IDs assigned yet, show them all to let them pick? 
                // No, just show them their primary one if it exists.
                allOrgs.filter { it.id in allManagedIds }
            }
            
            if (_selectedOrgId.value == null && managedOrgs.isNotEmpty()) {
                _selectedOrgId.value = managedOrgs.first().id
            }
            
            emit(managedOrgs to _selectedOrgId)
        }
    }.flatMapLatest { (managedOrgs, selectedOrgFlow) ->
        selectedOrgFlow.flatMapLatest { orgId ->
            if (orgId == null) return@flatMapLatest flowOf(AdminDashboardUiState())
            
            val seasonsFlow = seasonRepository.getSeasonsForOrganizationFlow(orgId)
            val activeSeasonFlow = seasonsFlow.map { it.find { s -> s.active } }
            
            val quizFlow = activeSeasonFlow.flatMapLatest { activeSeason ->
                if (activeSeason != null) quizRepository.getQuizForSeason(activeSeason.id)
                else flowOf(null)
            }

            combine(
                gameRepository.getGamesForOrganizationFlow(orgId),
                assignmentRepository.getAssignmentsForOrganizationFlow(orgId),
                seasonsFlow,
                profileRepository.getProfilesForOrganizationFlow(orgId),
                teamRepository.getTeamsForOrganizationFlow(orgId),
                quizRepository.getAllGlobalQuestions(),
                userRepository.getUserFlow(currentUserId ?: ""),
                organizationRepository.getOrganizationsFlow(),
                _approvedReportSearchQuery,
                quizFlow
            ) { results ->
                @Suppress("UNCHECKED_CAST")
                val games = results[0] as List<Game>
                @Suppress("UNCHECKED_CAST")
                val assignments = results[1] as List<Assignment>
                @Suppress("UNCHECKED_CAST")
                val seasons = results[2] as List<Season>
                @Suppress("UNCHECKED_CAST")
                val profiles = results[3] as List<RefereeProfile>
                @Suppress("UNCHECKED_CAST")
                val allTeams = results[4] as List<Team>
                @Suppress("UNCHECKED_CAST")
                val globalQuestions = results[5] as List<QuizQuestion>
                val user = results[6] as? User
                @Suppress("UNCHECKED_CAST")
                val allOrgs = results[7] as List<Organization>
                val searchQuery = results[8] as? ApprovedReportSearchQuery
                val activeQuiz = results[9] as? RefresherQuiz
                
                val currentOrg = allOrgs.find { it.id == orgId }
                val role = user?.userRole ?: UserRole.Referee
                val liveSeason = seasons.find { it.live }
                
                // For team/game filtering, if none is Live, we might show nothing or fallback to Active.
                // The user requested "current live season".
                val dashboardSeasonId = liveSeason?.id ?: ""

                val filteredGames = games.filter { it.seasonId == dashboardSeasonId }
                val filteredAssignments = assignments.filter { 
                    filteredGames.any { g -> g.id == it.gameId } 
                }

                val gamesWithAssignments = filteredGames.map { game ->
                    game to filteredAssignments.filter { it.gameId == game.id }
                }

                val searchResults = searchQuery?.let { query ->
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val queryDateStr = sdf.format(query.date)

                    gamesWithAssignments.filter { (game, _) ->
                        val gameDate = game.date ?: return@filter false
                        val isApprovedOrCompleted = game.status == GameStatus.ReportApproved || game.status == GameStatus.Completed
                        val matchesDate = sdf.format(gameDate) == queryDateStr
                        val matchesDiv = query.division == null || game.ageGroup == query.division
                        val matchesGender = query.gender == null || game.gender == query.gender
                        val matchesTeam = query.teamId == null || game.homeTeamId == query.teamId || game.awayTeamId == query.teamId || game.homeTeamName.contains(query.teamId, ignoreCase = true) || game.awayTeamName.contains(query.teamId, ignoreCase = true)

                        isApprovedOrCompleted && matchesDate && matchesDiv && matchesGender && matchesTeam
                    }
                } ?: emptyList()
                
                val isExpired = currentOrg?.isCurrentlyExpired ?: true
                val tier = if (role == UserRole.SystemAdmin) SubscriptionTier.Pro 
                          else if (isExpired) SubscriptionTier.Free 
                          else (currentOrg?.subscriptionTier ?: SubscriptionTier.Free)

                val totalGames = filteredGames.size
                val fullyCovered = gamesWithAssignments.count { (game, crew) ->
                    crew.size >= game.requiredCrewSize && crew.all { it.status == AssignmentStatus.Confirmed }
                }
                val missingRefs = gamesWithAssignments.sumOf { (game, crew) ->
                    (game.requiredCrewSize - crew.size).coerceAtLeast(0)
                }
                val pendingApprovals = filteredAssignments.count { it.status == AssignmentStatus.Pending }

                // Filter roster to only show referees who have onboarded for the current live season
                val currentSeasonRoster = profiles.filter { it.lastOnboardedSeasonId == dashboardSeasonId }

                AdminDashboardUiState(
                    managedOrganizations = managedOrgs,
                    selectedOrgId = orgId,
                    totalGames = totalGames,
                    fullyCovered = fullyCovered,
                    missingRefs = missingRefs,
                    pendingApprovals = pendingApprovals,
                    gamesWithAssignments = gamesWithAssignments,
                    pendingAssignments = filteredAssignments.filter { it.status == AssignmentStatus.Pending },
                    seasons = seasons,
                    refereeProfiles = currentSeasonRoster,
                    teams = allTeams.filter { it.seasonId == dashboardSeasonId },
                    globalQuizQuestions = globalQuestions,
                    activeSeasonQuiz = activeQuiz,
                    approvedReportsSearchResults = searchResults,
                    userRole = role,
                    activeTier = tier,
                    isSubscriptionExpired = isExpired,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminDashboardUiState())

    fun selectOrganization(orgId: String) {
        _selectedOrgId.value = orgId
    }

    fun searchApprovedReports(date: Date, division: String?, gender: String?, teamId: String?) {
        _approvedReportSearchQuery.value = ApprovedReportSearchQuery(date, division, gender, teamId)
    }

    fun createQuizQuestion(text: String, options: List<String>, correctIndex: Int) {
        viewModelScope.launch {
            val orgId = uiState.value.selectedOrgId
            val question = QuizQuestion(
                text = text,
                options = options,
                correctAnswerIndex = correctIndex,
                createdByOrgId = orgId
            )
            quizRepository.saveQuestion(question)
        }
    }

    fun configureSeasonQuiz(seasonId: String, questionIds: List<String>, quizLength: Int, passingPercentage: Int) {
        viewModelScope.launch {
            val orgId = _selectedOrgId.value ?: return@launch
            val existingQuiz = uiState.value.activeSeasonQuiz
            
            val quiz = RefresherQuiz(
                id = existingQuiz?.id ?: "",
                organizationId = orgId,
                seasonId = seasonId,
                questionIds = questionIds,
                quizLength = quizLength,
                passingPercentage = passingPercentage
            )
            quizRepository.saveQuiz(quiz)
        }
    }

    fun createSeason(season: Season) {
        viewModelScope.launch {
            val orgId = _selectedOrgId.value ?: return@launch
            val lastSeason = uiState.value.seasons.maxByOrNull { it.startDate ?: Date(0) }
            
            val newSeason = season.copy(
                organizationId = orgId,
                active = true,
                live = false,
                startDate = season.startDate ?: Date(),
                endDate = season.endDate ?: Calendar.getInstance().apply { add(Calendar.MONTH, 3) }.time,
                divisions = if (season.divisions.isEmpty()) lastSeason?.divisions ?: emptyList() else season.divisions,
                collectPoints = lastSeason?.collectPoints ?: season.collectPoints,
                maxPointsPerWeekend = lastSeason?.maxPointsPerWeekend ?: season.maxPointsPerWeekend,
                centerRefereePoints = lastSeason?.centerRefereePoints ?: season.centerRefereePoints,
                assistantRefereePoints = lastSeason?.assistantRefereePoints ?: season.assistantRefereePoints,
                lunchVoucherAssignedGamesRequired = lastSeason?.lunchVoucherAssignedGamesRequired ?: season.lunchVoucherAssignedGamesRequired,
                lunchVoucherCompletedGamesRequired = lastSeason?.lunchVoucherCompletedGamesRequired ?: season.lunchVoucherCompletedGamesRequired
            )
            seasonRepository.saveSeason(newSeason)
        }
    }

    fun markSeasonLive(season: Season) {
        viewModelScope.launch {
            _isProcessingSeason.value = true
            try {
                val orgId = season.organizationId
                if (orgId.isEmpty()) {
                    Log.e("SeasonManagement", "Aborting markSeasonLive: Organization ID is empty")
                    _isProcessingSeason.value = false
                    return@launch
                }

                // 1. Mark the target season as live and active
                val updatedSeason = season.copy(live = true, active = true, archivedAt = null)
                seasonRepository.saveSeason(updatedSeason)
                Log.d("SeasonManagement", "Marked season ${season.name} as LIVE")

                // 2. Fetch ALL seasons for this org and archive everything else
                val allSeasons = seasonRepository.getAllSeasons()
                allSeasons.filter { 
                    it.id != season.id && 
                    it.organizationId == orgId && 
                    (it.live || it.active) 
                }.forEach { oldSeason ->
                    Log.d("SeasonManagement", "Archiving old season: ${oldSeason.name}")
                    seasonRepository.saveSeason(oldSeason.copy(
                        live = false,
                        active = false,
                        archivedAt = Date()
                    ))
                }
            } catch (e: Exception) {
                Log.e("SeasonManagement", "Error marking season live", e)
            } finally {
                _isProcessingSeason.value = false
            }
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
                val freshGame = gameRepository.getGame(game.id) ?: game
                gameRepository.saveGame(freshGame.copy(
                    status = GameStatus.ReportApproved,
                    adminFeedback = null
                ))
            } catch (e: Exception) {
                Log.e("PointDistribution", "Error in approveReport", e)
            }
        }
    }

    fun pushBackReport(game: Game, feedback: String) {
        viewModelScope.launch {
            try {
                gameRepository.saveGame(game.copy(
                    status = GameStatus.NeedsRevision,
                    adminFeedback = feedback
                ))
            } catch (e: Exception) {
                Log.e("PointDistribution", "Error in pushBackReport", e)
            }
        }
    }

    fun finalizeGameDay(date: Date) {
        viewModelScope.launch {
            try {
                val orgId = _selectedOrgId.value ?: return@launch
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(date)

                val games = gameRepository.getGamesForOrganizationFlow(orgId).first()
                val dayGames = games.filter { g ->
                    val gameDate = g.date ?: return@filter false
                    val dStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(gameDate)
                    dStr == dateStr
                }

                if (dayGames.isEmpty()) return@launch

                val gamesToFinalize = dayGames.filter { it.status == GameStatus.ReportApproved }
                if (gamesToFinalize.isEmpty()) return@launch

                _distributeStatus.value = "Processing ${gamesToFinalize.size} games..."
                gamesToFinalize.forEach { game -> processGamePoints(game) }
                _distributeStatus.value = "Successfully awarded points for ${gamesToFinalize.size} games!"
            } catch (e: Exception) {
                _distributeStatus.value = "Error: ${e.message}"
            }
        }
    }

    private suspend fun processGamePoints(game: Game) {
        val verifications = game.refereeVerifications
        if (verifications.isEmpty()) {
            val assignments = assignmentRepository.getAssignmentsForGame(game.id).first()
            assignments.forEach { assignment ->
                awardPointsToRefereeAndTeams(assignment.refereeId, assignment.position, game)
            }
        } else {
            verifications.forEach { verification ->
                val actualRefereeId = verification.actualRefereeId ?: return@forEach
                val actualRole = if (game.isDualCenter && verification.isPresent) {
                    AssignmentPosition.HeadReferee
                } else {
                    verification.actualRole ?: return@forEach
                }
                awardPointsToRefereeAndTeams(actualRefereeId, actualRole, game)
            }
        }
        gameRepository.saveGame(game.copy(status = GameStatus.Completed))
    }

    private suspend fun awardPointsToRefereeAndTeams(refereeId: String, role: AssignmentPosition, game: Game) {
        val points = if (role == AssignmentPosition.HeadReferee) 2 else 1
        val profile = profileRepository.getProfile(refereeId)
        if (profile != null) {
            val finalProfile = if (profile.organizationId.isEmpty() && game.organizationId.isNotEmpty()) {
                val p = profile.copy(organizationId = game.organizationId)
                profileRepository.saveProfile(p)
                p
            } else profile
            distributePoints(finalProfile, game, points, role)
        }
    }

    private suspend fun distributePoints(
        profile: RefereeProfile, 
        game: Game, 
        points: Int, 
        role: AssignmentPosition
    ) {
        val teams = profile.teamIdsForPoints
        val distributionMode = profile.distributionMode
        val orgId = game.organizationId.ifEmpty { profile.organizationId }
        if (orgId.isEmpty()) return

        val season = seasonRepository.getAllSeasons().find { it.id == game.seasonId }
        val maxWeeklyLimit = season?.maxPointsPerWeekend ?: 2

        val baseAward = PointAward(
            gameId = game.id,
            refereeId = profile.id,
            organizationId = orgId,
            seasonId = game.seasonId,
            points = points,
            timestamp = game.date ?: Date()
        )

        val orgTeams = teamRepository.getTeamsForOrganizationFlow(orgId).first()
        var remainingPointsToDistribute = points
        
        when (distributionMode) {
            PointDistributionMode.Even -> {
                if (teams.isNotEmpty()) {
                    val pointsPerTeam = points / teams.size
                    val remainder = points % teams.size
                    teams.forEachIndexed { index, teamId ->
                        val extra = if (index < remainder) 1 else 0
                        val toAward = pointsPerTeam + extra
                        if (toAward > 0) awardCappedPoints(baseAward.copy(teamId = teamId, points = toAward), maxWeeklyLimit)
                    }
                } else awardCappedPoints(baseAward.copy(teamId = "unassigned"), 999)
            }
            PointDistributionMode.Manual -> {
                val targetId = if (profile.id == game.reportSubmittedBy) game.selectedTargetTeamId else ""
                val teamId = if (targetId.isNotEmpty()) targetId else teams.firstOrNull()
                if (teamId != null) awardCappedPoints(baseAward.copy(teamId = teamId), maxWeeklyLimit)
                else awardCappedPoints(baseAward.copy(teamId = "unassigned"), 999)
            }
            PointDistributionMode.NeedsBased -> {
                val myTeams = orgTeams.filter { it.id in teams }
                for (team in myTeams) {
                    if (remainingPointsToDistribute <= 0) break
                    val currentWeekendPoints = teamRepository.getPointsForTeamOnWeekend(team.id, game.seasonId, game.date ?: Date())
                    val spaceLeft = (maxWeeklyLimit - currentWeekendPoints).coerceAtLeast(0)
                    if (spaceLeft > 0) {
                        val pointsToGive = spaceLeft.coerceAtMost(remainingPointsToDistribute)
                        awardCappedPoints(baseAward.copy(teamId = team.id, points = pointsToGive), maxWeeklyLimit)
                        remainingPointsToDistribute -= pointsToGive
                    }
                }
                if (remainingPointsToDistribute > 0) {
                    val otherTeams = orgTeams.filter { it.id !in teams }
                    for (team in otherTeams) {
                    if (remainingPointsToDistribute <= 0) break
                    val currentWeekendPoints = teamRepository.getPointsForTeamOnWeekend(team.id, game.seasonId, game.date ?: Date())
                    val spaceLeft = (maxWeeklyLimit - currentWeekendPoints).coerceAtLeast(0)
                    if (spaceLeft > 0) {
                            val pointsToGive = spaceLeft.coerceAtMost(remainingPointsToDistribute)
                            awardCappedPoints(baseAward.copy(teamId = team.id, points = pointsToGive), maxWeeklyLimit)
                            remainingPointsToDistribute -= pointsToGive
                        }
                    }
                }
                if (remainingPointsToDistribute > 0) awardCappedPoints(baseAward.copy(teamId = "unassigned", points = remainingPointsToDistribute), 999)
            }
        }

        val updatedProfile = if (role == AssignmentPosition.HeadReferee) {
            profile.copy(totalPoints = profile.totalPoints + points, headRefereeGamesCount = profile.headRefereeGamesCount + 1)
        } else {
            profile.copy(totalPoints = profile.totalPoints + points, assistantRefereeGamesCount = profile.assistantRefereeGamesCount + 1)
        }
        profileRepository.saveProfile(updatedProfile)
    }

    private suspend fun awardCappedPoints(award: PointAward, limit: Int) {
        if (award.teamId == "unassigned") {
            teamRepository.awardPoints(award)
            return
        }
        val currentWeekendPoints = teamRepository.getPointsForTeamOnWeekend(award.teamId, award.seasonId, award.timestamp)
        val spaceLeft = (limit - currentWeekendPoints).coerceAtLeast(0)
        if (spaceLeft > 0) {
            val finalPoints = award.points.coerceAtMost(spaceLeft)
            if (finalPoints > 0) teamRepository.awardPoints(award.copy(points = finalPoints))
        }
    }

    fun denyAssignment(assignment: Assignment) {
        viewModelScope.launch {
            assignmentRepository.deleteAssignment(assignment.id)
        }
    }

    fun sendBroadcast(title: String, message: String) {
        viewModelScope.launch {
            val orgId = _selectedOrgId.value ?: return@launch
            val announcement = Announcement(
                organizationId = orgId,
                senderId = currentUserId ?: "",
                title = title,
                message = message
            )
            announcementRepository.sendAnnouncement(announcement)
        }
    }

    // New Organization Setup Methods
    fun updateOrganization(org: Organization) {
        viewModelScope.launch {
            organizationRepository.saveOrganization(org)
        }
    }

    fun uploadLogo(uri: Uri) {
        val orgId = _selectedOrgId.value ?: return
        _isUploadingLogo.value = true
        _uploadError.value = null
        viewModelScope.launch {
            try {
                val downloadUrl = storageRepository.uploadOrganizationLogo(orgId, uri)
                if (downloadUrl != null) {
                    organizationRepository.getOrganization(orgId)?.let { currentOrg ->
                        organizationRepository.saveOrganization(currentOrg.copy(logoUrl = downloadUrl))
                    }
                } else {
                    _uploadError.value = "Upload failed. Storage returned null."
                }
            } catch (e: Exception) {
                Log.e("AdminDashboardVM", "Logo upload exception", e)
                _uploadError.value = "Upload Error: ${e.localizedMessage ?: e.message ?: "Unknown"}"
            } finally {
                _isUploadingLogo.value = false
            }
        }
    }

    fun clearUploadError() { _uploadError.value = null }

    // New Match Setup Methods
    fun addTeamToDivision(
        teamId: String, 
        division: String, 
        gender: String, 
        subDivision: String?, 
        coachEmail: String?
    ) {
        viewModelScope.launch {
            val orgId = _selectedOrgId.value ?: return@launch
            
            // Prioritize Live, then Active
            val seasons = seasonRepository.getSeasonsForOrganizationFlow(orgId).first()
            val targetSeason = seasons.find { it.live } ?: seasons.find { it.active } ?: return@launch
            
            val team = Team(
                name = teamId,
                seasonId = targetSeason.id,
                divisionName = division,
                gender = gender,
                subDivision = subDivision,
                coachEmail = coachEmail?.trim()?.lowercase(),
                organizationId = orgId
            )
            teamRepository.saveTeam(team)
            
            // Note: Optional auto-creation of Coach accounts can be added here
            // using the same logic as the Teams App if desired.
        }
    }

    fun deleteTeam(teamId: String) {
        viewModelScope.launch {
            teamRepository.deleteTeam(teamId)
        }
    }

    // New Print Setup Methods
    fun savePrintTemplate(template: PrintTemplate) {
        viewModelScope.launch {
            printTemplateRepository.saveTemplate(template)
        }
    }
}
