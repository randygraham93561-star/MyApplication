package com.google.refereeschedule.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.model.QuizAttempt
import com.google.refereeschedule.domain.model.QuizQuestion
import com.google.refereeschedule.domain.model.RefresherQuiz
import com.google.refereeschedule.domain.repository.ProfileRepository
import com.google.refereeschedule.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizUiState(
    val quiz: RefresherQuiz? = null,
    val questions: List<QuizQuestion> = emptyList(),
    val isLoading: Boolean = true,
    val isFinished: Boolean = false,
    val lastAttempt: QuizAttempt? = null,
    val lockoutRemainingMillis: Long = 0L
)

@HiltViewModel
class RefresherQuizViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val quizRepository: QuizRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    init {
        startLockoutTimer()
    }

    private fun startLockoutTimer() {
        viewModelScope.launch {
            while (true) {
                val profile = profileRepository.getProfile(authRepository.currentUser?.uid ?: "")
                val lastFail = profile?.lastQuizFailTimestamp ?: 0L
                val now = System.currentTimeMillis()
                val diff = now - lastFail
                val remaining = (24 * 60 * 60 * 1000) - diff
                _uiState.update { it.copy(lockoutRemainingMillis = remaining.coerceAtLeast(0)) }
                delay(1000)
            }
        }
    }

    fun loadQuiz(seasonId: String) {
        viewModelScope.launch {
            val quiz = quizRepository.getQuizForSeason(seasonId).first()
            if (quiz != null) {
                val allQuestions = quizRepository.getAllGlobalQuestions().first()
                var quizQuestions = allQuestions.filter { it.id in quiz.questionIds }
                
                // Shuffle pool and take quizLength
                if (quiz.quizLength > 0 && quizQuestions.size > quiz.quizLength) {
                    quizQuestions = quizQuestions.shuffled().take(quiz.quizLength)
                }
                
                _uiState.update { it.copy(quiz = quiz, questions = quizQuestions, isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun submitQuiz(answers: Map<String, Int>) {
        val state = _uiState.value
        val quiz = state.quiz ?: return
        val uid = authRepository.currentUser?.uid ?: return
        
        var correctCount = 0
        state.questions.forEach { q ->
            if (answers[q.id] == q.correctAnswerIndex) correctCount++
        }
        
        val percentage = (correctCount.toFloat() / state.questions.size * 100).toInt()
        val passed = percentage >= quiz.passingPercentage
        
        val attempt = QuizAttempt(
            refereeId = uid,
            seasonId = quiz.seasonId,
            score = correctCount,
            totalQuestions = state.questions.size,
            passed = passed
        )
        
        viewModelScope.launch {
            quizRepository.saveAttempt(attempt)
            val profile = profileRepository.getProfile(uid)
            if (profile != null) {
                if (passed) {
                    val updatedQuizzes = (profile.completedSeasonQuizzes + quiz.seasonId).distinct()
                    profileRepository.saveProfile(profile.copy(
                        completedSeasonQuizzes = updatedQuizzes,
                        lastQuizFailTimestamp = null // Clear any existing lockout
                    ))
                } else {
                    // Record failure timestamp for 24h lockout
                    profileRepository.saveProfile(profile.copy(
                        lastQuizFailTimestamp = System.currentTimeMillis()
                    ))
                }
            }
            _uiState.update { it.copy(isFinished = true, lastAttempt = attempt) }
        }
    }

    fun resetQuiz() {
        val currentQuiz = _uiState.value.quiz
        if (currentQuiz != null) {
            loadQuiz(currentQuiz.seasonId)
        }
        _uiState.update { it.copy(isFinished = false, lastAttempt = null) }
    }
}
