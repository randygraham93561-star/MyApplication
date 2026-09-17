package com.google.refereeschedule.ui.scheduler

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.model.Assignment
import com.google.refereeschedule.domain.model.Game
import com.google.refereeschedule.domain.model.Organization
import com.google.refereeschedule.domain.model.RefereeProfile
import com.google.refereeschedule.domain.model.User
import com.google.refereeschedule.domain.repository.AssignmentRepository
import com.google.refereeschedule.domain.repository.GameRepository
import com.google.refereeschedule.domain.repository.OrganizationRepository
import com.google.refereeschedule.domain.repository.ProfileRepository
import com.google.refereeschedule.domain.repository.SeasonRepository
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SchedulerViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val assignmentRepository: AssignmentRepository,
    private val userRepository: UserRepository,
    private val seasonRepository: SeasonRepository,
    private val organizationRepository: OrganizationRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(Date())
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<RefereeProfile?>(null)
    val currentUserProfile: StateFlow<RefereeProfile?> = _currentUserProfile.asStateFlow()

    private val _organization = MutableStateFlow<Organization?>(null)
    val organization: StateFlow<com.google.refereeschedule.domain.model.Organization?> = _organization.asStateFlow()

    val allAssignments = assignmentRepository.getAllAssignmentsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers = userRepository.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProfiles = profileRepository.getAllProfilesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<SchedulerUiState>(SchedulerUiState.Loading)
    val uiState: StateFlow<SchedulerUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
        
        val userFlow = userRepository.getUserFlow(authRepository.currentUser?.uid ?: "")
        
        viewModelScope.launch {
            userFlow.combine(_currentUserProfile) { user, profile ->
                user?.organizationId ?: profile?.organizationId
            }.filterNotNull().distinctUntilChanged().collect { orgId ->
                _organization.value = organizationRepository.getOrganization(orgId)
            }
        }

        val organizationIdFlow = userFlow.combine(_currentUserProfile) { user, profile ->
            (user?.organizationId ?: profile?.organizationId) to (profile?.currentSeasonId ?: "")
        }.distinctUntilChanged()

        _selectedDate.combine(organizationIdFlow) { date, (orgId, seasonId) ->
            Triple(date, orgId, seasonId)
        }.flatMapLatest { (date, orgId, seasonId) ->
            combine(gameRepository.getGamesFlow(), seasonRepository.getSeasonsFlow()) { games, seasons ->
                val tz = TimeZone.getTimeZone(_organization.value?.timeZone ?: "UTC")
                val dateStr = formatDate(date, tz)
                games.filter { game ->
                    val season = seasons.find { it.id == game.seasonId }
                    val isLive = season?.live == true
                    val gameDate = game.date ?: return@filter false
                    
                    formatDate(gameDate, tz) == dateStr && 
                    (orgId.isNullOrEmpty() || game.organizationId == orgId) &&
                    (seasonId.isEmpty() || game.seasonId == seasonId) &&
                    isLive // Only show live seasons
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

    fun requestMentor(gameId: String) {
        viewModelScope.launch {
            gameRepository.getGame(gameId)?.let { game ->
                gameRepository.saveGame(game.copy(isMentorRequested = true))
            }
        }
    }

    fun cancelAssignment(assignmentId: String) {
        viewModelScope.launch {
            assignmentRepository.deleteAssignment(assignmentId)
        }
    }

    private fun formatDate(date: Date, tz: TimeZone): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.timeZone = tz
        return sdf.format(date)
    }
}

sealed interface SchedulerUiState {
    data object Loading : SchedulerUiState
    data class Success(val games: List<Game>) : SchedulerUiState
    data class Error(val message: String) : SchedulerUiState
}
