package com.google.refereeschedule.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.model.RefereeProfile
import com.google.refereeschedule.domain.model.User
import com.google.refereeschedule.domain.model.UserRole
import com.google.refereeschedule.domain.repository.ProfileRepository
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun signUp(name: String, email: String, password: String, onSuccess: () -> Unit) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        val trimmedName = name.trim()

        viewModelScope.launch {
            _uiState.value = SignUpUiState.Loading
            val result = authRepository.signUp(trimmedName, trimmedEmail, trimmedPassword)
            if (result.isSuccess) {
                val firebaseUser = result.getOrThrow()
                val role = if (trimmedEmail.lowercase() == "randygraham93561@gmail.com") {
                    UserRole.SystemAdmin
                } else {
                    UserRole.Referee
                }

                val user = User(
                    id = firebaseUser.uid,
                    email = trimmedEmail,
                    role = role
                )
                
                val profile = RefereeProfile(
                    id = firebaseUser.uid,
                    name = trimmedName,
                    badgeLevel = "Regional"
                )
                
                try {
                    userRepository.saveUser(user)
                    profileRepository.saveProfile(profile)
                    _uiState.value = SignUpUiState.Success
                    onSuccess()
                } catch (e: Exception) {
                    _uiState.value = SignUpUiState.Error("Account created in Auth, but failed to save profile to Firestore: ${e.message}")
                }
            } else {
                _uiState.value = SignUpUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }
}

sealed interface SignUpUiState {
    data object Idle : SignUpUiState
    data object Loading : SignUpUiState
    data object Success : SignUpUiState
    data class Error(val message: String) : SignUpUiState
}
