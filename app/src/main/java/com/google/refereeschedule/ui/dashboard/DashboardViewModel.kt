package com.google.refereeschedule.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.model.Assignment
import com.google.refereeschedule.domain.model.Division
import com.google.refereeschedule.domain.model.Game
import com.google.refereeschedule.domain.model.UserRole
import com.google.refereeschedule.domain.repository.AssignmentRepository
import com.google.refereeschedule.domain.repository.GameRepository
import com.google.refereeschedule.domain.repository.ProfileRepository
import com.google.refereeschedule.domain.repository.SeasonRepository
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class DashboardUiState(
    val userRole: UserRole = UserRole.Referee,
    val userName: String? = null,
    val upcomingAssignments: List<Triple<Assignment, Game, Division?>> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val gameRepository: GameRepository,
    private val profileRepository: ProfileRepository,
    private val assignmentRepository: AssignmentRepository,
    private val seasonRepository: SeasonRepository
) : ViewModel() {
    val currentUserEmail: String? = authRepository.currentUser?.email
    private val currentUserId = authRepository.currentUser?.uid

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val uid = currentUserId ?: return

        viewModelScope.launch {
            val userFlow = userRepository.getUserFlow(uid)
            val profileFlow = profileRepository.getProfileFlow(uid)
            val assignmentsFlow = assignmentRepository.getAssignmentsForReferee(uid)
            val gamesFlow = gameRepository.getGamesFlow()
            val seasonsFlow = seasonRepository.getSeasonsFlow()

            combine(userFlow, profileFlow, assignmentsFlow, gamesFlow, seasonsFlow) { user, profile, assignments, games, seasons ->
                val upcoming = assignments.mapNotNull { assignment ->
                    val game = games.find { it.id == assignment.gameId }
                    if (game != null) {
                        val season = seasons.find { it.id == game.seasonId }
                        val division = season?.divisions?.find { it.name == game.divisionName }
                        Triple(assignment, game, division)
                    } else null
                }.sortedBy { it.second.date }

                DashboardUiState(
                    userRole = user?.role ?: UserRole.Referee,
                    userName = profile?.name,
                    upcomingAssignments = upcoming,
                    isLoading = false
                )
            }.collect {
                _uiState.value = it
            }
        }
    }

    fun checkIn(assignment: Assignment) {
        viewModelScope.launch {
            assignmentRepository.saveAssignment(assignment.copy(checkedIn = true))
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        authRepository.signOut()
        onSignedOut()
    }
}
