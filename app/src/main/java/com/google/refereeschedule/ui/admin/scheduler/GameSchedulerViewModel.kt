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
import com.google.refereeschedule.domain.model.Season
import com.google.refereeschedule.domain.model.Team
import com.google.refereeschedule.domain.repository.AssignmentRepository
import com.google.refereeschedule.domain.repository.GameRepository
import com.google.refereeschedule.domain.repository.SeasonRepository
import com.google.refereeschedule.domain.repository.TeamRepository
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val isLoading: Boolean = true
)

@HiltViewModel
class GameSchedulerViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val seasonRepository: SeasonRepository,
    private val teamRepository: TeamRepository,
    private val gameRepository: GameRepository,
    private val assignmentRepository: AssignmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameSchedulerUiState())
    val uiState: StateFlow<GameSchedulerUiState> = _uiState.asStateFlow()
    
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

            seasonRepository.getSeasonsForOrganizationFlow(orgId)
                .onEach { seasons ->
                    val activeSeason = seasons.find { it.isActive }
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
        
        teamRepository.getTeamsForSeasonFlow(season.id)
            .onEach { teams ->
                val filteredTeams = teams.filter { it.divisionName == division.name }
                _uiState.update { it.copy(teamsInDivision = filteredTeams) }
            }.launchIn(viewModelScope)

        gameRepository.getGamesForOrganizationFlow(season.organizationId)
            .onEach { games ->
                val dateStr = formatDate(_uiState.value.selectedDate)
                val filteredGames = games.filter { 
                    it.seasonId == season.id && 
                    it.divisionName == division.name &&
                    formatDate(it.date) == dateStr
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
                ageGroup = division.name,
                homeTeamName = homeTeam.name,
                awayTeamName = awayTeam.name,
                difficultyLevel = DivisionDifficulty.getLevelForDivision(division.name),
                seasonId = season.id,
                organizationId = season.organizationId,
                mentorsAllowed = division.mentorsAllowed,
                isFriendly = isFriendly,
                requiredCrewSize = 3 // Default
            )
            gameRepository.saveGame(game)
        }
    }

    fun createGames(games: List<BulkGameData>) {
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
                    ageGroup = division.name,
                    homeTeamName = data.homeTeam.name,
                    awayTeamName = data.awayTeam.name,
                    difficultyLevel = DivisionDifficulty.getLevelForDivision(division.name),
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
        return try {
            val dateStr = formatDate(game.date)
            val fullStr = "$dateStr ${game.time}"
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(fullStr)?.time
        } catch (e: Exception) {
            null
        }
    }

    private fun formatDate(date: Date): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }
}
