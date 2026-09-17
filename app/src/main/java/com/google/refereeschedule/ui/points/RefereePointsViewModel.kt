package com.google.refereeschedule.ui.points

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.model.Organization
import com.google.refereeschedule.domain.model.PointAward
import com.google.refereeschedule.domain.model.Team
import com.google.refereeschedule.domain.repository.OrganizationRepository
import com.google.refereeschedule.domain.repository.ProfileRepository
import com.google.refereeschedule.domain.repository.TeamRepository
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RefereePointsUiState(
    val teamsByDivision: Map<String, List<Team>> = emptyMap(),
    val unassignedPoints: Int = 0,
    val pointHistory: List<PointAward> = emptyList(),
    val organization: Organization? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class RefereePointsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val teamRepository: TeamRepository,
    private val profileRepository: ProfileRepository,
    private val organizationRepository: OrganizationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RefereePointsUiState())
    val uiState: StateFlow<RefereePointsUiState> = _uiState.asStateFlow()

    init {
        loadPoints()
    }

    private fun loadPoints() {
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid ?: return@launch
            val user = userRepository.getUser(uid)
            val profile = profileRepository.getProfile(uid)
            val orgId = user?.organizationId ?: profile?.organizationId ?: ""
            val seasonId = profile?.currentSeasonId ?: ""
            
            if (orgId.isEmpty()) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            combine(
                teamRepository.getTeamsForOrganizationFlow(orgId),
                teamRepository.getUnassignedPointsFlow(orgId, seasonId),
                teamRepository.getPointAwardsForOrganizationFlow(orgId, seasonId),
                organizationRepository.getOrganizationsFlow()
            ) { teams, unassigned, allAwards, allOrgs ->
                // Filter teams by current season
                val filteredTeams = teams.filter { it.seasonId == seasonId }
                
                // Group teams by division and gender
                val grouped = filteredTeams.groupBy { "${it.divisionName} ${it.gender}" }
                    .toSortedMap()

                RefereePointsUiState(
                    teamsByDivision = grouped,
                    unassignedPoints = unassigned,
                    pointHistory = allAwards.sortedByDescending { it.timestamp },
                    organization = allOrgs.find { it.id == orgId },
                    isLoading = false
                )
            }.collect {
                _uiState.value = it
            }
        }
    }
}
