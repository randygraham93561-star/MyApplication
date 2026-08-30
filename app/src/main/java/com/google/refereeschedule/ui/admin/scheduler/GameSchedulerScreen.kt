package com.google.refereeschedule.ui.admin.scheduler

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.refereeschedule.domain.model.Assignment
import com.google.refereeschedule.domain.model.AssignmentPosition
import com.google.refereeschedule.domain.model.BulkGameData
import com.google.refereeschedule.domain.model.Division
import com.google.refereeschedule.domain.model.Game
import com.google.refereeschedule.domain.model.Season
import com.google.refereeschedule.domain.model.Team
import com.google.refereeschedule.ui.admin.components.SeasonAndDivisionSelectors
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSchedulerScreen(
    viewModel: GameSchedulerViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val allAssignments by viewModel.allAssignments.collectAsState()
    var showGameDialog by remember { mutableStateOf(false) }
    var showBulkDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf<Game?>(null) }
    var editingGame by remember { mutableStateOf<Game?>(null) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.selectedDate.time
    )

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let {
            viewModel.selectDate(Date(it))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Scheduler") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.selectedDivision != null) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    FloatingActionButton(onClick = { 
                        editingGame = null
                        showGameDialog = true 
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Game")
                    }
                    FloatingActionButton(onClick = { 
                        showBulkDialog = true 
                    }) {
                        Icon(Icons.Rounded.LibraryAdd, contentDescription = "Bulk Add Games")
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            SeasonAndDivisionSelectors(
                seasons = uiState.seasons,
                selectedSeason = uiState.selectedSeason,
                selectedDivision = uiState.selectedDivision,
                onSeasonSelected = { viewModel.selectSeason(it) },
                onDivisionSelected = { viewModel.selectDivision(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Clickable Date Selector
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Column {
                        Text("Selected Date", style = MaterialTheme.typography.labelSmall)
                        val dateText = remember(uiState.selectedDate) {
                            val sdf = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
                            sdf.timeZone = TimeZone.getTimeZone("UTC")
                            sdf.format(uiState.selectedDate)
                        }
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (uiState.gamesOnSelectedDate.isEmpty()) {
                    item {
                        Text("No games scheduled for this day.", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    items(uiState.gamesOnSelectedDate) { game ->
                        val crew = allAssignments.filter { it.gameId == game.id }
                        GameScheduleItem(
                            game = game,
                            crew = crew,
                            referees = uiState.allReferees,
                            onEdit = {
                                editingGame = game
                                showGameDialog = true
                            },
                            onAssign = { showAssignDialog = game },
                            onDelete = { viewModel.deleteGame(game.id) }
                        )
                    }
                }
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showGameDialog) {
            AddGameDialog(
                game = editingGame,
                teams = uiState.teamsInDivision,
                division = uiState.selectedDivision!!,
                onDismiss = { 
                    showGameDialog = false
                    editingGame = null
                },
                onConfirm = { home, away, time, loc, field, friendly ->
                    if (editingGame != null) {
                        viewModel.updateGame(editingGame!!.copy(
                            homeTeamName = home.name,
                            awayTeamName = away.name,
                            time = time,
                            location = loc,
                            fieldNumber = field,
                            isFriendly = friendly
                        ))
                    } else {
                        viewModel.createGame(home, away, time, loc, field, friendly)
                    }
                    showGameDialog = false
                    editingGame = null
                }
            )
        }

        if (showAssignDialog != null) {
            AssignRefereeDialog(
                game = showAssignDialog!!,
                referees = uiState.allReferees,
                assignments = allAssignments,
                games = uiState.gamesOnSelectedDate,
                viewModel = viewModel,
                onDismiss = { showAssignDialog = null },
                onConfirm = { refereeId, position ->
                    val gameId = showAssignDialog?.id ?: return@AssignRefereeDialog
                    viewModel.assignReferee(gameId, refereeId, position)
                    showAssignDialog = null
                }
            )
        }

        if (showBulkDialog) {
            BulkAddGamesDialog(
                teams = uiState.teamsInDivision,
                division = uiState.selectedDivision!!,
                onDismiss = { showBulkDialog = false },
                onConfirm = { games ->
                    viewModel.createGames(games)
                    showBulkDialog = false
                }
            )
        }
    }
}

@Composable
fun GameScheduleItem(
    game: Game,
    crew: List<Assignment>,
    referees: List<com.google.refereeschedule.domain.model.User>,
    onEdit: () -> Unit,
    onAssign: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Game #${game.gameNumber}: ${game.homeTeamName} vs ${game.awayTeamName}", style = MaterialTheme.typography.titleSmall)
                    Text("Time: ${game.time} @ ${game.location} (Field ${game.fieldNumber})", style = MaterialTheme.typography.bodySmall)
                }
                Row {
                    IconButton(onClick = onAssign) {
                        Icon(Icons.Rounded.PersonAdd, contentDescription = "Assign Referee")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                    }
                }
            }
            
            if (crew.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Crew:", style = MaterialTheme.typography.labelSmall)
                crew.forEach { assignment ->
                    val refName = referees.find { it.id == assignment.refereeId }?.email ?: assignment.refereeId
                    Text("• ${assignment.position}: $refName", style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                if (game.isFriendly) {
                    Text("Friendly", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun AssignRefereeDialog(
    game: Game,
    referees: List<com.google.refereeschedule.domain.model.User>,
    assignments: List<Assignment>,
    games: List<Game>,
    viewModel: GameSchedulerViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String, AssignmentPosition) -> Unit
) {
    var selectedReferee by remember { mutableStateOf<com.google.refereeschedule.domain.model.User?>(null) }
    var selectedPosition by remember { mutableStateOf(AssignmentPosition.AssistantReferee) }
    var expanded by remember { mutableStateOf(false) }
    var showConflictWarning by remember { mutableStateOf(false) }

    val conflict = remember(selectedReferee) {
        selectedReferee?.let { ref ->
            assignments.find { it.refereeId == ref.id && it.gameId != game.id }?.let { assignment ->
                val otherGame = games.find { it.id == assignment.gameId }
                if (otherGame != null) {
                    val targetTimes = viewModel.getGameStartAndEnd(game)
                    val otherTimes = viewModel.getGameStartAndEnd(otherGame)
                    
                    if (targetTimes != null && otherTimes != null) {
                        val overlaps = targetTimes.first < otherTimes.second && otherTimes.first < targetTimes.second
                        if (overlaps) otherGame else null
                    } else null
                } else null
            }
        }
    }

    if (showConflictWarning && conflict != null) {
        AlertDialog(
            onDismissRequest = { showConflictWarning = false },
            title = { Text("Double Booking Warning") },
            text = { 
                Text("This referee is already assigned to a game at ${conflict.time} (${conflict.homeTeamName} vs ${conflict.awayTeamName}). Are you sure you want to assign them again?")
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        selectedReferee?.let { onConfirm(it.id, selectedPosition) }
                        showConflictWarning = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Assign")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConflictWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Referee") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Assigning to Game #${game.gameNumber}")
                
                // Referee Dropdown
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedReferee?.email ?: "Select Referee")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f) // Explicit width
                    ) {
                        referees.forEach { ref ->
                            DropdownMenuItem(
                                text = { Text(ref.email) },
                                onClick = { selectedReferee = ref; expanded = false }
                            )
                        }
                    }
                }

                if (conflict != null) {
                    Text(
                        "Conflict: Assigned to ${conflict.homeTeamName} @ ${conflict.time}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Position Selection
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    AssignmentPosition.entries.forEach { pos ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedPosition == pos, onClick = { selectedPosition = pos })
                            Text(pos.name, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (conflict != null) {
                        showConflictWarning = true
                    } else {
                        selectedReferee?.let { onConfirm(it.id, selectedPosition) }
                    }
                },
                enabled = selectedReferee != null
            ) {
                Text(if (conflict != null) "Assign Anyway" else "Assign")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddGameDialog(
    game: Game? = null,
    teams: List<Team>,
    division: Division,
    onDismiss: () -> Unit,
    onConfirm: (Team, Team, String, String, String, Boolean) -> Unit
) {
    var homeTeam by remember(game) { mutableStateOf(teams.find { it.name == game?.homeTeamName }) }
    var awayTeam by remember(game) { mutableStateOf(teams.find { it.name == game?.awayTeamName }) }
    var time by remember(game) { mutableStateOf(game?.time ?: "10:00 AM") }
    var location by remember(game) { mutableStateOf(game?.location ?: "Main Complex") }
    var fieldNumber by remember(game) { mutableStateOf(game?.fieldNumber ?: "1") }
    var isFriendly by remember(game) { mutableStateOf(game?.isFriendly ?: division.isFriendlyByDefault) }

    var homeExpanded by remember { mutableStateOf(false) }
    var awayExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (game != null) "Edit Game" else "Add Game to ${division.name}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    // Home Team Dropdown
                    Box {
                        OutlinedButton(onClick = { homeExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(homeTeam?.name ?: "Select Home Team")
                        }
                        DropdownMenu(expanded = homeExpanded, onDismissRequest = { homeExpanded = false }) {
                            teams.forEach { team ->
                                DropdownMenuItem(text = { Text(team.name) }, onClick = { homeTeam = team; homeExpanded = false })
                            }
                        }
                    }
                }
                item {
                    // Away Team Dropdown
                    Box {
                        OutlinedButton(onClick = { awayExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(awayTeam?.name ?: "Select Away Team")
                        }
                        DropdownMenu(expanded = awayExpanded, onDismissRequest = { awayExpanded = false }) {
                            teams.forEach { team ->
                                DropdownMenuItem(text = { Text(team.name) }, onClick = { awayTeam = team; awayExpanded = false })
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(value = fieldNumber, onValueChange = { fieldNumber = it }, label = { Text("Field #") }, modifier = Modifier.fillMaxWidth())
                }
                
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isFriendly, onCheckedChange = { isFriendly = it })
                        Text("Friendly Game")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (homeTeam != null && awayTeam != null) {
                        onConfirm(homeTeam!!, awayTeam!!, time, location, fieldNumber, isFriendly)
                    }
                },
                enabled = homeTeam != null && awayTeam != null
            ) {
                Text(if (game != null) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun BulkAddGamesDialog(
    teams: List<Team>,
    division: Division,
    onDismiss: () -> Unit,
    onConfirm: (List<BulkGameData>) -> Unit
) {
    val gameList = remember { mutableStateListOf<BulkGameInput>() }
    
    // Start with one empty row
    if (gameList.isEmpty()) {
        LaunchedEffect(Unit) {
            gameList.add(BulkGameInput(
                time = "10:00 AM",
                location = "Main Complex",
                fieldNumber = "1",
                isFriendly = division.isFriendlyByDefault
            ))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Add Games") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxHeight(0.8f)) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(gameList.size) { index ->
                        val game = gameList[index]
                        var homeExpanded by remember { mutableStateOf(false) }
                        var awayExpanded by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Game ${index + 1}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { gameList.removeAt(index) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Home Team
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(onClick = { homeExpanded = true }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 4.dp)) {
                                            Text(game.homeTeam?.name ?: "Home", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                        }
                                        DropdownMenu(expanded = homeExpanded, onDismissRequest = { homeExpanded = false }) {
                                            teams.forEach { team ->
                                                DropdownMenuItem(text = { Text(team.name) }, onClick = { 
                                                    gameList[index] = game.copy(homeTeam = team)
                                                    homeExpanded = false 
                                                })
                                            }
                                        }
                                    }
                                    // Away Team
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(onClick = { awayExpanded = true }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 4.dp)) {
                                            Text(game.awayTeam?.name ?: "Away", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                        }
                                        DropdownMenu(expanded = awayExpanded, onDismissRequest = { awayExpanded = false }) {
                                            teams.forEach { team ->
                                                DropdownMenuItem(text = { Text(team.name) }, onClick = { 
                                                    gameList[index] = game.copy(awayTeam = team)
                                                    awayExpanded = false 
                                                })
                                            }
                                        }
                                    }
                                }
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = game.time,
                                        onValueChange = { gameList[index] = game.copy(time = it) },
                                        label = { Text("Time") },
                                        modifier = Modifier.weight(1.5f),
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )
                                    OutlinedTextField(
                                        value = game.fieldNumber,
                                        onValueChange = { gameList[index] = game.copy(fieldNumber = it) },
                                        label = { Text("Field #") },
                                        modifier = Modifier.weight(1f),
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )
                                }

                                OutlinedTextField(
                                    value = game.location,
                                    onValueChange = { gameList[index] = game.copy(location = it) },
                                    label = { Text("Location") },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = game.isFriendly,
                                        onCheckedChange = { gameList[index] = game.copy(isFriendly = it) }
                                    )
                                    Text("Friendly", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    item {
                        TextButton(onClick = { 
                            val last = gameList.lastOrNull() ?: BulkGameInput()
                            gameList.add(last.copy(homeTeam = null, awayTeam = null))
                        }) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Text("Add Another Game")
                        }
                    }
                }
            }
        },
        confirmButton = {
            val validInputs = gameList.filter { it.homeTeam != null && it.awayTeam != null && it.time.isNotBlank() }
            Button(
                onClick = { 
                    onConfirm(validInputs.map { 
                        BulkGameData(
                            homeTeam = it.homeTeam!!,
                            awayTeam = it.awayTeam!!,
                            time = it.time,
                            location = it.location,
                            fieldNumber = it.fieldNumber,
                            isFriendly = it.isFriendly
                        )
                    })
                },
                enabled = validInputs.isNotEmpty()
            ) {
                Text("Add ${validInputs.size} Games")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

data class BulkGameInput(
    val homeTeam: Team? = null,
    val awayTeam: Team? = null,
    val time: String = "10:00 AM",
    val location: String = "Main Complex",
    val fieldNumber: String = "1",
    val isFriendly: Boolean = false
)
