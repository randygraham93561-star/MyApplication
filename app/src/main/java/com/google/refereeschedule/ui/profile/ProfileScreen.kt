package com.google.refereeschedule.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.google.refereeschedule.domain.model.DivisionDifficulty
import com.google.refereeschedule.domain.model.PointDistributionMode
import com.google.refereeschedule.domain.model.RefereeProfile
import com.google.refereeschedule.ui.theme.MyApplicationTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { padding ->
        val screenModifier = modifier.padding(padding)
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(modifier = screenModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ProfileUiState.Success -> {
                ProfileContent(
                    profile = state.data.profile,
                    teams = state.data.teams,
                    organizations = state.data.organizations,
                    seasons = state.data.seasons,
                    onSave = { viewModel.updateProfile(it) },
                    modifier = screenModifier
                )
            }
            is ProfileUiState.Error -> {
                Box(modifier = screenModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    profile: RefereeProfile,
    teams: List<com.google.refereeschedule.domain.model.Team>,
    organizations: List<com.google.refereeschedule.domain.model.Organization>,
    seasons: List<com.google.refereeschedule.domain.model.Season>,
    onSave: (RefereeProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(profile.name) }
    var phone by remember { mutableStateOf(profile.phoneNumber) }
    var selectedOrgId by remember { mutableStateOf(profile.organizationId) }
    var selectedSeasonId by remember { mutableStateOf(profile.currentSeasonId) }
    var headComfort by remember { mutableFloatStateOf(profile.headRefereeComfortLevel.toFloat()) }
    var assistantComfort by remember { mutableFloatStateOf(profile.assistantRefereeComfortLevel.toFloat()) }
    var selectedTeamIds by remember { mutableStateOf(profile.teamIdsForPoints) }
    var distributionMode by remember { mutableStateOf(profile.distributionMode) }
    
    var teamExpanded by remember { mutableStateOf(false) }
    var orgExpanded by remember { mutableStateOf(false) }
    var seasonExpanded by remember { mutableStateOf(false) }

    val minLevel = remember(profile.badgeLevel) {
        DivisionDifficulty.getMinLevelForBadge(profile.badgeLevel)
    }

    val isNational = profile.badgeLevel.lowercase() == "national"

    // Auto-adjust if below minimum
    LaunchedEffect(minLevel) {
        if (headComfort < minLevel) headComfort = minLevel.toFloat()
        // Assistant minimum is at least 2 (excluding 8U) OR the badge minimum
        val effectiveArMin = minLevel.coerceAtLeast(2)
        if (assistantComfort < effectiveArMin) assistantComfort = effectiveArMin.toFloat()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Edit Profile",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number") },
            leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        Text(text = "Organization & Season", style = MaterialTheme.typography.titleMedium)
        
        // Organization Selection - Wrap in Box with zIndex to prevent clipping
        Box(modifier = Modifier.fillMaxWidth().zIndex(2f)) {
            ExposedDropdownMenuBox(
                expanded = orgExpanded,
                onExpandedChange = { orgExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                val orgName = organizations.find { it.id == selectedOrgId }?.name ?: "Select Organization"
                OutlinedTextField(
                    value = orgName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Organization") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orgExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = orgExpanded, onDismissRequest = { orgExpanded = false }) {
                    organizations.forEach { org ->
                        DropdownMenuItem(
                            text = { Text(org.name) },
                            onClick = {
                                selectedOrgId = org.id
                                selectedSeasonId = ""
                                selectedTeamIds = emptyList()
                                orgExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Season Selection - Wrap in Box with zIndex to prevent clipping
        Box(modifier = Modifier.fillMaxWidth().zIndex(1f)) {
            ExposedDropdownMenuBox(
                expanded = seasonExpanded,
                onExpandedChange = { if (selectedOrgId.isNotEmpty()) seasonExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                val filteredSeasons = seasons.filter { it.organizationId == selectedOrgId }
                val seasonName = filteredSeasons.find { it.id == selectedSeasonId }?.name ?: "Select Season"
                
                OutlinedTextField(
                    value = seasonName,
                    onValueChange = {},
                    readOnly = true,
                    enabled = selectedOrgId.isNotEmpty(),
                    label = { Text("Active Season") },
                    supportingText = { 
                        if (selectedOrgId.isEmpty()) Text("Select an organization first")
                        else if (filteredSeasons.isEmpty()) Text("No active seasons found for this organization")
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = seasonExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                if (selectedOrgId.isNotEmpty()) {
                    ExposedDropdownMenu(expanded = seasonExpanded, onDismissRequest = { seasonExpanded = false }) {
                        if (filteredSeasons.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No seasons available") },
                                onClick = { seasonExpanded = false }
                            )
                        } else {
                            filteredSeasons.forEach { season ->
                                DropdownMenuItem(
                                    text = { Text(season.name) },
                                    onClick = {
                                        selectedSeasonId = season.id
                                        seasonExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Comfort Levels", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            HelpIcon(
                title = "About Comfort Levels",
                message = "Comfort levels represent the highest division you feel confident officiating. Your Badge Level set by the admin restricts how low these can go (e.g., Advanced referees must stay at 14U Boys or higher)."
            )
        }
        
        // Head Referee Comfort
        val headLevelIndex = headComfort.toInt().coerceIn(0, 9)
        val headLevelName = DivisionDifficulty.headRefereeLevels[headLevelIndex]
        Text(text = "Head Referee: $headLevelName")
        Slider(
            value = headComfort,
            onValueChange = { headComfort = it },
            valueRange = minLevel.toFloat()..9f,
            steps = if (9 - minLevel > 0) (9 - minLevel) - 1 else 0,
            enabled = !isNational
        )

        // Assistant Referee Comfort (Excluding 8U)
        val effectiveArMin = minLevel.coerceAtLeast(2)
        val arLevelIndex = assistantComfort.toInt().coerceIn(effectiveArMin, 9)
        val arLevelName = DivisionDifficulty.headRefereeLevels[arLevelIndex]
        Text(text = "Assistant Referee: $arLevelName")
        Slider(
            value = assistantComfort,
            onValueChange = { assistantComfort = it },
            valueRange = effectiveArMin.toFloat()..9f,
            steps = if (9 - effectiveArMin > 0) (9 - effectiveArMin) - 1 else 0,
            enabled = !isNational
        )

        HorizontalDivider()

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Team for Points", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            HelpIcon(
                title = "Point Distribution",
                message = "Evenly: Points are split among your selected teams.\n\nManual: You pick the team after each game.\n\nBy Need: Points go to the teams needing them most to reach their weekend goal. Your teams are prioritized first."
            )
        }
        
        Text("How should your points be distributed?", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = distributionMode == PointDistributionMode.Even, onClick = { distributionMode = PointDistributionMode.Even })
                Text("Evenly between selected teams", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = distributionMode == PointDistributionMode.Manual, onClick = { distributionMode = PointDistributionMode.Manual })
                Text("Manually select per game", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = distributionMode == PointDistributionMode.NeedsBased, onClick = { distributionMode = PointDistributionMode.NeedsBased })
                Text("Prioritize teams who need points most", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (distributionMode != PointDistributionMode.NeedsBased || selectedTeamIds.isNotEmpty()) {
            Text("Selected Teams (${selectedTeamIds.size}/3)", style = MaterialTheme.typography.labelSmall)
            selectedTeamIds.forEachIndexed { index, teamId ->
                val team = teams.find { it.id == teamId }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = team?.let { "${it.name} (${it.divisionName})" } ?: "Unknown Team",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { 
                        selectedTeamIds = selectedTeamIds.toMutableList().apply { removeAt(index) }
                    }) {
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
                        value = "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("Add a team...") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teamExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = teamExpanded, onDismissRequest = { teamExpanded = false }) {
                        val availableTeams = teams.filter { it.organizationId == selectedOrgId && it.id !in selectedTeamIds }
                        if (availableTeams.isEmpty()) {
                            DropdownMenuItem(text = { Text("No other teams available") }, onClick = {})
                        } else {
                            availableTeams.forEach { team ->
                                DropdownMenuItem(
                                    text = { Text("${team.name} (${team.divisionName})") },
                                    onClick = {
                                        selectedTeamIds = selectedTeamIds + team.id
                                        teamExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "You haven't selected any specific teams. Your points will go to any organization teams that need them most.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        HorizontalDivider()

        Text(text = "Team Points Breakdown", style = MaterialTheme.typography.titleMedium)
        if (selectedTeamIds.isEmpty() && distributionMode != PointDistributionMode.NeedsBased) {
            Text("No teams selected for points.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        } else if (distributionMode == PointDistributionMode.NeedsBased && selectedTeamIds.isEmpty()) {
            Text("Points are automatically distributed to organization teams in need.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        } else {
            selectedTeamIds.forEach { teamId ->
                val team = teams.find { it.id == teamId }
                if (team != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(team.name, style = MaterialTheme.typography.bodyLarge)
                            Text(team.divisionName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "${team.totalPoints} pts",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider()
        
        InfoItem(label = "Badge Level", value = profile.badgeLevel)
        
        if (!profile.badgeCorrectionRequested) {
            OutlinedButton(
                onClick = {
                    onSave(profile.copy(badgeCorrectionRequested = true))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Request Badge Correction")
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Badge correction requested. An admin will review your level.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        InfoItem(label = "Total Points", value = "${profile.totalPoints}")

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Head Games", style = MaterialTheme.typography.labelSmall)
                    Text("${profile.headRefereeGamesCount}", style = MaterialTheme.typography.headlineSmall)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Assistant Games", style = MaterialTheme.typography.labelSmall)
                    Text("${profile.assistantRefereeGamesCount}", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onSave(profile.copy(
                    name = name,
                    phoneNumber = phone,
                    organizationId = selectedOrgId,
                    currentSeasonId = selectedSeasonId,
                    headRefereeComfortLevel = headComfort.toInt(),
                    assistantRefereeComfortLevel = assistantComfort.toInt(),
                    teamIdsForPoints = selectedTeamIds,
                    distributionMode = distributionMode
                ))
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedOrgId.isNotEmpty() && selectedSeasonId.isNotEmpty()
        ) {
            Text("Save Changes")
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
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

@Preview(showBackground = true)
@Composable
fun ProfilePreview() {
    MyApplicationTheme {
        ProfileContent(
            profile = RefereeProfile(
                name = "John Doe",
                phoneNumber = "123-456-7890",
                badgeLevel = "Level 8",
                totalPoints = 150,
                headRefereeComfortLevel = 5,
                assistantRefereeComfortLevel = 8
            ),
            teams = emptyList(),
            organizations = emptyList(),
            seasons = emptyList(),
            onSave = {}
        )
    }
}
