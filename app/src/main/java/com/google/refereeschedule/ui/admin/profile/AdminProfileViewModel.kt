package com.google.refereeschedule.ui.admin.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.model.Organization
import com.google.refereeschedule.domain.repository.OrganizationRepository
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminProfileUiState(
    val organization: Organization? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class AdminProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminProfileUiState())
    val uiState: StateFlow<AdminProfileUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            val user = userRepository.getUser(uid)
            val orgId = user?.organizationId
            if (orgId != null) {
                val org = organizationRepository.getOrganization(orgId)
                _uiState.value = AdminProfileUiState(organization = org, isLoading = false)
            }
        }
    }

    fun updateOrganization(organization: Organization) {
        viewModelScope.launch {
            organizationRepository.saveOrganization(organization)
            _uiState.value = _uiState.value.copy(organization = organization)
        }
    }
}
