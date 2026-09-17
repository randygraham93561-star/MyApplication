package com.google.refereeschedule.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.model.Organization
import com.google.refereeschedule.domain.model.RefereeProfile
import com.google.refereeschedule.domain.model.Team
import com.google.refereeschedule.domain.repository.OrganizationRepository
import com.google.refereeschedule.domain.repository.ProfileRepository
import com.google.refereeschedule.domain.repository.SeasonRepository
import com.google.refereeschedule.domain.repository.TeamRepository
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class OnboardingUiState(
    val organizations: List<Organization> = emptyList(),
    val teams: List<Team> = emptyList(),
    val profile: RefereeProfile? = null,
    val activeSeasonId: String? = null,
    val isAccountLocked: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val organizationRepository: OrganizationRepository,
    private val teamRepository: TeamRepository,
    private val seasonRepository: SeasonRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _onboardingOrgId = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid ?: ""
            combine(
                organizationRepository.getOrganizationsFlow(),
                teamRepository.getTeamsFlow(),
                profileRepository.getProfileFlow(uid),
                seasonRepository.getSeasonsFlow(),
                _onboardingOrgId
            ) { orgs, teams, profile, seasons, selectedOrgId ->
                // Priority: UI selection (for new accounts) -> Profile setting (for returning)
                val orgId = selectedOrgId ?: profile?.organizationId ?: ""
                val activeSeason = seasons.find { it.organizationId == orgId && it.active }
                val filteredTeams = teams.filter { it.seasonId == activeSeason?.id }
                
                OnboardingUiState(
                    organizations = orgs,
                    teams = filteredTeams,
                    profile = profile,
                    activeSeasonId = activeSeason?.id,
                    isAccountLocked = profile?.isAccountLocked ?: false,
                    isLoading = false
                )
            }.collect {
                _uiState.value = it
            }
        }
    }

    fun selectOrganization(orgId: String) {
        _onboardingOrgId.value = orgId
    }

    fun submitOnboarding(
        name: String,
        dob: Date?,
        organizationId: String,
        teamIds: List<String>,
        headComfort: Int,
        assistantComfort: Int,
        isNew: Boolean,
        takenCourse: Boolean,
        onSuccess: () -> Unit
    ) {
        val uid = authRepository.currentUser?.uid ?: return
        val currentState = _uiState.value
        val existingProfile = currentState.profile
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                // Find the active season for this organization ID manually to be sure
                val allSeasons = seasonRepository.getSeasonsFlow().first()
                val activeSeasonId = allSeasons.find { it.organizationId == organizationId && it.active }?.id ?: ""

                // If it's a new season onboarding for a returning referee:
                // isNewReferee becomes FALSE.
                val finalIsNew = if (existingProfile?.isOnboardingCompleted == true) false else isNew
                
                // If new and no course, lock the account
                val shouldLock = finalIsNew && !takenCourse
                
                val profile = (existingProfile ?: RefereeProfile()).copy(
                    id = uid,
                    name = name,
                    dateOfBirth = dob,
                    organizationId = organizationId,
                    teamIdsForPoints = teamIds,
                    headRefereeComfortLevel = headComfort,
                    assistantRefereeComfortLevel = assistantComfort,
                    isNewReferee = finalIsNew,
                    hasTakenCourse = if (existingProfile?.isOnboardingCompleted == true) true else takenCourse,
                    isOnboardingCompleted = true,
                    lastOnboardedSeasonId = activeSeasonId,
                    currentSeasonId = activeSeasonId.ifEmpty { existingProfile?.currentSeasonId ?: "" },
                    isAccountLocked = shouldLock,
                    badgeLevel = existingProfile?.badgeLevel ?: "Regional"
                )
                
                profileRepository.saveProfile(profile)
                
                // Also link org to User account
                val user = userRepository.getUser(uid)
                if (user != null && user.organizationId.isNullOrEmpty()) {
                    userRepository.saveUser(user.copy(organizationId = organizationId))
                }
                
                _uiState.update { it.copy(isSaving = false) }
                // Small delay to let DB propagation start before UI navigates
                delay(500)
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}
