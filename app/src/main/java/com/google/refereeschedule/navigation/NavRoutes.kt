package com.google.refereeschedule.navigation

import kotlinx.serialization.Serializable
import androidx.navigation3.runtime.NavKey

@Serializable
data object Login : NavKey

@Serializable
data object SignUp : NavKey

@Serializable
data object Dashboard : NavKey

@Serializable
data object Scheduler : NavKey

@Serializable
data object Profile : NavKey

@Serializable
data class MatchReport(val gameId: String) : NavKey

@Serializable
data object AdminDashboard : NavKey

@Serializable
data object SystemAdminDashboard : NavKey

@Serializable
data object UserManagement : NavKey

@Serializable
data object AdminProfile : NavKey

@Serializable
data object TeamManagement : NavKey

@Serializable
data object DivisionManagement : NavKey

@Serializable
data object GameScheduler : NavKey

@Serializable
data object Laws : NavKey

@Serializable
data object RefereePoints : NavKey
