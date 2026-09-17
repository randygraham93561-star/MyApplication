package com.google.refereeschedule.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.model.Announcement
import com.google.refereeschedule.domain.repository.AnnouncementRepository
import com.google.refereeschedule.domain.repository.ProfileRepository
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InboxUiState(
    val announcements: List<Announcement> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository,
    private val announcementRepository: AnnouncementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        loadAnnouncements()
    }

    private fun loadAnnouncements() {
        val uid = authRepository.currentUser?.uid ?: return
        
        viewModelScope.launch {
            val user = userRepository.getUser(uid)
            val profile = profileRepository.getProfile(uid)
            val orgId = user?.organizationId ?: profile?.organizationId
            
            if (orgId != null) {
                announcementRepository.getAnnouncementsForOrganization(orgId)
                    .onEach { list ->
                        val filtered = list.filter { 
                            it.recipientIds.isEmpty() || it.recipientIds.contains(uid) || it.senderId == uid 
                        }
                        _uiState.update { it.copy(
                            announcements = filtered.sortedByDescending { it.timestamp },
                            isLoading = false
                        ) }
                    }
                    .catch { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
                    .launchIn(viewModelScope)
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Organization not found.") }
            }
        }
    }
}
