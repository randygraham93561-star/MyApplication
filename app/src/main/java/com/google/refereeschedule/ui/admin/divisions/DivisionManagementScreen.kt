package com.google.refereeschedule.ui.admin.divisions

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
import androidx.compose.ui.unit.dp
import com.google.refereeschedule.domain.model.Division
import com.google.refereeschedule.domain.model.DivisionDifficulty
import com.google.refereeschedule.ui.admin.components.SeasonSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DivisionManagementScreen(
    viewModel: DivisionManagementViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingDivision by remember { mutableStateOf<Division?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Divisions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.selectedSeason != null) {
                FloatingActionButton(onClick = { 
                    editingDivision = null
                    showAddDialog = true 
                }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Division")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            SeasonSelector(
                seasons = uiState.seasons,
                selectedSeason = uiState.selectedSeason,
                onSeasonSelected = { viewModel.selectSeason(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.selectedSeason == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Create a season first to manage divisions")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val divisions = uiState.selectedSeason?.divisions ?: emptyList()
                    if (divisions.isEmpty()) {
                        item {
                            Text("No divisions in this season yet.", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        items(divisions) { division ->
                            DivisionItem(
                                division = division,
                                onEdit = {
                                    editingDivision = division
                                    showAddDialog = true
                                },
                                onDelete = { viewModel.deleteDivision(division.name) }
                            )
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddDivisionDialog(
                division = editingDivision,
                onDismiss = { 
                    showAddDialog = false
                    editingDivision = null
                },
                onConfirm = { updatedDivision ->
                    if (editingDivision != null) {
                        viewModel.updateDivision(editingDivision!!.name, updatedDivision)
                    } else {
                        viewModel.addDivision(updatedDivision)
                    }
                    showAddDialog = false
                    editingDivision = null
                }
            )
        }
    }
}

@Composable
fun DivisionItem(
    division: Division,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(division.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Players: ${division.playersPerTeam} | Half: ${division.halfDurationMinutes}m | Ball: ${division.ballSize}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDivisionDialog(
    division: Division? = null,
    onDismiss: () -> Unit,
    onConfirm: (Division) -> Unit
) {
    var name by remember { mutableStateOf(division?.name ?: "12U") }
    var players by remember { mutableStateOf(division?.playersPerTeam?.toString() ?: "11") }
    var duration by remember { mutableStateOf(division?.halfDurationMinutes?.toString() ?: "45") }
    var ballSize by remember { mutableStateOf(division?.ballSize?.toString() ?: "5") }
    
    var nameExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (division != null) "Edit Division" else "Add Division") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Text("Select Division Name (Age Group)", style = MaterialTheme.typography.labelMedium)
                    ExposedDropdownMenuBox(
                        expanded = nameExpanded,
                        onExpandedChange = { nameExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nameExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = nameExpanded, onDismissRequest = { nameExpanded = false }) {
                            DivisionDifficulty.ageGroups.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { 
                                        name = option
                                        nameExpanded = false 
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = players,
                        onValueChange = { players = it },
                        label = { Text("Players per Team") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Half Duration (Minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = ballSize,
                        onValueChange = { ballSize = it },
                        label = { Text("Ball Size") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            val isFormValid = players.isNotBlank() && duration.isNotBlank() && ballSize.isNotBlank()
            Button(
                onClick = {
                    onConfirm(Division(
                        name = name,
                        difficultyLevel = DivisionDifficulty.getLevelForDivision(name, "Boys"), // Baseline
                        playersPerTeam = players.toIntOrNull() ?: 11,
                        halfDurationMinutes = duration.toIntOrNull() ?: 45,
                        ballSize = ballSize.toIntOrNull() ?: 5
                    ))
                },
                enabled = isFormValid
            ) {
                Text(if (division != null) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
