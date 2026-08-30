package com.google.refereeschedule.ui.admin.system

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.domain.model.Organization
import com.google.refereeschedule.domain.model.User
import com.google.refereeschedule.domain.model.UserRole
import com.google.refereeschedule.domain.repository.OrganizationRepository
import com.google.refereeschedule.domain.model.RefereeProfile
import com.google.refereeschedule.domain.repository.ProfileRepository
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SystemAdminUiState(
    val organizations: List<Organization> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SystemAdminDashboardViewModel @Inject constructor(
    private val organizationRepository: OrganizationRepository,
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SystemAdminUiState())
    val uiState: StateFlow<SystemAdminUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        organizationRepository.getOrganizationsFlow()
            .onEach { organizations ->
                _uiState.value = _uiState.value.copy(
                    organizations = organizations,
                    isLoading = false
                )
            }.launchIn(viewModelScope)
    }

    fun createOrganization(name: String, adminEmail: String) {
        viewModelScope.launch {
            val org = Organization(name = name, contactEmail = adminEmail)
            organizationRepository.saveOrganization(org)
            // Note: In a real app, we'd also handle creating the admin user or inviting them
        }
    }

    fun updateOrganization(organization: Organization) {
        viewModelScope.launch {
            organizationRepository.saveOrganization(organization)
        }
    }

    fun createAdminForOrganization(organizationId: String, email: String) {
        viewModelScope.launch {
            // Note: In a real app, we'd use Firebase Auth to create the user
            // and get a proper UID. For now, saveUser handles generating a doc ID.
            val user = User(
                email = email,
                role = UserRole.Admin,
                organizationId = organizationId
            )
            val savedUserId = userRepository.saveUserWithReturn(user)
            
            // Also create a referee profile for the admin so they can manage themselves
            if (savedUserId.isNotEmpty()) {
                val profile = RefereeProfile(
                    id = savedUserId,
                    name = email.substringBefore("@"),
                    badgeLevel = "Regional",
                    organizationId = organizationId
                )
                profileRepository.saveProfile(profile)
            }
        }
    }
}
