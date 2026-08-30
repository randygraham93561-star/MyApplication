package com.google.refereeschedule.ui.report

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.model.Assignment
import com.google.refereeschedule.domain.model.AssignmentPosition
import com.google.refereeschedule.domain.model.Game
import com.google.refereeschedule.domain.model.GameStatus
import com.google.refereeschedule.domain.model.RefereeVerification
import com.google.refereeschedule.domain.model.User
import com.google.refereeschedule.domain.repository.AssignmentRepository
import com.google.refereeschedule.domain.repository.GameRepository
import com.google.refereeschedule.domain.repository.ProfileRepository
import com.google.refereeschedule.domain.repository.TeamRepository
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MatchReportViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val assignmentRepository: AssignmentRepository,
    private val profileRepository: ProfileRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    var game by mutableStateOf<Game?>(null)
        private set

    var homeScore by mutableStateOf("")
    var awayScore by mutableStateOf("")
    var cards by mutableStateOf("")
    var notes by mutableStateOf("")
    
    var cardsShown by mutableStateOf(false)
    var yellowCards by mutableStateOf(false)
    var redCards by mutableStateOf(false)
    var disciplinaryDescription by mutableStateOf("")
    var signature by mutableStateOf("")
    
    var refereeVerifications = mutableStateMapOf<String, RefereeVerification>()
    
    var assignedReferees by mutableStateOf<List<Pair<Assignment, User>>>(emptyList())
    var allOrganizationReferees by mutableStateOf<List<User>>(emptyList())

    var selectedTargetTeamId by mutableStateOf("")
    var isSubmitting by mutableStateOf(false)
    var submitError by mutableStateOf<String?>(null)

    val currentUserProfile = flow {
        val uid = authRepository.currentUser?.uid ?: return@flow
        emit(profileRepository.getProfile(uid))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val myTeams = currentUserProfile.flatMapLatest { profile ->
        if (profile != null) {
            teamRepository.getTeamsForOrganizationFlow(profile.organizationId).map { allTeams ->
                allTeams.filter { it.id in profile.teamIdsForPoints }
            }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadGame(gameId: String) {
        viewModelScope.launch {
            val foundGame = gameRepository.getGame(gameId)
            game = foundGame
            
            // Load assigned referees
            if (foundGame != null) {
                val orgId = foundGame.organizationId
                allOrganizationReferees = userRepository.getUsersForOrganizationFlow(orgId).first()

                val assignments = assignmentRepository.getAssignmentsForGame(gameId).first()
                val list = mutableListOf<Pair<Assignment, User>>()
                assignments.forEach { assignment ->
                    val user = userRepository.getUser(assignment.refereeId)
                    if (user != null) {
                        list.add(assignment to user)
                        // Pre-fill verification
                        // Default to Present for testing/ease, reporter can uncheck if they missed it
                        refereeVerifications[assignment.refereeId] = RefereeVerification(
                            refereeId = assignment.refereeId,
                            isPresent = true, // Default to true so points flow by default
                            correctRole = true,
                            actualRefereeId = assignment.refereeId,
                            actualRole = assignment.position
                        )
                    }
                }
                assignedReferees = list
            }
        }
    }

    fun updateVerification(
        assignedRefereeId: String,
        isPresent: Boolean,
        correctRole: Boolean,
        actualRefereeId: String?,
        actualRole: AssignmentPosition? = null
    ) {
        refereeVerifications[assignedRefereeId] = RefereeVerification(
            refereeId = assignedRefereeId,
            isPresent = isPresent,
            correctRole = correctRole,
            actualRefereeId = actualRefereeId,
            actualRole = actualRole
        )
    }

    fun submitReport(onSuccess: () -> Unit) {
        val currentGame = game ?: return
        val myUid = authRepository.currentUser?.uid ?: return

        if (cardsShown && (disciplinaryDescription.isBlank() || signature.isBlank())) {
            submitError = "Please provide an incident description and signature."
            return
        }

        isSubmitting = true
        submitError = null

        viewModelScope.launch {
            try {
                val cardTypesList = mutableListOf<String>()
                if (yellowCards) cardTypesList.add("Yellow")
                if (redCards) cardTypesList.add("Red")

                val updatedGame = currentGame.copy(
                    status = GameStatus.PendingReview,
                    homeScore = homeScore.toIntOrNull(),
                    awayScore = awayScore.toIntOrNull(),
                    cards = cards,
                    notes = notes,
                    cardsShown = cardsShown,
                    cardTypes = cardTypesList.joinToString(", "),
                    disciplinaryDescription = disciplinaryDescription,
                    reporterSignature = signature,
                    selectedTargetTeamId = selectedTargetTeamId,
                    refereeVerifications = refereeVerifications.values.toList(),
                    reportSubmittedAt = Date(),
                    reportSubmittedBy = myUid
                )
                gameRepository.saveGame(updatedGame)

                // Note: Points distribution is moved to Admin Approval

                onSuccess()
            } catch (e: Exception) {
                submitError = "Failed to submit report: ${e.message}"
            } finally {
                isSubmitting = false
            }
        }
    }
}
