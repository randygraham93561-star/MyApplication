package com.google.refereeschedule.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.model.SubscriptionTier
import com.google.refereeschedule.domain.model.UserRole
import com.google.refereeschedule.domain.repository.ProfileRepository
import com.google.refereeschedule.domain.repository.QuizRepository
import com.google.refereeschedule.domain.repository.UserRepository
import com.google.firebase.messaging.FirebaseMessaging
import com.google.refereeschedule.domain.repository.OrganizationRepository
import com.google.refereeschedule.domain.repository.SeasonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class MainUiState(
    val userRole: UserRole = UserRole.Referee,
    val isLoggedIn: Boolean = false,
    val isOnboarded: Boolean = true,
    val isNewSeason: Boolean = false,
    val isLocked: Boolean = false,
    val currentSeasonId: String = "",
    val hasPassedCurrentQuiz: Boolean = true,
    val activeTier: SubscriptionTier = SubscriptionTier.Free,
    val isSubscriptionExpired: Boolean = false,
    val themeColor: String = "Default"
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository,
    private val quizRepository: QuizRepository,
    private val organizationRepository: OrganizationRepository,
    private val seasonRepository: SeasonRepository
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = authRepository.currentUserFlow
        .flatMapLatest { firebaseUser ->
            if (firebaseUser == null) {
                flowOf(MainUiState(isLoggedIn = false))
            } else {
                // Update FCM Token
                updateFcmToken(firebaseUser.uid)

                combine(
                    userRepository.getUserFlow(firebaseUser.uid),
                    profileRepository.getProfileFlow(firebaseUser.uid),
                    organizationRepository.getOrganizationsFlow(),
                    seasonRepository.getSeasonsFlow(),
                    quizRepository.getAllQuizzesFlow()
                ) { user, profile, allOrgs, allSeasons, allQuizzes ->
                    val orgId = user?.organizationId ?: profile?.organizationId ?: ""
                    val organization = allOrgs.find { it.id == orgId }
                    
                    val orgSeasons = allSeasons.filter { it.organizationId == orgId }
                    val activeSeason = orgSeasons.find { s -> s.active }
                    
                    val currentSeasonId = activeSeason?.id ?: profile?.currentSeasonId ?: ""
                    
                    val isOnboardingCompleted = profile?.isOnboardingCompleted ?: false
                    val lastOnboardedSeasonId = profile?.lastOnboardedSeasonId ?: ""
                    
                    // New Season logic: If they finished onboarding before, but not for THIS season
                    val isNewSeason = isOnboardingCompleted && 
                                     activeSeason != null && 
                                     lastOnboardedSeasonId != activeSeason.id
                    
                    // Quiz logic:
                    // 1. If season is empty, no quiz needed.
                    // 2. New referees (First season) do not take the quiz.
                    // 3. Only returning (Experienced) referees must take it.
                    // 4. Check if a quiz exists for the current season AND has questions in the pool.
                    // 5. If no quiz document exists or it has 0 questions, it's considered "passed" (skipped) for now.
                    val isNewReferee = profile?.isNewReferee ?: false
                    val quizForSeason = allQuizzes.find { it.seasonId == currentSeasonId && it.isActive }
                    val quizPassed = if (isNewReferee) {
                        // New referees skip the quiz as requested
                        true
                    } else if (currentSeasonId.isNotEmpty() && quizForSeason != null && quizForSeason.questionIds.isNotEmpty()) {
                        profile?.completedSeasonQuizzes?.contains(currentSeasonId) == true
                    } else {
                        // Either no current season, no quiz set up, or pool is empty
                        true
                    }
                    
                    val isExpired = organization?.isCurrentlyExpired ?: true
                    
                    // Force roles based on email to ensure service continuity
                    val role = when (firebaseUser.email?.lowercase()?.trim()) {
                        "randygraham93561@gmail.com" -> UserRole.SystemAdmin
                        "randalltruck@gmail.com" -> UserRole.Admin
                        else -> user?.userRole ?: UserRole.Referee
                    }

                    // Check for 24h quiz lockout
                    val now = System.currentTimeMillis()
                    val lastFail = profile?.lastQuizFailTimestamp ?: 0L
                    val isQuizLocked = (now - lastFail) < (24 * 60 * 60 * 1000)
                    
                    // Determine Active Tier
                    val activeTier = if (role == UserRole.SystemAdmin) {
                        SubscriptionTier.Pro
                    } else if (isExpired) {
                        SubscriptionTier.Free
                    } else {
                        organization?.subscriptionTier ?: SubscriptionTier.Free
                    }
                    
                    val isStaff = (role == UserRole.Admin) || (role == UserRole.SystemAdmin)

                    MainUiState(
                        userRole = role,
                        isLoggedIn = true,
                        isOnboarded = if (isStaff) true else (isOnboardingCompleted && !isNewSeason),
                        isNewSeason = isNewSeason,
                        isLocked = if (isStaff) false else (profile?.isAccountLocked ?: false || isQuizLocked),
                        currentSeasonId = currentSeasonId,
                        hasPassedCurrentQuiz = if (isStaff) true else quizPassed,
                        activeTier = if (role == UserRole.SystemAdmin) SubscriptionTier.Pro else activeTier,
                        isSubscriptionExpired = isExpired,
                        themeColor = organization?.themeColor ?: "Default"
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    private fun updateFcmToken(userId: String) {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                android.util.Log.d("FCM", "My Device Token: $token")
                
                val profile = profileRepository.getProfile(userId)
                if (profile != null && profile.fcmToken != token) {
                    profileRepository.saveProfile(profile.copy(fcmToken = token))
                }
            } catch (e: Exception) {
                android.util.Log.w("FCM", "Fetching FCM registration token failed", e)
            }
        }
    }
}
