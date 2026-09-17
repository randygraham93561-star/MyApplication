package com.google.refereeschedule.ui.admin.teams

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.refereeschedule.domain.model.Team
import com.google.refereeschedule.ui.admin.components.SeasonAndDivisionSelectors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamManagementScreen(
    viewModel: TeamManagementViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTeam by remember { mutableStateOf<Team?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Teams") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.selectedDivision != null) {
                FloatingActionButton(onClick = { 
                    editingTeam = null
                    showAddDialog = true 
                }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Team")
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

            if (uiState.selectedDivision == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a division to manage teams")
                }
            } else {
                val filteredTeams = uiState.teams.filter { it.divisionName == uiState.selectedDivision?.name }
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredTeams.isEmpty()) {
                        item {
                            Text("No teams in this division yet.", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        items(filteredTeams) { team ->
                            TeamItem(
                                team = team,
                                onEdit = {
                                    editingTeam = team
                                    showAddDialog = true
                                },
                                onDelete = { viewModel.deleteTeam(team.id) }
                            )
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddTeamManagementDialog(
                team = editingTeam,
                initialDivision = uiState.selectedDivision?.name ?: "12U",
                onDismiss = { 
                    showAddDialog = false
                    editingTeam = null
                },
                onConfirm = { id, _, gen, _, _ ->
                    if (editingTeam != null) {
                        viewModel.updateTeam(editingTeam!!, id, gen, null, null)
                    } else {
                        viewModel.addTeam(id, gen, null, null)
                    }
                    showAddDialog = false
                    editingTeam = null
                }
            )
        }
    }
}

@Composable
fun TeamItem(team: Team, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(team.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = team.gender,
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTeamManagementDialog(
    team: Team? = null,
    initialDivision: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String?, String?) -> Unit
) {
    var teamNumber by remember(team) { 
        mutableStateOf(team?.name?.substringAfterLast("-") ?: "") 
    }
    var selectedDiv by remember(team) { mutableStateOf(team?.divisionName ?: initialDivision) }
    var selectedGender by remember(team) { mutableStateOf(team?.gender ?: "Boys") }
    
    var divExpanded by remember { mutableStateOf(false) }

    val generatedId = remember(selectedDiv, selectedGender, teamNumber) {
        if (selectedDiv.isNotEmpty() && selectedGender.isNotEmpty() && teamNumber.isNotEmpty()) {
            val genChar = selectedGender.first().uppercase()
            val formattedNum = if (teamNumber.length == 1) "0$teamNumber" else teamNumber
            "$selectedDiv$genChar-$formattedNum"
        } else {
            "----"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (team != null) "Edit Team (ID Builder)" else "Add Team (ID Builder)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Preview: $generatedId",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { divExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedDiv)
                        }
                        DropdownMenu(expanded = divExpanded, onDismissRequest = { divExpanded = false }) {
                            val ageGroups = listOf("8U", "10U", "12U", "14U", "16U", "19U")
                            ageGroups.forEach { age ->
                                DropdownMenuItem(text = { Text(age) }, onClick = { selectedDiv = age; divExpanded = false })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = teamNumber,
                        onValueChange = { if (it.length <= 2) teamNumber = it.filter { c -> c.isDigit() } },
                        label = { Text("Number") },
                        placeholder = { Text("01") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Boys", "Girls", "Coed").forEach { g ->
                        FilterChip(
                            selected = selectedGender == g,
                            onClick = { selectedGender = g },
                            label = { Text(g) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    onConfirm(generatedId, selectedDiv, selectedGender, null, null) 
                }, 
                enabled = generatedId != "----"
            ) {
                Text(if (team != null) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
