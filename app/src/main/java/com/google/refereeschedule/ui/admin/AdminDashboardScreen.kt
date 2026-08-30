package com.google.refereeschedule.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.refereeschedule.domain.model.Assignment
import com.google.refereeschedule.domain.model.DivisionDifficulty
import com.google.refereeschedule.domain.model.RefereeProfile
import com.google.refereeschedule.domain.model.Season

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminDashboardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingSeason by remember { mutableStateOf<Season?>(null) }
    var editingReferee by remember { mutableStateOf<RefereeProfile?>(null) }
    var showSeasonDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Organization Profile")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                editingSeason = null
                showSeasonDialog = true 
            }) {
                Icon(Icons.Rounded.Add, contentDescription = "Create Season")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StatsSection(uiState)
            }

            item {
                FinalizeDaySection(
                    onFinalize = { date -> 
                        viewModel.finalizeGameDay(date)
                    }
                )
            }

            item {
                Text("Seasons", style = MaterialTheme.typography.titleLarge)
            }

            if (uiState.seasons.isEmpty()) {
                item {
                    Text("No seasons created yet", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(uiState.seasons) { season ->
                    SeasonCard(
                        season = season,
                        onEdit = {
                            editingSeason = season
                            showSeasonDialog = true
                        }
                    )
                }
            }

            item {
                Text("Pending Approvals", style = MaterialTheme.typography.titleLarge)
            }

            if (uiState.pendingAssignments.isEmpty()) {
                item {
                    Text("No pending approvals", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(uiState.pendingAssignments) { assignment ->
                    PendingAssignmentCard(
                        assignment = assignment,
                        onApprove = { viewModel.approveAssignment(assignment) },
                        onDeny = { viewModel.denyAssignment(assignment) }
                    )
                }
            }

            item {
                Text("Games", style = MaterialTheme.typography.titleLarge)
            }

            items(uiState.gamesWithAssignments) { (game, assignments) ->
                GameAdminCard(
                    game = game,
                    assignments = assignments,
                    onApproveReport = { viewModel.approveReport(game) }
                )
            }

            item {
                Text("Referees", style = MaterialTheme.typography.titleLarge)
            }

            if (uiState.refereeProfiles.isEmpty()) {
                item {
                    Text("No referees in your organization yet.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(uiState.refereeProfiles) { profile ->
                    RefereeAdminCard(
                        profile = profile,
                        onEdit = { editingReferee = profile }
                    )
                }
            }
        }

        if (showSeasonDialog) {
            SeasonDialog(
                season = editingSeason,
                onDismiss = { 
                    showSeasonDialog = false
                    editingSeason = null
                },
                onConfirm = { updatedSeason ->
                    if (editingSeason != null) {
                        viewModel.updateSeason(updatedSeason)
                    } else {
                        viewModel.createSeason(updatedSeason)
                    }
                    showSeasonDialog = false
                    editingSeason = null
                }
            )
        }

        if (editingReferee != null) {
            RefereeEditDialog(
                profile = editingReferee!!,
                teams = uiState.teams,
                onDismiss = { editingReferee = null },
                onConfirm = { 
                    viewModel.updateRefereeProfile(it)
                    editingReferee = null
                }
            )
        }
    }
}

@Composable
fun SeasonCard(season: Season, onEdit: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(season.name, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Season")
                }
            }
            if (season.collectPoints) {
                Text("Collecting points: Yes", style = MaterialTheme.typography.bodySmall)
                Text("Max Weekend Points: ${season.maxPointsPerWeekend}", style = MaterialTheme.typography.bodySmall)
                Text("Points: Center(${season.centerRefereePoints}) / AR(${season.assistantRefereePoints})", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Collecting points: No", style = MaterialTheme.typography.bodySmall)
            }

            if (season.divisions.isNotEmpty()) {
                Text("Divisions: ${season.divisions.joinToString { "${it.name}${if (!it.mentorsAllowed) " (No Mentors)" else ""}" }}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun SeasonDialog(
    season: Season? = null,
    onDismiss: () -> Unit,
    onConfirm: (Season) -> Unit
) {
    var name by remember(season) { mutableStateOf(season?.name ?: "") }
    var collectPoints by remember(season) { mutableStateOf(season?.collectPoints ?: false) }
    var maxPoints by remember(season) { mutableStateOf(season?.maxPointsPerWeekend?.toString() ?: "10") }
    var centerPoints by remember(season) { mutableStateOf(season?.centerRefereePoints?.toString() ?: "3") }
    var arPoints by remember(season) { mutableStateOf(season?.assistantRefereePoints?.toString() ?: "2") }
    
    val divisions = remember(season) { mutableStateListOf<com.google.refereeschedule.domain.model.Division>().apply {
        addAll(season?.divisions ?: emptyList())
    }}

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (season != null) "Edit Season" else "Create New Season") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Season Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Collect Referee Points")
                        Switch(checked = collectPoints, onCheckedChange = { collectPoints = it })
                    }
                }

                if (collectPoints) {
                    item {
                        OutlinedTextField(
                            value = maxPoints,
                            onValueChange = { maxPoints = it },
                            label = { Text("Max Points per Weekend") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = centerPoints,
                            onValueChange = { centerPoints = it },
                            label = { Text("Center Referee Points") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = arPoints,
                            onValueChange = { arPoints = it },
                            label = { Text("Assistant Referee Points") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Text("Divisions", style = MaterialTheme.typography.titleMedium)
                }

                items(divisions.size) { index ->
                    val division = divisions[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = division.name,
                            onValueChange = { divisions[index] = division.copy(name = it) },
                            placeholder = { Text("e.g. 10U") },
                            modifier = Modifier.weight(1f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Team Pts", style = MaterialTheme.typography.labelSmall)
                            Checkbox(
                                checked = division.teamsAccumulatePoints,
                                onCheckedChange = { divisions[index] = division.copy(teamsAccumulatePoints = it) }
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Mentors", style = MaterialTheme.typography.labelSmall)
                            Checkbox(
                                checked = division.mentorsAllowed,
                                onCheckedChange = { divisions[index] = division.copy(mentorsAllowed = it) }
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Friendly", style = MaterialTheme.typography.labelSmall)
                            Checkbox(
                                checked = division.isFriendlyByDefault,
                                onCheckedChange = { divisions[index] = division.copy(isFriendlyByDefault = it) }
                            )
                        }
                        IconButton(onClick = { divisions.removeAt(index) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Remove")
                        }
                    }
                }

                item {
                    TextButton(onClick = { 
                        divisions.add(com.google.refereeschedule.domain.model.Division(teamsAccumulatePoints = true))
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Division")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalSeason = (season ?: Season()).copy(
                        name = name,
                        collectPoints = collectPoints,
                        maxPointsPerWeekend = maxPoints.toIntOrNull() ?: 0,
                        centerRefereePoints = centerPoints.toIntOrNull() ?: 0,
                        assistantRefereePoints = arPoints.toIntOrNull() ?: 0,
                        divisions = divisions.toList()
                    )
                    onConfirm(finalSeason)
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (season != null) "Save" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalizeDaySection(onFinalize: (java.util.Date) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(java.util.Date()) }
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.time
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Finalize Game Day", style = MaterialTheme.typography.titleMedium)
            Text("This will award points for all approved reports on the selected date. Ensure all games for the day are reviewed first.", style = MaterialTheme.typography.bodySmall)
            
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(selectedDate)
                Text("Selected Date: $dateStr")
            }

            Button(
                onClick = { onFinalize(selectedDate) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Distribute Points for This Day")
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { 
                    datePickerState.selectedDateMillis?.let { selectedDate = java.util.Date(it) }
                    showDatePicker = false 
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun StatsSection(uiState: AdminDashboardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Total Games", uiState.totalGames.toString(), Modifier.weight(1f))
            StatCard("Fully Covered", uiState.fullyCovered.toString(), Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Missing Refs", uiState.missingRefs.toString(), Modifier.weight(1f))
            StatCard("Pending Appr.", uiState.pendingApprovals.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(text = value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
fun PendingAssignmentCard(
    assignment: Assignment,
    onApprove: () -> Unit,
    onDeny: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Referee ID: ${assignment.refereeId}")
            Text("Game ID: ${assignment.gameId}")
            Text("Position: ${assignment.position}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApprove) { Text("Approve") }
                OutlinedButton(onClick = onDeny) { Text("Deny") }
            }
        }
    }
}

@Composable
fun HelpIcon(title: String, message: String) {
    var showDialog by remember { mutableStateOf(false) }

    IconButton(onClick = { showDialog = true }, modifier = Modifier.size(24.dp)) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
            contentDescription = "Help",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("Got it") }
            }
        )
    }
}
    @Composable
fun GameAdminCard(
    game: com.google.refereeschedule.domain.model.Game,
    assignments: List<Assignment>,
    onApproveReport: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${game.homeTeamName} vs ${game.awayTeamName}", style = MaterialTheme.typography.titleMedium)
            Text("Crew: ${assignments.size}/${game.requiredCrewSize}")
            Text("Status: ${game.status}")
            
            if (game.status == com.google.refereeschedule.domain.model.GameStatus.PendingReview) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("Report Details", style = MaterialTheme.typography.labelMedium)
                Text("Score: ${game.homeScore} - ${game.awayScore}", style = MaterialTheme.typography.bodySmall)
                if (game.cardsShown) {
                    Text("Cards: ${game.cardTypes}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Text("Description: ${game.disciplinaryDescription}", style = MaterialTheme.typography.bodySmall)
                    Text("Signed: ${game.reporterSignature}", style = MaterialTheme.typography.bodySmall)
                }
                
                Spacer(Modifier.height(8.dp))
                Button(onClick = onApproveReport, modifier = Modifier.fillMaxWidth()) {
                    Text("Approve Report")
                }
            } else if (game.status == com.google.refereeschedule.domain.model.GameStatus.ReportApproved) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                ) {
                    Text(
                        "Report Approved - Points will be awarded upon Game Day Finalization",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun RefereeAdminCard(profile: RefereeProfile, onEdit: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(profile.name, style = MaterialTheme.typography.titleMedium)
                    if (profile.badgeCorrectionRequested) {
                        Spacer(Modifier.width(8.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
                            Text("Correction Requested", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                Text("Badge: ${profile.badgeLevel}", style = MaterialTheme.typography.bodySmall)
                val teamText = if (profile.distributionMode == com.google.refereeschedule.domain.model.PointDistributionMode.NeedsBased && profile.teamIdsForPoints.isEmpty()) {
                    "Points: Auto-Assigned (By Need)"
                } else {
                    "Teams: ${profile.teamIdsForPoints.size}/3 (${profile.distributionMode.name})"
                }
                Text(teamText, style = MaterialTheme.typography.bodySmall)
                Text("Points: ${profile.totalPoints}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit Referee")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefereeEditDialog(
    profile: RefereeProfile,
    teams: List<com.google.refereeschedule.domain.model.Team>,
    onDismiss: () -> Unit,
    onConfirm: (RefereeProfile) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var badgeLevel by remember { mutableStateOf(profile.badgeLevel) }
    var headComfort by remember { mutableFloatStateOf(profile.headRefereeComfortLevel.toFloat()) }
    var arComfort by remember { mutableFloatStateOf(profile.assistantRefereeComfortLevel.toFloat()) }
    var selectedTeamIds by remember { mutableStateOf(profile.teamIdsForPoints) }
    var distributionMode by remember { mutableStateOf(profile.distributionMode) }
    
    var teamExpanded by remember { mutableStateOf(false) }
    var badgeExpanded by remember { mutableStateOf(false) }

    val badges = listOf("Regional", "Intermediate", "Advanced", "National")
    
    val minLevel = remember(badgeLevel) {
        DivisionDifficulty.getMinLevelForBadge(badgeLevel)
    }
    val isNational = badgeLevel.lowercase() == "national"

    // Auto-adjust if below minimum
    LaunchedEffect(minLevel) {
        if (headComfort < minLevel) headComfort = minLevel.toFloat()
        val effectiveArMin = minLevel.coerceAtLeast(2)
        if (arComfort < effectiveArMin) arComfort = effectiveArMin.toFloat()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Referee Profile") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                }
                
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Badge Level", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        HelpIcon(
                            title = "Badge Levels",
                            message = "Regional: No restrictions.\n\nIntermediate: Comfort levels restricted to 12U Boys minimum.\n\nAdvanced: Comfort levels restricted to 14U Boys minimum.\n\nNational: Locked to 19U Boys only."
                        )
                    }
                    ExposedDropdownMenuBox(
                        expanded = badgeExpanded,
                        onExpandedChange = { badgeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = badgeLevel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = badgeExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = badgeExpanded, onDismissRequest = { badgeExpanded = false }) {
                            badges.forEach { level ->
                                DropdownMenuItem(text = { Text(level) }, onClick = { badgeLevel = level; badgeExpanded = false })
                            }
                        }
                    }
                }

                item {
                    Text("Comfort Levels", style = MaterialTheme.typography.titleSmall)
                    
                    val headLevelName = DivisionDifficulty.headRefereeLevels[headComfort.toInt().coerceIn(0, 9)]
                    Text("Head: $headLevelName", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = headComfort,
                        onValueChange = { headComfort = it },
                        valueRange = minLevel.toFloat()..9f,
                        steps = if (9 - minLevel > 0) (9 - minLevel) - 1 else 0,
                        enabled = !isNational,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val effectiveArMin = minLevel.coerceAtLeast(2)
                    val arLevelName = DivisionDifficulty.headRefereeLevels[arComfort.toInt().coerceIn(effectiveArMin, 9)]
                    Text("Assistant: $arLevelName", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = arComfort,
                        onValueChange = { arComfort = it },
                        valueRange = effectiveArMin.toFloat()..9f,
                        steps = if (9 - effectiveArMin > 0) (9 - effectiveArMin) - 1 else 0,
                        enabled = !isNational,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Team for Points", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        HelpIcon(
                            title = "Point Distribution",
                            message = "Evenly: Points are split among up to 3 selected teams.\n\nManual: Referee chooses team per game.\n\nBy Need: Automatically assigns to organization teams with lowest weekend points (prioritizes referee's selected teams)."
                        )
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        com.google.refereeschedule.domain.model.PointDistributionMode.entries.forEach { mode ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = distributionMode == mode, onClick = { distributionMode = mode })
                                Text(
                                    text = when(mode) {
                                        com.google.refereeschedule.domain.model.PointDistributionMode.Even -> "Evenly Distributed"
                                        com.google.refereeschedule.domain.model.PointDistributionMode.Manual -> "Manual Selection"
                                        com.google.refereeschedule.domain.model.PointDistributionMode.NeedsBased -> "By Need"
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    selectedTeamIds.forEachIndexed { index, teamId ->
                        val team = teams.find { it.id == teamId }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(team?.let { "${it.name} (${it.divisionName})" } ?: "Unknown Team", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { selectedTeamIds = selectedTeamIds.toMutableList().apply { removeAt(index) } }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Remove")
                            }
                        }
                    }

                    if (selectedTeamIds.size < 3) {
                        ExposedDropdownMenuBox(
                            expanded = teamExpanded,
                            onExpandedChange = { teamExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = "Add team...",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teamExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = teamExpanded, onDismissRequest = { teamExpanded = false }) {
                                teams.filter { it.id !in selectedTeamIds }.forEach { team ->
                                    DropdownMenuItem(
                                        text = { Text("${team.name} (${team.divisionName})") },
                                        onClick = { selectedTeamIds = selectedTeamIds + team.id; teamExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(profile.copy(
                    name = name,
                    badgeLevel = badgeLevel,
                    headRefereeComfortLevel = headComfort.toInt(),
                    assistantRefereeComfortLevel = arComfort.toInt(),
                    teamIdsForPoints = selectedTeamIds,
                    distributionMode = distributionMode,
                    badgeCorrectionRequested = false
                ))
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
