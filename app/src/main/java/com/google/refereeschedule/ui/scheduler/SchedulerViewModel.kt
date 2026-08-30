package com.google.refereeschedule.ui.scheduler

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.model.Assignment
import com.google.refereeschedule.domain.model.Game
import com.google.refereeschedule.domain.model.RefereeProfile
import com.google.refereeschedule.domain.model.User
import com.google.refereeschedule.domain.repository.AssignmentRepository
import com.google.refereeschedule.domain.repository.GameRepository
import com.google.refereeschedule.domain.repository.ProfileRepository
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SchedulerViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val assignmentRepository: AssignmentRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(Date())
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<RefereeProfile?>(null)
    val currentUserProfile: StateFlow<RefereeProfile?> = _currentUserProfile.asStateFlow()

    val allAssignments = assignmentRepository.getAllAssignmentsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers = userRepository.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<SchedulerUiState>(SchedulerUiState.Loading)
    val uiState: StateFlow<SchedulerUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
        
        val organizationIdFlow = combine(
            userRepository.getUserFlow(authRepository.currentUser?.uid ?: ""),
            _currentUserProfile
        ) { user, profile ->
            (user?.organizationId ?: profile?.organizationId) to (profile?.currentSeasonId ?: "")
        }.distinctUntilChanged()

        _selectedDate.combine(organizationIdFlow) { date, (orgId, seasonId) ->
            Triple(date, orgId, seasonId)
        }.flatMapLatest { (date, orgId, seasonId) ->
            gameRepository.getGamesFlow().map { games ->
                val dateStr = formatDate(date)
                games.filter { 
                    formatDate(it.date) == dateStr && 
                    (orgId.isNullOrEmpty() || it.organizationId == orgId) &&
                    (seasonId.isEmpty() || it.seasonId == seasonId)
                }
            }
        }.onEach { filteredGames ->
            _uiState.value = SchedulerUiState.Success(filteredGames)
        }.launchIn(viewModelScope)
    }

    private fun loadUserProfile() {
        val uid = authRepository.currentUser?.uid ?: return
        val email = authRepository.currentUser?.email ?: ""
        profileRepository.getProfileFlow(uid)
            .onEach { profile ->
                _currentUserProfile.value = profile ?: RefereeProfile(
                    id = uid,
                    name = email.substringBefore("@")
                )
            }.launchIn(viewModelScope)
    }

    fun selectDate(date: Date) {
        _selectedDate.value = date
    }

    fun cancelAssignment(assignmentId: String) {
        viewModelScope.launch {
            assignmentRepository.deleteAssignment(assignmentId)
        }
    }

    private fun formatDate(date: Date): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }
}

sealed interface SchedulerUiState {
    data object Loading : SchedulerUiState
    data class Success(val games: List<Game>) : SchedulerUiState
    data class Error(val message: String) : SchedulerUiState
}
