package com.google.refereeschedule.ui.admin.system

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.domain.model.Organization
import com.google.refereeschedule.domain.model.User
import com.google.refereeschedule.domain.model.UserRole
import com.google.refereeschedule.domain.repository.OrganizationRepository
import com.google.refereeschedule.domain.repository.ProfileRepository
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserManagementUiState(
    val users: List<User> = emptyList(),
    val organizations: List<Organization> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        combine(
            userRepository.getAllUsersFlow(),
            organizationRepository.getOrganizationsFlow()
        ) { users, orgs ->
            UserManagementUiState(
                users = users,
                organizations = orgs,
                isLoading = false
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun updateUserRole(user: com.google.refereeschedule.domain.model.User, newRole: com.google.refereeschedule.domain.model.UserRole, organizationId: String? = null) {
        viewModelScope.launch {
            try {
                val updatedRoles = user.roles.toMutableMap().apply {
                    put("referee", newRole.name)
                }
                val updatedUser = when (newRole) {
                    com.google.refereeschedule.domain.model.UserRole.Admin -> {
                        val newAdminOrgs = if (organizationId != null) {
                            (user.adminOrgIds + organizationId).distinct()
                        } else user.adminOrgIds
                        user.copy(roles = updatedRoles, adminOrgIds = newAdminOrgs)
                    }
                    com.google.refereeschedule.domain.model.UserRole.CoachAdmin -> {
                        val newCoachOrgs = if (organizationId != null) {
                            (user.coachOrgIds + organizationId).distinct()
                        } else user.coachOrgIds
                        user.copy(roles = updatedRoles, coachOrgIds = newCoachOrgs)
                    }
                    else -> user.copy(roles = updatedRoles, organizationId = organizationId)
                }
                userRepository.saveUser(updatedUser)
            } catch (e: Exception) {
                // In a real app, we'd show an error snackbar
            }
        }
    }

    fun removeAdminAccess(user: com.google.refereeschedule.domain.model.User, orgId: String) {
        viewModelScope.launch {
            val updatedUser = user.copy(adminOrgIds = user.adminOrgIds.filter { it != orgId })
            userRepository.saveUser(updatedUser)
        }
    }

    fun removeCoachAccess(user: com.google.refereeschedule.domain.model.User, orgId: String) {
        viewModelScope.launch {
            val updatedUser = user.copy(coachOrgIds = user.coachOrgIds.filter { it != orgId })
            userRepository.saveUser(updatedUser)
        }
    }

    fun unlockUser(userId: String) {
        viewModelScope.launch {
            try {
                userRepository.getUser(userId)?.let { user ->
                    userRepository.saveUser(user.copy(isAccountLocked = false))
                }
                profileRepository.getProfile(userId)?.let { profile ->
                    profileRepository.saveProfile(profile.copy(isAccountLocked = false))
                }
            } catch (e: Exception) {
                android.util.Log.e("UserManagement", "Failed to unlock user", e)
            }
        }
    }
}
