package com.google.refereeschedule.ui.admin.scheduler

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.model.Assignment
import com.google.refereeschedule.domain.model.AssignmentPosition
import com.google.refereeschedule.domain.model.AssignmentStatus
import com.google.refereeschedule.domain.model.BulkGameData
import com.google.refereeschedule.domain.model.Division
import com.google.refereeschedule.domain.model.DivisionDifficulty
import com.google.refereeschedule.domain.model.Game
import com.google.refereeschedule.domain.model.Organization
import com.google.refereeschedule.domain.model.Season
import com.google.refereeschedule.domain.model.Team
import com.google.refereeschedule.domain.repository.AssignmentRepository
import com.google.refereeschedule.domain.repository.GameRepository
import com.google.refereeschedule.domain.repository.OrganizationRepository
import com.google.refereeschedule.domain.repository.SeasonRepository
import com.google.refereeschedule.domain.repository.TeamRepository
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class GameSchedulerUiState(
    val seasons: List<Season> = emptyList(),
    val selectedSeason: Season? = null,
    val selectedDivision: Division? = null,
    val selectedDate: Date = Date(),
    val teamsInDivision: List<Team> = emptyList(),
    val gamesOnSelectedDate: List<Game> = emptyList(),
    val allReferees: List<com.google.refereeschedule.domain.model.User> = emptyList(),
    val organization: Organization? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class GameSchedulerViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val seasonRepository: SeasonRepository,
    private val teamRepository: TeamRepository,
    private val gameRepository: GameRepository,
    private val assignmentRepository: AssignmentRepository,
    private val organizationRepository: OrganizationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameSchedulerUiState())
    val uiState: StateFlow<GameSchedulerUiState> = _uiState.asStateFlow()

    private val _organization = MutableStateFlow<Organization?>(null)
    
    val allAssignments = assignmentRepository.getAllAssignmentsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val currentUserId = authRepository.currentUser?.uid

    init {
        loadSeasons()
    }

    private fun loadSeasons() {
        viewModelScope.launch {
            val uid = currentUserId ?: return@launch
            val user = userRepository.getUser(uid)
            val orgId = user?.organizationId ?: return@launch

            val org = organizationRepository.getOrganization(orgId)
            _organization.value = org
            _uiState.update { it.copy(organization = org) }

            seasonRepository.getSeasonsForOrganizationFlow(orgId)
                .onEach { seasons ->
                    val activeSeason = seasons.find { it.active }
                    _uiState.update { it.copy(
                        seasons = seasons,
                        selectedSeason = activeSeason,
                        isLoading = false
                    )}
                    if (activeSeason != null) {
                        loadTeamsAndGames(activeSeason)
                    }
                }.launchIn(viewModelScope)

            userRepository.getUsersForOrganizationFlow(orgId)
                .onEach { users ->
                    _uiState.update { it.copy(allReferees = users) }
                }.launchIn(viewModelScope)
        }
    }

    private fun loadTeamsAndGames(season: Season) {
        val division = _uiState.value.selectedDivision ?: return
        val tz = TimeZone.getTimeZone(_organization.value?.timeZone ?: "UTC")
        
        teamRepository.getTeamsForSeasonFlow(season.id)
            .onEach { teams ->
                val filteredTeams = teams.filter { it.divisionName == division.name }
                _uiState.update { it.copy(teamsInDivision = filteredTeams) }
            }.launchIn(viewModelScope)

        gameRepository.getGamesForOrganizationFlow(season.organizationId)
            .onEach { games ->
                val dateStr = formatDate(_uiState.value.selectedDate, tz)
                val filteredGames = games.filter { g ->
                    val gameDate = g.date ?: return@filter false
                    g.seasonId == season.id && 
                    g.divisionName == division.name &&
                    formatDate(gameDate, tz) == dateStr
                }
                _uiState.update { it.copy(gamesOnSelectedDate = filteredGames) }
            }.launchIn(viewModelScope)
    }

    fun selectSeason(season: Season) {
        _uiState.update { it.copy(selectedSeason = season, selectedDivision = null) }
    }

    fun selectDivision(division: Division) {
        _uiState.update { it.copy(selectedDivision = division) }
        _uiState.value.selectedSeason?.let { loadTeamsAndGames(it) }
    }

    fun selectDate(date: Date) {
        _uiState.update { it.copy(selectedDate = date) }
        _uiState.value.selectedSeason?.let { loadTeamsAndGames(it) }
    }

    fun createGame(
        homeTeam: Team,
        awayTeam: Team,
        time: String,
        location: String,
        fieldNumber: String,
        gender: String,
        isFriendly: Boolean
    ) {
        viewModelScope.launch {
            val season = _uiState.value.selectedSeason ?: return@launch
            val division = _uiState.value.selectedDivision ?: return@launch
            
            val latestNumber = gameRepository.getLatestGameNumber(season.id)
            
            val game = Game(
                gameNumber = latestNumber + 1,
                date = _uiState.value.selectedDate,
                time = time,
                location = location,
                fieldNumber = fieldNumber,
                divisionName = division.name,
                gender = gender,
                ageGroup = division.name,
                homeTeamName = homeTeam.name,
                homeTeamId = homeTeam.id,
                awayTeamName = awayTeam.name,
                awayTeamId = awayTeam.id,
                difficultyLevel = DivisionDifficulty.getLevelForDivision(division.name, gender),
                seasonId = season.id,
                organizationId = season.organizationId,
                mentorsAllowed = division.mentorsAllowed,
                isFriendly = isFriendly,
                requiredCrewSize = 3 // Default
            )
            gameRepository.saveGame(game)
        }
    }

    fun createGames(games: List<BulkGameData>, gender: String) {
        viewModelScope.launch {
            val season = _uiState.value.selectedSeason ?: return@launch
            val division = _uiState.value.selectedDivision ?: return@launch
            
            var latestNumber = gameRepository.getLatestGameNumber(season.id)
            
            games.forEach { data ->
                latestNumber++
                val game = Game(
                    gameNumber = latestNumber,
                    date = _uiState.value.selectedDate,
                    time = data.time,
                    location = data.location,
                    fieldNumber = data.fieldNumber,
                    divisionName = division.name,
                    gender = gender,
                    ageGroup = division.name,
                    homeTeamName = data.homeTeam.name,
                    homeTeamId = data.homeTeam.id,
                    awayTeamName = data.awayTeam.name,
                    awayTeamId = data.awayTeam.id,
                    difficultyLevel = DivisionDifficulty.getLevelForDivision(division.name, gender),
                    seasonId = season.id,
                    organizationId = season.organizationId,
                    mentorsAllowed = division.mentorsAllowed,
                    isFriendly = data.isFriendly,
                    requiredCrewSize = 3 // Default
                )
                gameRepository.saveGame(game)
            }
        }
    }

    fun deleteGame(gameId: String) {
        viewModelScope.launch {
            gameRepository.deleteGame(gameId)
        }
    }

    fun updateGame(game: Game) {
        viewModelScope.launch {
            gameRepository.saveGame(game)
        }
    }

    fun assignReferee(
        gameId: String,
        refereeId: String,
        position: AssignmentPosition
    ) {
        viewModelScope.launch {
            if (refereeId.isEmpty()) return@launch
            val user = userRepository.getUser(refereeId)
            val orgId = user?.organizationId ?: return@launch

            val assignment = Assignment(
                gameId = gameId,
                refereeId = refereeId,
                position = position,
                status = com.google.refereeschedule.domain.model.AssignmentStatus.Confirmed, // Admin assignments are auto-confirmed
                organizationId = orgId
            )
            assignmentRepository.saveAssignment(assignment)
        }
    }

    fun getGameStartAndEnd(game: Game): Pair<Long, Long>? {
        val start = parseGameDateTime(game) ?: return null
        val duration = getGameDuration(game)
        return start to (start + (duration * 60 * 1000))
    }

    private fun getGameDuration(game: Game): Int {
        val season = _uiState.value.seasons.find { it.id == game.seasonId }
        val division = season?.divisions?.find { it.name == game.divisionName }
        val halves = division?.halfDurationMinutes ?: 45
        return (halves * 2) + 5
    }

    private fun parseGameDateTime(game: Game): Long? {
        val gameDate = game.date ?: return null
        val tz = TimeZone.getTimeZone(_organization.value?.timeZone ?: "UTC")
        val dateStr = formatDate(gameDate, tz)
        val fullStr = "$dateStr ${game.time}"
        
        // Try 24h format first (new standardized format)
        val sdf24 = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply { timeZone = tz }
        val time24 = try { sdf24.parse(fullStr)?.time } catch (e: Exception) { null }
        if (time24 != null) return time24

        // Fallback to 12h format (legacy data)
        val sdf12 = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).apply { timeZone = tz }
        return try { sdf12.parse(fullStr)?.time } catch (e: Exception) { null }
    }

    private fun formatDate(date: Date, tz: TimeZone = TimeZone.getTimeZone("UTC")): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.timeZone = tz
        return sdf.format(date)
    }
}
