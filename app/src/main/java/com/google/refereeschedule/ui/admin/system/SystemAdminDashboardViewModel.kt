package com.google.refereeschedule.ui.admin.system

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.domain.model.Organization
import com.google.refereeschedule.domain.model.User
import com.google.refereeschedule.domain.model.UserRole
import com.google.refereeschedule.domain.repository.OrganizationRepository
import com.google.refereeschedule.domain.model.RefereeProfile
import com.google.refereeschedule.domain.repository.ProfileRepository
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SystemAdminAnalytics(
    val totalUsers: Int = 0,
    val activeUsersPercentage: Float = 0f,
    val organizationUsagePercentage: Map<String, Float> = emptyMap(),
    val mostUsedFeatures: List<Pair<String, Int>> = emptyList()
)

data class SystemAdminUiState(
    val organizations: List<Organization> = emptyList(),
    val analytics: SystemAdminAnalytics = SystemAdminAnalytics(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SystemAdminDashboardViewModel @Inject constructor(
    private val organizationRepository: OrganizationRepository,
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SystemAdminUiState())
    val uiState: StateFlow<SystemAdminUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        combine(
            organizationRepository.getOrganizationsFlow(),
            userRepository.getAllUsersFlow(),
            profileRepository.getAllProfilesFlow()
        ) { orgs, users, profiles ->
            val totalUsers = users.size
            val activeReferees = profiles.count { it.isActive }
            val activePercentage = if (totalUsers > 0) (activeReferees.toFloat() / totalUsers) * 100f else 0f

            val orgUsage = if (totalUsers > 0) {
                orgs.associate { org ->
                    val userCount = users.count { it.organizationId == org.id }
                    org.name to (userCount.toFloat() / totalUsers) * 100f
                }
            } else emptyMap()

            // Simulate feature usage based on available data
            val features = listOf(
                "Match Reporting" to profiles.sumOf { it.headRefereeGamesCount + it.assistantRefereeGamesCount },
                "Quiz Refresher" to profiles.sumOf { it.completedSeasonQuizzes.size },
                "Print Vouchers" to orgs.size * 5 // Mock factor for now
            ).sortedByDescending { it.second }

            SystemAdminUiState(
                organizations = orgs,
                analytics = SystemAdminAnalytics(
                    totalUsers = totalUsers,
                    activeUsersPercentage = activePercentage,
                    organizationUsagePercentage = orgUsage,
                    mostUsedFeatures = features
                ),
                isLoading = false
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun createOrganization(
        name: String, 
        adminEmail: String, 
        tier: com.google.refereeschedule.domain.model.SubscriptionTier, 
        expiresAt: java.util.Date?,
        printerModel: String,
        printerIp: String,
        printerMac: String,
        printerSettings: Map<String, String> = emptyMap()
    ) {
        viewModelScope.launch {
            val org = Organization(
                name = name, 
                contactEmail = adminEmail,
                tier = tier.name,
                subscriptionExpiresAt = expiresAt,
                printerModel = printerModel,
                printerIp = printerIp,
                printerMacAddress = printerMac,
                printerSettings = printerSettings.ifEmpty { 
                    mapOf(
                        "paper_width" to "80mm",
                        "print_density" to "Normal",
                        "auto_cut" to "true",
                        "voucher_header" to "League Lunch Voucher"
                    )
                }
            )
            organizationRepository.saveOrganization(org)
        }
    }

    fun updateOrganization(organization: Organization) {
        viewModelScope.launch {
            organizationRepository.saveOrganization(organization)
        }
    }

    fun createAdminForOrganization(organizationId: String, email: String) {
        viewModelScope.launch {
            // Note: In a real app, we'd use Firebase Auth to create the user
            // and get a proper UID. For now, saveUser handles generating a doc ID.
            val user = User(
                email = email,
                roles = mapOf("referee" to UserRole.Admin.name),
                organizationId = organizationId
            )
            val savedUserId = userRepository.saveUserWithReturn(user)
            
            // Also create a referee profile for the admin so they can manage themselves
            if (savedUserId.isNotEmpty()) {
                val profile = RefereeProfile(
                    id = savedUserId,
                    name = email.substringBefore("@"),
                    badgeLevel = "Regional",
                    organizationId = organizationId
                )
                profileRepository.saveProfile(profile)
            }
        }
    }
}
