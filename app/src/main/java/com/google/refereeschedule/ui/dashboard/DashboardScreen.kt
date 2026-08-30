package com.google.refereeschedule.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.refereeschedule.domain.model.Assignment
import com.google.refereeschedule.domain.model.AssignmentPosition
import com.google.refereeschedule.domain.model.Game
import com.google.refereeschedule.domain.model.UserRole
import com.google.refereeschedule.ui.theme.MyApplicationTheme
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onLogout: () -> Unit,
    onNavigateToReport: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.signOut(onLogout) }) {
                        Text("Logout")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.upcomingAssignments.isEmpty() && uiState.userName.isNullOrBlank()) {
            // Likely a new user or missing profile info
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Welcome to Referee Schedule!", style = MaterialTheme.typography.headlineMedium)
                    Text("To get started, please complete your profile by selecting your Organization and Season.", style = MaterialTheme.typography.bodyLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Button(onClick = { /* Navigation handled by bottom bar usually, or we can add a callback */ }) {
                        Text("Go to Profile")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    val displayName = if (!uiState.userName.isNullOrBlank()) {
                        uiState.userName
                    } else {
                        viewModel.currentUserEmail?.substringBefore("@") ?: "Referee"
                    }
                    Text(text = "Welcome, $displayName!", style = MaterialTheme.typography.headlineSmall)
                }

                if (uiState.userRole != UserRole.SystemAdmin) {
                    item {
                        Text(text = "Upcoming Assignments", style = MaterialTheme.typography.titleMedium)
                    }

                    if (uiState.upcomingAssignments.isEmpty()) {
                        item {
                            Text("No upcoming assignments.")
                        }
                    } else {
                        items(uiState.upcomingAssignments) { (assignment, game, division) ->
                            AssignmentCard(
                                assignment = assignment,
                                game = game,
                                division = division,
                                onCheckIn = { viewModel.checkIn(assignment) },
                                onReport = { onNavigateToReport(game.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AssignmentCard(
    assignment: Assignment,
    game: Game,
    division: com.google.refereeschedule.domain.model.Division?,
    onCheckIn: () -> Unit,
    onReport: () -> Unit
) {
    val now = Date()
    val gameStartTime = game.date // Assuming game.date has the full time or combined
    // Check-in enabled 30 mins before
    val checkInEnabled = !assignment.checkedIn && (gameStartTime.time - now.time) <= 30 * 60 * 1000 && (gameStartTime.time - now.time) >= -2 * 60 * 60 * 1000
    
    // Report enabled after game starts and if not already submitted
    val canSubmitReport = game.status != com.google.refereeschedule.domain.model.GameStatus.Completed && 
                         game.status != com.google.refereeschedule.domain.model.GameStatus.PendingReview &&
                         now.after(gameStartTime)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${game.homeTeamName} vs ${game.awayTeamName}", style = MaterialTheme.typography.titleMedium)
            
            if (division != null) {
                Text(
                    text = "${division.name} | ${division.playersPerTeam}v${division.playersPerTeam} | Half: ${division.halfDurationMinutes}m | Ball: ${division.ballSize}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                Text("Division: ${game.divisionName}", style = MaterialTheme.typography.bodySmall)
            }

            Text("Role: ${assignment.position}")
            Text("Time: ${game.time} @ ${game.location}")
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onCheckIn,
                    enabled = checkInEnabled
                ) {
                    Text(if (assignment.checkedIn) "Checked In" else "Check-in")
                }
                
                if (canSubmitReport) {
                    Button(onClick = onReport) {
                        Text("Submit Report")
                    }
                }
            }
            
            if (game.status == com.google.refereeschedule.domain.model.GameStatus.PendingReview) {
                Text("Status: Report Pending Review", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall)
            } else if (assignment.checkedIn) {
                Text("Status: Checked In", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AssignmentCardPreview() {
    MyApplicationTheme {
        AssignmentCard(
            assignment = Assignment(
                id = "1",
                gameId = "game1",
                refereeId = "ref1",
                position = AssignmentPosition.HeadReferee,
                checkedIn = false
            ),
            game = Game(
                id = "game1",
                homeTeamName = "Tigers",
                awayTeamName = "Lions",
                location = "Central Park Field 1",
                time = "10:00 AM",
                date = Date()
            ),
            division = com.google.refereeschedule.domain.model.Division(
                name = "12U Boys",
                playersPerTeam = 11,
                halfDurationMinutes = 35,
                ballSize = 4
            ),
            onCheckIn = {},
            onReport = {}
        )
    }
}
