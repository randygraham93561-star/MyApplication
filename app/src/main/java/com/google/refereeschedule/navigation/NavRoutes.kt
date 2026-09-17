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
data class MatchChat(val gameId: String) : NavKey

@Serializable
data class AdminDashboard(val initialView: Int = 0) : NavKey

@Serializable
data object SystemAdminDashboard : NavKey

@Serializable
data object UserManagement : NavKey

@Serializable
data object PrintTemplateManagement : NavKey

@Serializable
data class PrintTemplateCanvas(val templateId: String? = null, val isReadOnly: Boolean = false) : NavKey

@Serializable
data class AdminProfile(val organizationId: String) : NavKey

@Serializable
data object TeamManagement : NavKey

@Serializable
data object DivisionManagement : NavKey

@Serializable
data object GameScheduler : NavKey

@Serializable
data object ScheduleImport : NavKey

@Serializable
data object Laws : NavKey

@Serializable
data object RefereePoints : NavKey

@Serializable
data object Onboarding : NavKey

@Serializable
data class Quiz(val seasonId: String) : NavKey

@Serializable
data object QuizManagement : NavKey

@Serializable
data object PrintManagement : NavKey

@Serializable
data object Inbox : NavKey

@Serializable
data object MessagingHub : NavKey
