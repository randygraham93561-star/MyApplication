package com.google.refereeschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.google.refereeschedule.ui.MainViewModel
import com.google.refereeschedule.ui.admin.AdminDashboardScreen
import com.google.refereeschedule.ui.admin.profile.AdminProfileScreen
import com.google.refereeschedule.ui.admin.scheduler.GameSchedulerScreen
import com.google.refereeschedule.ui.admin.system.SystemAdminDashboardScreen
import com.google.refereeschedule.ui.admin.system.UserManagementScreen
import com.google.refereeschedule.ui.admin.teams.TeamManagementScreen
import com.google.refereeschedule.ui.auth.LoginScreen
import com.google.refereeschedule.ui.auth.SignUpScreen
import com.google.refereeschedule.ui.components.AppDrawer
import com.google.refereeschedule.ui.dashboard.DashboardScreen
import com.google.refereeschedule.ui.profile.ProfileScreen
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

            MyApplicationTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val backStack = rememberNavBackStack(Login)
                    val currentRoute = backStack.lastOrNull()
                    val isOnline by networkMonitor.isOnline.collectAsState(initial = true)
                    val snackbarHostState = remember { SnackbarHostState() }
                    val showNav = currentRoute !in listOf(Login, SignUp, null)

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = showNav,
                        drawerContent = {
                            AppDrawer(
                                userRole = mainUiState.userRole,
                                onNavigateToAdmin = { 
                                    backStack.add(AdminDashboard)
                                    while (backStack.size > 1) backStack.removeAt(0)
                                },
                                onNavigateToTeams = { 
                                    backStack.add(TeamManagement)
                                    while (backStack.size > 1) backStack.removeAt(0)
                                },
                                onNavigateToDivisions = { 
                                    backStack.add(DivisionManagement)
                                    while (backStack.size > 1) backStack.removeAt(0)
                                },
                                onNavigateToRefereePoints = {
                                    backStack.add(RefereePoints)
                                    while (backStack.size > 1) backStack.removeAt(0)
                                },
                                onNavigateToGameScheduler = { 
                                    backStack.add(GameScheduler)
                                    while (backStack.size > 1) backStack.removeAt(0)
                                },
                                onNavigateToSystemAdmin = { 
                                    backStack.add(SystemAdminDashboard)
                                    while (backStack.size > 1) backStack.removeAt(0)
                                },
                                onNavigateToUserManagement = { 
                                    backStack.add(UserManagement)
                                    while (backStack.size > 1) backStack.removeAt(0)
                                },
                                onCloseDrawer = { scope.launch { drawerState.close() } }
                            )
                        }
                    ) {
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
                                        NavigationBarItem(
                                            selected = currentRoute == Dashboard,
                                            onClick = {
                                                backStack.add(Dashboard)
                                                while (backStack.size > 1) backStack.removeAt(0)
                                            },
                                            icon = { Icon(Icons.Rounded.Dashboard, contentDescription = "Dashboard") },
                                            label = { Text("Dashboard") }
                                        )

                                        NavigationBarItem(
                                            selected = currentRoute == Scheduler,
                                            onClick = {
                                                backStack.add(Scheduler)
                                                while (backStack.size > 1) backStack.removeAt(0)
                                            },
                                            icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = "Schedule") },
                                            label = { Text("Schedule") }
                                        )

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
                                            backStack.add(Dashboard)
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
                                            onOpenDrawer = { scope.launch { drawerState.open() } }
                                        )
                                    }
                                    entry<Scheduler> {
                                        SchedulerScreen(
                                            viewModel = hiltViewModel(),
                                            claimViewModel = hiltViewModel(),
                                            onOpenDrawer = { scope.launch { drawerState.open() } }
                                        )
                                    }
                                    entry<Laws> {
                                        ResourcesScreen(
                                            onNavigateBack = { backStack.removeLastOrNull() }
                                        )
                                    }
                                    entry<Profile> {
                                        ProfileScreen(
                                            viewModel = hiltViewModel(),
                                            onOpenDrawer = { scope.launch { drawerState.open() } }
                                        )
                                    }
                                    entry<MatchReport> { key ->
                                        MatchReportScreen(
                                            gameId = key.gameId,
                                            viewModel = hiltViewModel(),
                                            onNavigateBack = { backStack.removeLastOrNull() }
                                        )
                                    }
                                    entry<AdminDashboard> {
                                        AdminDashboardScreen(
                                            viewModel = hiltViewModel(),
                                            onNavigateBack = { backStack.removeLastOrNull() },
                                            onNavigateToProfile = { backStack.add(AdminProfile) }
                                        )
                                    }
                                    entry<AdminProfile> {
                                        AdminProfileScreen(
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
                                    entry<GameScheduler> {
                                        GameSchedulerScreen(
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
                                            viewModel = hiltViewModel(),
                                            onOpenDrawer = { scope.launch { drawerState.open() } }
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
