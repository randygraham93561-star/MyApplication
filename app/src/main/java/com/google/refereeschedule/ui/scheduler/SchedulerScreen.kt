package com.google.refereeschedule.ui.scheduler

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.refereeschedule.domain.model.Assignment
import com.google.refereeschedule.domain.model.Game
import com.google.refereeschedule.domain.model.GameStatus
import com.google.refereeschedule.domain.model.RefereeProfile
import com.google.refereeschedule.domain.model.User
import com.google.refereeschedule.ui.theme.MyApplicationTheme
import com.google.refereeschedule.ui.theme.StatusCompleted
import com.google.refereeschedule.ui.theme.StatusFull
import com.google.refereeschedule.ui.theme.StatusOpen
import com.google.refereeschedule.ui.theme.StatusPartiallyFilled
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SchedulerScreen(
    viewModel: SchedulerViewModel,
    claimViewModel: ClaimAssignmentViewModel,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentUserProfile by viewModel.currentUserProfile.collectAsState()
    val allAssignments by viewModel.allAssignments.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val coroutineScope = rememberCoroutineScope()
    var showClaimDialog by remember { mutableStateOf<Game?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var assignmentToDelete by remember { mutableStateOf<String?>(null) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.time
    )

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let {
            viewModel.selectDate(Date(it))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Games") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = modifier.padding(padding)) {
            // Compact Date Header
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Column {
                        Text("Selected Date", style = MaterialTheme.typography.labelSmall)
                        val dateText = remember(selectedDate) {
                            val sdf = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
                            sdf.timeZone = TimeZone.getTimeZone("UTC")
                            sdf.format(selectedDate)
                        }
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            when (val state = uiState) {
                is SchedulerUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SchedulerUiState.Success -> {
                    ListDetailPaneScaffold(
                        directive = navigator.scaffoldDirective,
                        value = navigator.scaffoldValue,
                        listPane = {
                            GameList(
                                games = state.games,
                                currentUserProfile = currentUserProfile,
                                assignments = allAssignments,
                                onGameClick = { game ->
                                    coroutineScope.launch {
                                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, game.id)
                                    }
                                }
                            )
                        },
                        detailPane = {
                            val gameId = navigator.currentDestination?.contentKey
                            val game = state.games.find { it.id == gameId }

                            if (game != null) {
                                val crew = allAssignments.filter { it.gameId == game.id }
                                GameDetail(
                                    game = game,
                                    crew = crew,
                                    allUsers = allUsers,
                                    currentUserProfile = currentUserProfile,
                                    onClaimClick = { showClaimDialog = game },
                                    onCancelAssignment = { assignmentId ->
                                        assignmentToDelete = assignmentId
                                    }
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Select a game to see details")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                is SchedulerUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (assignmentToDelete != null) {
            AlertDialog(
                onDismissRequest = { assignmentToDelete = null },
                title = { Text("Cancel Assignment") },
                text = { Text("Are you sure you want to cancel this assignment? You will no longer be officiating this game.") },
                confirmButton = {
                    TextButton(
                        onClick = { 
                            viewModel.cancelAssignment(assignmentToDelete!!)
                            assignmentToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Yes, Cancel")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { assignmentToDelete = null }) { Text("No") }
                }
            )
        }
    }

    if (showClaimDialog != null && currentUserProfile != null) {
        ClaimAssignmentDialog(
            game = showClaimDialog!!,
            referee = currentUserProfile!!,
            viewModel = claimViewModel,
            onDismiss = { showClaimDialog = null }
        )
    }
}

@Composable
fun GameList(
    games: List<Game>,
    currentUserProfile: RefereeProfile?,
    assignments: List<Assignment>,
    onGameClick: (Game) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(games) { game ->
            val isAssigned = assignments.any { it.gameId == game.id && it.refereeId == currentUserProfile?.id }
            GameItem(game = game, isAssigned = isAssigned, onClick = { onGameClick(game) })
        }
    }
}

@Composable
fun GameItem(
    game: Game,
    isAssigned: Boolean,
    onClick: () -> Unit
) {
    val statusColor = when (game.status) {
        GameStatus.Open -> StatusOpen
        GameStatus.PartiallyFilled -> StatusPartiallyFilled
        GameStatus.Full -> StatusFull
        GameStatus.PendingReview -> MaterialTheme.colorScheme.secondary
        GameStatus.ReportApproved -> MaterialTheme.colorScheme.tertiary
        GameStatus.Completed -> StatusCompleted
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${game.homeTeamName} vs ${game.awayTeamName}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (isAssigned) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = "Assigned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = game.time, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = game.location, style = MaterialTheme.typography.bodySmall)
                }
            }
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = statusColor.copy(alpha = 0.2f),
                contentColor = statusColor
            ) {
                Text(
                    text = game.status.name,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun GameDetail(
    game: Game,
    crew: List<Assignment>,
    allUsers: List<User>,
    currentUserProfile: RefereeProfile?,
    onClaimClick: () -> Unit,
    onCancelAssignment: (String) -> Unit
) {
    val myAssignment = crew.find { it.refereeId == currentUserProfile?.id }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Game Details", style = MaterialTheme.typography.headlineSmall)
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "${game.homeTeamName} vs ${game.awayTeamName}", style = MaterialTheme.typography.titleLarge)
                Text(text = game.ageGroup, style = MaterialTheme.typography.bodyMedium)
            }
        }

        InfoRow(
            icon = Icons.Rounded.LocationOn,
            label = "Location",
            value = "${game.location} - Field ${game.fieldNumber}"
        )

        InfoRow(
            icon = Icons.Rounded.Schedule,
            label = "Time",
            value = game.time
        )

        HorizontalDivider()

        Text(text = "Current Crew", style = MaterialTheme.typography.titleMedium)
        if (crew.isEmpty()) {
            Text("No referees assigned yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        } else {
            crew.forEach { assignment ->
                val user = allUsers.find { it.id == assignment.refereeId }
                CrewMemberRow(
                    email = user?.email ?: "Unknown Referee",
                    position = assignment.position.name
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (myAssignment != null) {
            Button(
                onClick = { onCancelAssignment(myAssignment.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cancel My Assignment")
            }
        } else {
            Button(
                onClick = onClaimClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = game.status != GameStatus.Full
            ) {
                Text("Claim Assignment")
            }
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun CrewMemberRow(
    email: String,
    position: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        Column {
            Text(email, style = MaterialTheme.typography.bodyLarge)
            Text(position, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Preview(showBackground = true, widthDp = 800)
@Composable
fun SchedulerPreview() {
    MyApplicationTheme {
        val games = listOf(
            Game(id = "1", homeTeamName = "Tigers", awayTeamName = "Lions", time = "10:00 AM", location = "Central Park", status = GameStatus.Open),
            Game(id = "2", homeTeamName = "Eagles", awayTeamName = "Hawks", time = "11:30 AM", location = "West Field", status = GameStatus.PartiallyFilled)
        )
        // Manual implementation of a mock state for preview
        GameList(
            games = games,
            currentUserProfile = null,
            assignments = emptyList(),
            onGameClick = {}
        )
    }
}
