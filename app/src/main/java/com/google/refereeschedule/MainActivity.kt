package com.google.refereeschedule

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.google.refereeschedule.domain.model.UserRole
import com.google.refereeschedule.navigation.*
import com.google.refereeschedule.ui.inbox.InboxScreen
import com.google.refereeschedule.ui.inbox.InboxViewModel
import com.google.refereeschedule.ui.admin.messaging.MessagingHubScreen
import com.google.refereeschedule.ui.admin.messaging.MessagingHubViewModel
import com.google.refereeschedule.ui.admin.AdminDashboardViewModel
import com.google.refereeschedule.ui.admin.system.PrintTemplateViewModel
import com.google.refereeschedule.ui.MainViewModel
import com.google.refereeschedule.ui.admin.AdminDashboardScreen
import com.google.refereeschedule.ui.admin.print.PrintManagementScreen
import com.google.refereeschedule.ui.admin.profile.AdminProfileScreen
import com.google.refereeschedule.ui.admin.scheduler.GameSchedulerScreen
import com.google.refereeschedule.ui.admin.scheduler.ScheduleImportScreen
import com.google.refereeschedule.ui.admin.system.PrintTemplateCanvasScreen
import com.google.refereeschedule.ui.admin.system.PrintTemplateManagementScreen
import com.google.refereeschedule.ui.admin.system.SystemAdminDashboardScreen
import com.google.refereeschedule.ui.admin.system.UserManagementScreen
import com.google.refereeschedule.ui.admin.teams.TeamManagementScreen
import com.google.refereeschedule.ui.auth.LoginScreen
import com.google.refereeschedule.ui.auth.SignUpScreen
import com.google.refereeschedule.ui.components.AppDrawer
import com.google.refereeschedule.ui.dashboard.DashboardScreen
import com.google.refereeschedule.ui.onboarding.OnboardingScreen
import com.google.refereeschedule.ui.quiz.RefresherQuizScreen
import com.google.refereeschedule.ui.profile.ProfileScreen
import com.google.refereeschedule.ui.report.MatchChatScreen
import com.google.refereeschedule.ui.report.MatchReportScreen
import com.google.refereeschedule.ui.resources.ResourcesScreen
import com.google.refereeschedule.ui.scheduler.SchedulerScreen
import com.google.refereeschedule.ui.theme.MyApplicationTheme
import com.google.refereeschedule.util.NetworkMonitor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val mainUiState by mainViewModel.uiState.collectAsState()
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            // Request Notification Permission for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { _ ->
                    // Handle result if needed
                }
                LaunchedEffect(Unit) {
                    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            MyApplicationTheme(
                dynamicColor = false,
                orgThemeColor = mainUiState.themeColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val backStack = rememberNavBackStack(Login)
                    val currentRoute = backStack.lastOrNull()
                    val isOnline by networkMonitor.isOnline.collectAsState(initial = true)
                    val snackbarHostState = remember { SnackbarHostState() }
                    val showNav = currentRoute !in listOf(Login, SignUp, Onboarding, null)

                    LaunchedEffect(mainUiState.isLoggedIn, mainUiState.isOnboarded, mainUiState.isLocked, mainUiState.hasPassedCurrentQuiz, currentRoute) {
                        if (mainUiState.isLoggedIn) {
                            if (!mainUiState.isOnboarded && currentRoute != Onboarding) {
                                backStack.add(Onboarding)
                                while (backStack.size > 1) backStack.removeAt(0)
                            } else if (mainUiState.isLocked && currentRoute != Onboarding) {
                                backStack.add(Onboarding)
                                while (backStack.size > 1) backStack.removeAt(0)
                            } else if (mainUiState.isOnboarded && !mainUiState.hasPassedCurrentQuiz && currentRoute !is Quiz && currentRoute != Profile) {
                                backStack.add(Quiz(mainUiState.currentSeasonId))
                                while (backStack.size > 1) backStack.removeAt(0)
                            }
                        }
                    }

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = showNav,
                        drawerContent = {
                            AppDrawer(
                                userRole = mainUiState.userRole,
                                activeTier = mainUiState.activeTier,
                                isSubscriptionExpired = mainUiState.isSubscriptionExpired,
                                onNavigateToAdmin = { 
                                    if (mainUiState.hasPassedCurrentQuiz) {
                                        backStack.add(AdminDashboard(0))
                                    } else {
                                        backStack.add(Quiz(mainUiState.currentSeasonId))
                                    }
                                    while (backStack.size > 1) backStack.removeAt(0)
                                },
                                onNavigateToSystemAdmin = { 
                                    if (mainUiState.hasPassedCurrentQuiz) {
                                        backStack.add(SystemAdminDashboard)
                                    } else {
                                        backStack.add(Quiz(mainUiState.currentSeasonId))
                                    }
                                    while (backStack.size > 1) backStack.removeAt(0)
                                },
                                onNavigateToUserManagement = { 
                                    if (mainUiState.hasPassedCurrentQuiz) {
                                        backStack.add(UserManagement)
                                    } else {
                                        backStack.add(Quiz(mainUiState.currentSeasonId))
                                    }
                                    while (backStack.size > 1) backStack.removeAt(0)
                                },
                                onNavigateToPrintTemplates = {
                                    if (mainUiState.hasPassedCurrentQuiz) {
                                        backStack.add(PrintTemplateManagement)
                                    } else {
                                        backStack.add(Quiz(mainUiState.currentSeasonId))
                                    }
                                    while (backStack.size > 1) backStack.removeAt(0)
                                },
                                onNavigateToOrgSetup = {
                                    if (mainUiState.hasPassedCurrentQuiz) {
                                        backStack.add(AdminDashboard(1))
                                    } else {
                                        backStack.add(Quiz(mainUiState.currentSeasonId))
                                    }
                                    while (backStack.size > 1) backStack.removeAt(0)
                                },
                                onNavigateToRefSetup = {
                                    if (mainUiState.hasPassedCurrentQuiz) {
                                        backStack.add(AdminDashboard(2))
                                    } else {
                                        backStack.add(Quiz(mainUiState.currentSeasonId))
                                    }
                                    while (backStack.size > 1) backStack.removeAt(0)
                                },
                                onNavigateToMatchSetup = {
                                    if (mainUiState.hasPassedCurrentQuiz) {
                                        backStack.add(AdminDashboard(3))
                                    } else {
                                        backStack.add(Quiz(mainUiState.currentSeasonId))
                                    }
                                    while (backStack.size > 1) backStack.removeAt(0)
                                },
                                onNavigateToPrintSetup = {
                                    if (mainUiState.hasPassedCurrentQuiz) {
                                        backStack.add(AdminDashboard(4))
                                    } else {
                                        backStack.add(Quiz(mainUiState.currentSeasonId))
                                    }
                                    while (backStack.size > 1) backStack.removeAt(0)
                                },
                                onNavigateToStandings = {
                                    if (mainUiState.hasPassedCurrentQuiz) {
                                        backStack.add(AdminDashboard(5))
                                    } else {
                                        backStack.add(Quiz(mainUiState.currentSeasonId))
                                    }
                                    while (backStack.size > 1) backStack.removeAt(0)
                                },
                                onNavigateToInbox = {
                                    backStack.add(Inbox)
                                    // Don't clear backstack for Inbox, allow coming back
                                },
                                onNavigateToMessagingHub = {
                                    backStack.add(MessagingHub)
                                },
                                onNavigateToProfile = {
                                    backStack.add(Profile)
                                    while (backStack.size > 1) backStack.removeAt(0)
                                },
                                onCloseDrawer = { scope.launch { drawerState.close() } }
                            )
                        }
                    )
{
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            topBar = {
                                if (!isOnline && showNav) {
                                    Surface(color = MaterialTheme.colorScheme.errorContainer) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.WifiOff, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Offline Mode", color = MaterialTheme.colorScheme.onErrorContainer)
                                        }
                                    }
                                }
                            },
                            bottomBar = {
                                if (showNav) {
                                    NavigationBar {
                                        if (mainUiState.userRole == UserRole.Admin || mainUiState.userRole == UserRole.SystemAdmin || mainUiState.userRole == UserRole.CoachAdmin) {
                                            NavigationBarItem(
                                                selected = false,
                                                onClick = { scope.launch { drawerState.open() } },
                                                icon = { Icon(Icons.Rounded.Menu, contentDescription = "Menu") },
                                                label = { Text("Menu") }
                                            )
                                        }

                                        NavigationBarItem(
                                            selected = (mainUiState.userRole == UserRole.SystemAdmin && currentRoute == SystemAdminDashboard) || 
                                                       (mainUiState.userRole == UserRole.Admin && currentRoute is AdminDashboard) ||
                                                       (mainUiState.userRole == UserRole.Referee && currentRoute == Dashboard),
                                            onClick = {
                                                when (mainUiState.userRole) {
                                                    UserRole.SystemAdmin -> {
                                                        backStack.add(SystemAdminDashboard)
                                                    }
                                                    UserRole.Admin -> {
                                                        backStack.add(AdminDashboard(0))
                                                    }
                                                    else -> {
                                                        if (mainUiState.hasPassedCurrentQuiz) {
                                                            backStack.add(Dashboard)
                                                        } else {
                                                            backStack.add(Quiz(mainUiState.currentSeasonId))
                                                        }
                                                    }
                                                }
                                                while (backStack.size > 1) backStack.removeAt(0)
                                            },
                                            icon = { Icon(Icons.Rounded.Dashboard, contentDescription = "Dashboard") },
                                            label = { Text("Dashboard") }
                                        )

                                        if (mainUiState.userRole != UserRole.SystemAdmin) {
                                            NavigationBarItem(
                                                selected = currentRoute == Scheduler,
                                                onClick = {
                                                    if (mainUiState.hasPassedCurrentQuiz) {
                                                        backStack.add(Scheduler)
                                                    } else {
                                                        backStack.add(Quiz(mainUiState.currentSeasonId))
                                                    }
                                                    while (backStack.size > 1) backStack.removeAt(0)
                                                },
                                                icon = {
                                                    Icon(
                                                        Icons.Rounded.CalendarMonth,
                                                        contentDescription = "Schedule"
                                                    )
                                                },
                                                label = { Text("Schedule") }
                                            )
                                        }

                                        NavigationBarItem(
                                            selected = currentRoute == Profile,
                                            onClick = {
                                                backStack.add(Profile)
                                                while (backStack.size > 1) backStack.removeAt(0)
                                            },
                                            icon = { Icon(Icons.Rounded.Person, contentDescription = "Profile") },
                                            label = { Text("Profile") }
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            NavDisplay(
                                backStack = backStack,
                                onBack = { 
                                    if (backStack.size > 1) {
                                        backStack.removeLastOrNull()
                                    }
                                },
                                modifier = Modifier.padding(innerPadding),
                                entryDecorators = listOf(
                                    rememberSaveableStateHolderNavEntryDecorator(),
                                    rememberViewModelStoreNavEntryDecorator()
                                ),
                                entryProvider = entryProvider {
                                    entry<Login> {
                                    LoginScreen(
                                        viewModel = hiltViewModel(),
                                        onNavigateToSignUp = { backStack.add(SignUp) },
                                        onLoginSuccess = {
                                            if (mainUiState.userRole == UserRole.SystemAdmin) {
                                                backStack.add(SystemAdminDashboard)
                                            } else if (mainUiState.userRole == UserRole.Admin) {
                                                backStack.add(AdminDashboard())
                                            } else {
                                                backStack.add(Dashboard)
                                            }
                                            while (backStack.size > 1) backStack.removeAt(0)
                                        }
                                    )
                                }
                                    entry<SignUp> {
                                    SignUpScreen(
                                        viewModel = hiltViewModel(),
                                        onNavigateBack = { backStack.removeLastOrNull() },
                                        onSignUpSuccess = {
                                            backStack.add(Dashboard)
                                            while (backStack.size > 1) backStack.removeAt(0)
                                        }
                                    )
                                }
                                    entry<Dashboard> {
                                        DashboardScreen(
                                            viewModel = hiltViewModel(),
                                            onLogout = {
                                                backStack.add(Login)
                                                while (backStack.size > 1) backStack.removeAt(0)
                                            },
                                            onNavigateToReport = { gameId -> backStack.add(MatchReport(gameId)) },
                                            onNavigateToChat = { gameId -> backStack.add(MatchChat(gameId)) }
                                        )
                                    }
                                    entry<Scheduler> {
                                        SchedulerScreen(
                                            viewModel = hiltViewModel(),
                                            claimViewModel = hiltViewModel(),
                                            userRole = mainUiState.userRole,
                                            activeTier = mainUiState.activeTier,
                                            onNavigateToChat = { gameId -> backStack.add(MatchChat(gameId)) }
                                        )
                                    }
                                    entry<Laws> {
                                        ResourcesScreen(
                                            onNavigateBack = { backStack.removeLastOrNull() }
                                        )
                                    }
                                    entry<Profile> {
                                        ProfileScreen(
                                            viewModel = hiltViewModel()
                                        )
                                    }
                                    entry<MatchReport> { key ->
                                        MatchReportScreen(
                                            gameId = key.gameId,
                                            viewModel = hiltViewModel(),
                                            onNavigateBack = { backStack.removeLastOrNull() }
                                        )
                                    }
                                    entry<MatchChat> { key ->
                                        MatchChatScreen(
                                            channelId = key.gameId,
                                            viewModel = hiltViewModel(),
                                            onNavigateBack = { backStack.removeLastOrNull() }
                                        )
                                    }
                                    entry<AdminDashboard> { key ->
                                        val adminViewModel: AdminDashboardViewModel = hiltViewModel()
                                        val printViewModel: PrintTemplateViewModel = hiltViewModel()
                                        
                                        AdminDashboardScreen(
                                            viewModel = adminViewModel,
                                            initialView = key.initialView,
                                            onNavigateBack = { backStack.removeLastOrNull() },
                                            onNavigateToProfile = { orgId -> backStack.add(AdminProfile(orgId)) },
                                            onNavigateToPrintQueue = { backStack.add(PrintManagement) },
                                            onEditReport = { gameId -> backStack.add(MatchReport(gameId)) },
                                            onNavigateToChat = { gameId -> backStack.add(MatchChat(gameId)) },
                                            onChatWithReferee = { refId -> backStack.add(MatchChat("admin_ref_$refId")) },
                                            onOpenDrawer = { scope.launch { drawerState.open() } },
                                            onNavigateToCanvas = { type ->
                                                val templates = printViewModel.uiState.value.templates
                                                val templateId = templates.find { it.type == type }?.id
                                                val isReadOnly = mainUiState.userRole == UserRole.Admin
                                                backStack.add(PrintTemplateCanvas(templateId, isReadOnly))
                                            },
                                            onNavigateToQuizBank = { backStack.add(QuizManagement) },
                                            onNavigateToScheduler = { backStack.add(GameScheduler) },
                                            onNavigateToDivisions = { backStack.add(DivisionManagement) },
                                            onNavigateToInbox = { backStack.add(Inbox) },
                                            onNavigateToMessagingHub = { backStack.add(MessagingHub) }
                                        )
                                    }
                                    entry<AdminProfile> { key ->
                                        AdminProfileScreen(
                                            organizationId = key.organizationId,
                                            viewModel = hiltViewModel(),
                                            onNavigateBack = { backStack.removeLastOrNull() }
                                        )
                                    }
                                    entry<SystemAdminDashboard> {
                                        SystemAdminDashboardScreen(
                                            viewModel = hiltViewModel(),
                                            onNavigateToUserManagement = { backStack.add(UserManagement) },
                                            onNavigateBack = { backStack.removeLastOrNull() }
                                        )
                                    }
                                    entry<UserManagement> {
                                        UserManagementScreen(
                                            viewModel = hiltViewModel(),
                                            onNavigateBack = { backStack.removeLastOrNull() }
                                        )
                                    }
                                    entry<PrintTemplateManagement> {
                                        PrintTemplateManagementScreen(
                                            viewModel = hiltViewModel(),
                                            onNavigateToCanvas = { id -> backStack.add(PrintTemplateCanvas(id)) },
                                            onNavigateBack = { backStack.removeLastOrNull() }
                                        )
                                    }
                                    entry<PrintTemplateCanvas> { key ->
                                        PrintTemplateCanvasScreen(
                                            templateId = key.templateId,
                                            isReadOnly = key.isReadOnly,
                                            viewModel = hiltViewModel(),
                                            onNavigateBack = { backStack.removeLastOrNull() }
                                        )
                                    }
                                    entry<GameScheduler> {
                                        GameSchedulerScreen(
                                            viewModel = hiltViewModel(),
                                            onNavigateToImport = { backStack.add(ScheduleImport) },
                                            onNavigateBack = { backStack.removeLastOrNull() }
                                        )
                                    }
                                    entry<ScheduleImport> {
                                        ScheduleImportScreen(
                                            viewModel = hiltViewModel(),
                                            onNavigateBack = { backStack.removeLastOrNull() }
                                        )
                                    }
                                    entry<TeamManagement> {
                                        TeamManagementScreen(
                                            viewModel = hiltViewModel(),
                                            onNavigateBack = { backStack.removeLastOrNull() }
                                        )
                                    }
                                    entry<DivisionManagement> {
                                        com.google.refereeschedule.ui.admin.divisions.DivisionManagementScreen(
                                            viewModel = hiltViewModel(),
                                            onNavigateBack = { backStack.removeLastOrNull() }
                                        )
                                    }
                                    entry<RefereePoints> {
                                        com.google.refereeschedule.ui.points.RefereePointsScreen(
                                            viewModel = hiltViewModel()
                                        )
                                    }
                                    entry<Onboarding> {
                                        OnboardingScreen(
                                            viewModel = hiltViewModel(),
                                            onFinish = {
                                                backStack.add(Dashboard)
                                                while (backStack.size > 1) backStack.removeAt(0)
                                            }
                                        )
                                    }
                                    entry<Quiz> { key ->
                                        RefresherQuizScreen(
                                            seasonId = key.seasonId,
                                            viewModel = hiltViewModel(),
                                            onFinish = {
                                                backStack.add(Dashboard)
                                                while (backStack.size > 1) backStack.removeAt(0)
                                            }
                                        )
                                    }
                                    entry<QuizManagement> {
                                        com.google.refereeschedule.ui.admin.quiz.QuizManagementScreen(
                                            viewModel = hiltViewModel(),
                                            onNavigateBack = { backStack.removeLastOrNull() }
                                        )
                                    }
                                    entry<PrintManagement> {
                                        PrintManagementScreen(
                                            viewModel = hiltViewModel(),
                                            onNavigateBack = { backStack.removeLastOrNull() }
                                        )
                                    }
                                    entry<Inbox> {
                                        InboxScreen(
                                            viewModel = hiltViewModel(),
                                            onReplyToAdmin = { adminId -> backStack.add(MatchChat("admin_ref_$adminId")) },
                                            onNavigateBack = { backStack.removeLastOrNull() }
                                        )
                                    }
                                    entry<MessagingHub> {
                                        MessagingHubScreen(
                                            viewModel = hiltViewModel(),
                                            onNavigateBack = { backStack.removeLastOrNull() }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
