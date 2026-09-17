package com.google.refereeschedule.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.refereeschedule.domain.model.Assignment
import com.google.refereeschedule.domain.model.AssignmentPosition
import com.google.refereeschedule.domain.model.Game
import com.google.refereeschedule.domain.model.PrintJobType
import com.google.refereeschedule.domain.model.UserRole
import com.google.refereeschedule.ui.theme.MyApplicationTheme
import com.google.refereeschedule.util.TimeUtils
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onLogout: () -> Unit,
    onNavigateToReport: (String) -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Dashboard")
                        IconButton(onClick = { viewModel.openPrintDialog() }) {
                            Icon(
                                Icons.Rounded.Print,
                                contentDescription = "Print Options",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
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
        if (uiState.showPrintDialog) {
            PrintSyncDialog(
                viewModel = viewModel,
                uiState = uiState,
                onDismiss = { viewModel.closePrintDialog() }
            )
        }
        
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.upcomingAssignments.isEmpty() && uiState.userName.isNullOrBlank()) {
            // Likely a new user or missing profile info
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Welcome to Referee Schedule!", style = MaterialTheme.typography.headlineMedium)
                    Text("To get started, please complete your profile by selecting your Organization and Season.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    Button(onClick = { /* Navigation handled by bottom bar */ }) {
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
                    uiState.organization?.logoUrl?.let { logo ->
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = logo,
                                contentDescription = "League Logo",
                                modifier = Modifier.height(80.dp).fillMaxWidth(0.6f),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    
                    val displayName = if (!uiState.userName.isNullOrBlank()) {
                        uiState.userName
                    } else {
                        viewModel.currentUserEmail?.substringBefore("@") ?: "Referee"
                    }
                    Text(text = "Welcome, $displayName!", style = MaterialTheme.typography.headlineSmall)
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Button(
                        onClick = { onNavigateToChat("admin_ref_${uiState.currentUserId}") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Chat with Referee Admin")
                    }
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
                        items(uiState.upcomingAssignments) { detail ->
                            AssignmentCard(
                                assignment = detail.myAssignment,
                                game = detail.game,
                                division = detail.division,
                                crew = detail.crew,
                                timeZone = uiState.organization?.timeZone ?: "UTC",
                                onCheckIn = { viewModel.checkIn(detail.myAssignment) },
                                onReport = { onNavigateToReport(detail.game.id) },
                                onChat = { onNavigateToChat(detail.game.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrintSyncDialog(
    viewModel: DashboardViewModel,
    uiState: DashboardUiState,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    
    val selectedVoucherIds = remember { mutableStateListOf<String>() }
    var printSchedule by remember { mutableStateOf(false) }
    var printReports by remember { mutableStateOf(false) }
    var reprintAllReports by remember { mutableStateOf(false) }

    val isHeadRef = uiState.upcomingAssignments.any { it.myAssignment.position == AssignmentPosition.HeadReferee }
    
    val hasPrintedReportsBefore = remember(uiState.printHistory) {
        uiState.printHistory.any { it.type == PrintJobType.MatchReport.name }
    }

    // Phase control
    var phase by remember { mutableIntStateOf(0) }
    
    // Once VM verifies, move to selection phase
    LaunchedEffect(uiState.isGameDayVerified) {
        if (uiState.isGameDayVerified) {
            phase = 1
        }
    }

    // Close when print is finished
    LaunchedEffect(Unit) {
        viewModel.printFinishedEvent.collect {
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!uiState.isPrinting) onDismiss() },
        title = { 
            Text(if (phase == 0) "Sync with Game Day" else "Select Items to Print") 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (phase == 0) {
                    Text("Enter the 6-digit code shown on the Game Day Scheduler to proceed.", style = MaterialTheme.typography.bodySmall)
                    
                    OutlinedTextField(
                        value = code,
                        onValueChange = { if (it.length <= 6) code = it },
                        label = { Text("6-Digit Code") },
                        placeholder = { Text("000000") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    if (uiState.verificationError != null) {
                        Text(
                            text = uiState.verificationError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else {
                    Text("Choose which items to print to the league's thermal printer.", style = MaterialTheme.typography.bodySmall)
                    
                    // 1. LUNCH VOUCHERS
                    if (uiState.eligibleVouchers.isNotEmpty()) {
                        Text("Eligible Lunch Vouchers:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        uiState.eligibleVouchers.forEach { voucher ->
                            PrintOptionRow(
                                label = voucher.label,
                                selected = selectedVoucherIds.contains(voucher.id),
                                onToggle = {
                                    if (selectedVoucherIds.contains(voucher.id)) selectedVoucherIds.remove(voucher.id)
                                    else selectedVoucherIds.add(voucher.id)
                                }
                            )
                        }
                    } else {
                        Text("No lunch vouchers available yet. (Requirements not met)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }

                    HorizontalDivider()

                    // 2. MATCH SCHEDULE
                    PrintOptionRow(
                        label = "My Personal Schedule",
                        selected = printSchedule,
                        onToggle = { printSchedule = !printSchedule }
                    )

                    // 3. MATCH REPORTS
                    if (isHeadRef) {
                        HorizontalDivider()
                        PrintOptionRow(
                            label = "Official Match Reports",
                            selected = printReports,
                            onToggle = { printReports = !printReports }
                        )
                        
                        if (printReports && hasPrintedReportsBefore) {
                            Column(modifier = Modifier.padding(start = 32.dp)) {
                                Text("Previous reports found. How to proceed?", style = MaterialTheme.typography.labelSmall)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = reprintAllReports, onCheckedChange = { reprintAllReports = it })
                                    Text("Reprint all assigned games", style = MaterialTheme.typography.bodySmall)
                                }
                                if (!reprintAllReports) {
                                    Text("(Only new games will be printed)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                    
                    if (uiState.verificationError != null) {
                        Text(
                            text = uiState.verificationError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                
                if (uiState.isPrinting) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Sending to Cloud...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            if (phase == 0) {
                Button(
                    onClick = { viewModel.verifyGameDayCode(code) },
                    enabled = code.length == 6 && !uiState.isPrinting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Verify & Continue")
                }
            } else {
                val canPrint = selectedVoucherIds.isNotEmpty() || printSchedule || printReports
                Button(
                    onClick = { 
                        viewModel.submitPrintRequest(
                            selectedVoucherIds = selectedVoucherIds.toSet(),
                            printSchedule = printSchedule,
                            printReports = printReports,
                            reprintAllReports = reprintAllReports
                        )
                    },
                    enabled = canPrint && !uiState.isPrinting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Print Selected")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !uiState.isPrinting) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PrintOptionRow(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onToggle() }
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { onToggle() },
            enabled = enabled
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }
}

@Composable
fun AssignmentCard(
    assignment: Assignment,
    game: Game,
    division: com.google.refereeschedule.domain.model.Division?,
    crew: List<CrewMemberDetail> = emptyList(),
    timeZone: String = "UTC",
    onCheckIn: () -> Unit,
    onReport: () -> Unit,
    onChat: () -> Unit
) {
    val tz = remember(timeZone) { TimeZone.getTimeZone(timeZone) }
    val now = Calendar.getInstance(tz).time
    val gameStartTime = game.date ?: Date()
    
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
            Text("Time: ${TimeUtils.formatTo12h(game.time)} @ ${game.location}")

            if (crew.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("Match Crew:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                crew.forEach { memberDetail ->
                    val isMe = memberDetail.assignment.refereeId == assignment.refereeId
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "• ${memberDetail.assignment.position}: ${memberDetail.name}${if (isMe) " (Me)" else ""}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (memberDetail.isYouth) {
                            Spacer(Modifier.width(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = "YOUTH",
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
            
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

                OutlinedButton(onClick = onChat) {
                    Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Chat with Crew")
                }
            }
            
            if (game.status == com.google.refereeschedule.domain.model.GameStatus.PendingReview) {
                Text("Status: Report Pending Review", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall)
            } else if (assignment.checkedIn) {
                Text("Status: Checked In", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            }
            
            OutlinedButton(onClick = onChat, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Chat with Crew")
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
            crew = listOf(
                CrewMemberDetail(
                    assignment = Assignment(refereeId = "ref1", position = AssignmentPosition.HeadReferee),
                    name = "John Doe",
                    isYouth = false
                ),
                CrewMemberDetail(
                    assignment = Assignment(refereeId = "ref2", position = AssignmentPosition.AssistantReferee),
                    name = "Jane Smith",
                    isYouth = true
                )
            ),
            timeZone = "America/Los_Angeles",
            onCheckIn = {},
            onReport = {},
            onChat = {}
        )
    }
}
