package com.google.refereeschedule.ui.points

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefereePointsScreen(
    viewModel: RefereePointsViewModel,
    onOpenDrawer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("League Standings") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Unassigned Points Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Unassigned Points", style = MaterialTheme.typography.titleMedium)
                                Text("Points not yet linked to a team", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = "${uiState.unassignedPoints}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Tab-like section headers
                item {
                    Text("Team Standings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }

                // Teams grouped by Division
                if (uiState.teamsByDivision.isEmpty()) {
                    item {
                        Text("No teams found for this organization.", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                uiState.teamsByDivision.forEach { (division, teams) ->
                    item {
                        Text(
                            text = division,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(teams.sortedByDescending { it.totalPoints }) { team ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(team.name, style = MaterialTheme.typography.bodyLarge)
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "${team.totalPoints} pts",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    Text("Point Audit History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Live records of every point awarded this season", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }

                if (uiState.pointHistory.isEmpty()) {
                    item {
                        Text("No point awards have been processed yet.", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    items(uiState.pointHistory) { award ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(award.timestamp),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = "+${award.points} pts",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text("Team: ${if(award.teamId == "unassigned") "Unassigned" else award.teamId}", style = MaterialTheme.typography.bodySmall)
                                Text("Game ID: ${award.gameId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
