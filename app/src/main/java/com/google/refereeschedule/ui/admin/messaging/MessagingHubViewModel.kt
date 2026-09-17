package com.google.refereeschedule.ui.admin.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.model.Announcement
import com.google.refereeschedule.domain.model.RefereeProfile
import com.google.refereeschedule.domain.repository.AnnouncementRepository
import com.google.refereeschedule.domain.repository.ProfileRepository
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessagingHubUiState(
    val referees: List<RefereeProfile> = emptyList(),
    val selectedRefereeIds: Set<String> = emptySet(),
    val isSending: Boolean = false,
    val isLoading: Boolean = true,
    val success: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MessagingHubViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository,
    private val announcementRepository: AnnouncementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagingHubUiState())
    val uiState: StateFlow<MessagingHubUiState> = _uiState.asStateFlow()

    init {
        loadReferees()
    }

    private fun loadReferees() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            val user = userRepository.getUser(uid)
            val orgId = user?.organizationId
            
            if (orgId != null) {
                profileRepository.getProfilesForOrganizationFlow(orgId)
                    .onEach { list ->
                        _uiState.update { it.copy(referees = list.sortedBy { it.name }, isLoading = false) }
                    }.launchIn(viewModelScope)
            }
        }
    }

    fun toggleRefereeSelection(refId: String) {
        _uiState.update { state ->
            val current = state.selectedRefereeIds
            val new = if (current.contains(refId)) current - refId else current + refId
            state.copy(selectedRefereeIds = new)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedRefereeIds = state.referees.map { it.id }.toSet())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedRefereeIds = emptySet()) }
    }

    fun sendMessage(title: String, message: String) {
        val uid = authRepository.currentUser?.uid ?: return
        val state = _uiState.value
        
        if (state.selectedRefereeIds.isEmpty()) {
            _uiState.update { it.copy(error = "Please select at least one recipient.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }
            try {
                val user = userRepository.getUser(uid)
                val orgId = user?.organizationId ?: throw Exception("Organization not found.")

                // For now, announcements are org-wide in the current repository implementation.
                // We'll extend this to support targeted messaging if needed, but the prompt 
                // mentioned "broadcast" and "messaging working".
                // In a true messaging hub, we'd save targeted messages to a 'messages' collection.
                // For this implementation, we'll use the existing Announcement system as requested.
                
                val isBroadcast = state.selectedRefereeIds.size == state.referees.size
                
                val announcement = Announcement(
                    organizationId = orgId,
                    senderId = uid,
                    recipientIds = if (isBroadcast) emptyList() else state.selectedRefereeIds.toList(),
                    title = title,
                    message = message
                )
                announcementRepository.sendAnnouncement(announcement)
                
                _uiState.update { it.copy(isSending = false, success = true, selectedRefereeIds = emptySet()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSending = false, error = e.message) }
            }
        }
    }

    fun clearStatus() {
        _uiState.update { it.copy(success = false, error = null) }
    }
}
