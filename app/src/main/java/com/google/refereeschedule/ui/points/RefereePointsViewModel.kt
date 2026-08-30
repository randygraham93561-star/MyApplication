package com.google.refereeschedule.ui.points

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.model.PointAward
import com.google.refereeschedule.domain.model.Team
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
    val isLoading: Boolean = true
)

@HiltViewModel
class RefereePointsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val teamRepository: TeamRepository,
    private val profileRepository: ProfileRepository
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
            
            if (orgId.isEmpty()) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            combine(
                teamRepository.getTeamsForOrganizationFlow(orgId),
                teamRepository.getUnassignedPointsFlow(orgId),
                teamRepository.getPointAwardsForOrganizationFlow(orgId)
            ) { teams, unassigned, allAwards ->
                // Group teams by division
                val grouped = teams.groupBy { it.divisionName }
                    .toSortedMap()

                RefereePointsUiState(
                    teamsByDivision = grouped,
                    unassignedPoints = unassigned,
                    pointHistory = allAwards.sortedByDescending { it.timestamp },
                    isLoading = false
                )
            }.collect {
                _uiState.value = it
            }
        }
    }
}
