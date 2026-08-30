package com.google.refereeschedule.ui.admin.teams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.model.Division
import com.google.refereeschedule.domain.model.Season
import com.google.refereeschedule.domain.model.Team
import com.google.refereeschedule.domain.repository.SeasonRepository
import com.google.refereeschedule.domain.repository.TeamRepository
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamManagementUiState(
    val seasons: List<Season> = emptyList(),
    val selectedSeason: Season? = null,
    val selectedDivision: Division? = null,
    val teams: List<Team> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class TeamManagementViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val seasonRepository: SeasonRepository,
    private val teamRepository: TeamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeamManagementUiState())
    val uiState: StateFlow<TeamManagementUiState> = _uiState.asStateFlow()

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
                        loadTeams(activeSeason)
                    }
                }.launchIn(viewModelScope)
        }
    }

    private fun loadTeams(season: Season) {
        teamRepository.getTeamsForSeasonFlow(season.id)
            .onEach { teams ->
                _uiState.update { it.copy(teams = teams) }
            }.launchIn(viewModelScope)
    }

    fun selectSeason(season: Season) {
        _uiState.update { it.copy(selectedSeason = season, selectedDivision = null) }
        loadTeams(season)
    }

    fun selectDivision(division: Division) {
        _uiState.update { it.copy(selectedDivision = division) }
    }

    fun addTeam(name: String) {
        viewModelScope.launch {
            val season = _uiState.value.selectedSeason ?: return@launch
            val division = _uiState.value.selectedDivision ?: return@launch
            
            val team = Team(
                name = name,
                seasonId = season.id,
                divisionName = division.name,
                organizationId = season.organizationId
            )
            teamRepository.saveTeam(team)
        }
    }

    fun updateTeam(team: Team, newName: String) {
        viewModelScope.launch {
            teamRepository.saveTeam(team.copy(name = newName))
        }
    }

    fun deleteTeam(teamId: String) {
        viewModelScope.launch {
            teamRepository.deleteTeam(teamId)
        }
    }
}
