package com.google.refereeschedule.ui.admin.system

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.domain.model.Organization
import com.google.refereeschedule.domain.model.User
import com.google.refereeschedule.domain.model.UserRole
import com.google.refereeschedule.domain.repository.OrganizationRepository
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
    private val organizationRepository: OrganizationRepository
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

    fun updateUserRole(user: User, newRole: UserRole, organizationId: String? = null) {
        viewModelScope.launch {
            try {
                userRepository.saveUser(user.copy(role = newRole, organizationId = organizationId))
            } catch (e: Exception) {
                // In a real app, we'd show an error snackbar
            }
        }
    }
}
