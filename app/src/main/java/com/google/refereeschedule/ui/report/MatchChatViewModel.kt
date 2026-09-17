package com.google.refereeschedule.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.model.ChatMessage
import com.google.refereeschedule.domain.repository.ChatRepository
import com.google.refereeschedule.domain.repository.GameRepository
import com.google.refereeschedule.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentUserId: String = "",
    val channelTitle: String = "Communication",
    val isLoading: Boolean = true
)

@HiltViewModel
class MatchChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var channelId: String = ""

    fun setChannel(id: String) {
        this.channelId = id
        
        // Determine Title
        viewModelScope.launch {
            if (id.startsWith("admin_ref_")) {
                val refId = id.removePrefix("admin_ref_")
                val profile = profileRepository.getProfile(refId)
                _uiState.update { it.copy(channelTitle = "Chat: ${profile?.name ?: "Referee"}") }
            } else {
                val game = gameRepository.getGame(id)
                _uiState.update { it.copy(channelTitle = "Crew: ${game?.homeTeamName} v ${game?.awayTeamName}") }
            }
        }
        
        loadMessages()
    }

    private fun loadMessages() {
        val uid = authRepository.currentUser?.uid ?: return
        _uiState.update { it.copy(currentUserId = uid) }

        chatRepository.getMessages(channelId)
            .onEach { messages ->
                _uiState.update { it.copy(messages = messages, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        val uid = authRepository.currentUser?.uid ?: return
        
        viewModelScope.launch {
            val profile = profileRepository.getProfile(uid)
            val message = ChatMessage(
                channelId = channelId,
                senderId = uid,
                senderName = profile?.name ?: "Referee",
                content = content
            )
            chatRepository.sendMessage(message)
        }
    }
}
