package com.google.refereeschedule.ui.admin.teams

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            AddTeamDialog(
                team = editingTeam,
                onDismiss = { 
                    showAddDialog = false
                    editingTeam = null
                },
                onConfirm = { name ->
                    if (editingTeam != null) {
                        viewModel.updateTeam(editingTeam!!, name)
                    } else {
                        viewModel.addTeam(name)
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
            Text(team.name, style = MaterialTheme.typography.titleMedium)
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

@Composable
fun AddTeamDialog(
    team: Team? = null,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(team) { mutableStateOf(team?.name ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (team != null) "Edit Team" else "Add New Team") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Team Name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
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
