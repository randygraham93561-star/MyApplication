package com.google.refereeschedule.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.model.UserRole
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class MainUiState(
    val userRole: UserRole = UserRole.Referee,
    val isLoggedIn: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = authRepository.currentUserFlow
        .flatMapLatest { firebaseUser ->
            if (firebaseUser == null) {
                flowOf(MainUiState(isLoggedIn = false))
            } else {
                userRepository.getUserFlow(firebaseUser.uid).map { user ->
                    MainUiState(
                        userRole = user?.role ?: UserRole.Referee,
                        isLoggedIn = true
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())
}
