package com.google.refereeschedule.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.model.RefereeProfile
import com.google.refereeschedule.domain.model.Organization
import com.google.refereeschedule.domain.model.Season
import com.google.refereeschedule.domain.model.Team
import com.google.refereeschedule.domain.repository.OrganizationRepository
import com.google.refereeschedule.domain.repository.ProfileRepository
import com.google.refereeschedule.domain.repository.SeasonRepository
import com.google.refereeschedule.domain.repository.TeamRepository
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiStateData(
    val profile: RefereeProfile,
    val teams: List<Team>,
    val organizations: List<Organization>,
    val seasons: List<Season>
)

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val data: ProfileUiStateData) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val seasonRepository: SeasonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUserFlow.flatMapLatest { firebaseUser ->
                if (firebaseUser != null) {
                    val uid = firebaseUser.uid
                    
                    combine(
                        profileRepository.getProfileFlow(uid),
                        organizationRepository.getOrganizationsFlow(),
                        seasonRepository.getSeasonsFlow(),
                        teamRepository.getTeamsFlow()
                    ) { profile, orgs, allSeasons, allTeams ->
                        val currentProfile = profile ?: RefereeProfile(
                            id = uid,
                            name = firebaseUser.displayName ?: firebaseUser.email ?: "Referee",
                            badgeLevel = "Regional"
                        )
                        
                        ProfileUiState.Success(
                            ProfileUiStateData(
                                profile = currentProfile,
                                teams = allTeams,
                                organizations = orgs,
                                seasons = allSeasons
                            )
                        )
                    }
                } else {
                    flowOf(ProfileUiState.Error("User not logged in"))
                }
            }.collect {
                _uiState.value = it
            }
        }
    }

    fun updateProfile(profile: RefereeProfile) {
        viewModelScope.launch {
            profileRepository.saveProfile(profile)
            
            // Also update the user's organizationId in the User collection
            val user = userRepository.getUser(profile.id)
            if (user != null && user.organizationId != profile.organizationId) {
                userRepository.saveUser(user.copy(organizationId = profile.organizationId))
            }
        }
    }
}
