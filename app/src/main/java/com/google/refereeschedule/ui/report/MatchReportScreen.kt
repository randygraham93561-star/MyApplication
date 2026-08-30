package com.google.refereeschedule.ui.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.refereeschedule.domain.model.AssignmentPosition
import com.google.refereeschedule.domain.model.PointDistributionMode
import com.google.refereeschedule.domain.model.RefereeVerification
import com.google.refereeschedule.domain.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchReportScreen(
    gameId: String,
    viewModel: MatchReportViewModel,
    onNavigateBack: () -> Unit
) {
    val game = viewModel.game
    val myProfile by viewModel.currentUserProfile.collectAsState()
    val myTeams by viewModel.myTeams.collectAsState()
    
    var teamExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(gameId) {
        viewModel.loadGame(gameId)
    }

    LaunchedEffect(myProfile, myTeams) {
        if (myProfile?.distributionMode == PointDistributionMode.Manual && viewModel.selectedTargetTeamId.isEmpty()) {
            viewModel.selectedTargetTeamId = myTeams.firstOrNull()?.id ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Match Report") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (game == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "${game.homeTeamName} vs ${game.awayTeamName}",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "${game.date} @ ${game.time}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = viewModel.homeScore,
                        onValueChange = { viewModel.homeScore = it },
                        label = { Text("${game.homeTeamName} Score") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = viewModel.awayScore,
                        onValueChange = { viewModel.awayScore = it },
                        label = { Text("${game.awayTeamName} Score") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                HorizontalDivider()

                Text("Disciplinary Action", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = viewModel.cardsShown, onCheckedChange = { viewModel.cardsShown = it })
                    Spacer(Modifier.width(12.dp))
                    Text("Were cards shown?")
                }

                if (viewModel.cardsShown) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = viewModel.yellowCards, onCheckedChange = { viewModel.yellowCards = it })
                            Text("Yellow")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = viewModel.redCards, onCheckedChange = { viewModel.redCards = it })
                            Text("Red")
                        }
                    }

                    OutlinedTextField(
                        value = viewModel.disciplinaryDescription,
                        onValueChange = { viewModel.disciplinaryDescription = it },
                        label = { Text("Incident Description (Required)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    OutlinedTextField(
                        value = viewModel.signature,
                        onValueChange = { viewModel.signature = it },
                        label = { Text("Reporter Signature / Name (Required)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = viewModel.notes,
                    onValueChange = { viewModel.notes = it },
                    label = { Text("Internal Game Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5
                )

                HorizontalDivider()

                Text("Referee Verification", style = MaterialTheme.typography.titleMedium)
                viewModel.assignedReferees.forEach { (assignment, user) ->
                    val verification = viewModel.refereeVerifications[user.id]
                    RefereeVerificationCard(
                        assignedUser = user,
                        assignment = assignment,
                        verification = verification,
                        allReferees = viewModel.allOrganizationReferees,
                        onUpdate = { isPresent, correctRole, actualId, actualRole ->
                            viewModel.updateVerification(user.id, isPresent, correctRole, actualId, actualRole)
                        }
                    )
                }

                HorizontalDivider()

                if (myProfile?.distributionMode == PointDistributionMode.Manual && myTeams.isNotEmpty()) {
                    Text("Select team to receive your points:", style = MaterialTheme.typography.titleMedium)
                    ExposedDropdownMenuBox(
                        expanded = teamExpanded,
                        onExpandedChange = { teamExpanded = it }
                    ) {
                        val teamName = myTeams.find { it.id == viewModel.selectedTargetTeamId }?.name ?: "Select Team"
                        OutlinedTextField(
                            value = teamName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teamExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = teamExpanded, onDismissRequest = { teamExpanded = false }) {
                            myTeams.forEach { team ->
                                DropdownMenuItem(
                                    text = { Text("${team.name} (${team.divisionName})") },
                                    onClick = { 
                                        viewModel.selectedTargetTeamId = team.id
                                        teamExpanded = false 
                                    }
                                )
                            }
                        }
                    }
                }

                if (viewModel.submitError != null) {
                    Text(text = viewModel.submitError!!, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = { viewModel.submitReport(onNavigateBack) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting
                ) {
                    if (viewModel.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Submit Report")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefereeVerificationCard(
    assignedUser: User,
    assignment: com.google.refereeschedule.domain.model.Assignment,
    verification: RefereeVerification?,
    allReferees: List<User>,
    onUpdate: (Boolean, Boolean, String?, AssignmentPosition?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var roleExpanded by remember { mutableStateOf(false) }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${assignedUser.email} (${assignment.position.name})", style = MaterialTheme.typography.bodyMedium)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = verification?.isPresent ?: false,
                    onCheckedChange = { onUpdate(it, verification?.correctRole ?: true, if (it) assignedUser.id else null, verification?.actualRole) }
                )
                Text("Referee was present", style = MaterialTheme.typography.bodySmall)
            }

            if (verification?.isPresent == false) {
                Text("Who officiated this role?", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    val actualRef = allReferees.find { it.id == verification?.actualRefereeId }
                    OutlinedTextField(
                        value = actualRef?.email ?: "No Referee",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("No Referee") },
                            onClick = { onUpdate(false, false, null, null); expanded = false }
                        )
                        allReferees.forEach { ref ->
                            DropdownMenuItem(
                                text = { Text(ref.email) },
                                onClick = { onUpdate(false, true, ref.id, assignment.position); expanded = false }
                            )
                        }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = verification?.correctRole ?: false,
                        onCheckedChange = { onUpdate(true, it, assignedUser.id, if (it) assignment.position else null) }
                    )
                    Text("Officiated in assigned role", style = MaterialTheme.typography.bodySmall)
                }

                if (verification?.correctRole == false) {
                    Text("What role did they perform?", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    ExposedDropdownMenuBox(
                        expanded = roleExpanded,
                        onExpandedChange = { roleExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = verification.actualRole?.name ?: "Select Role",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                            AssignmentPosition.entries.forEach { pos ->
                                DropdownMenuItem(
                                    text = { Text(pos.name) },
                                    onClick = { onUpdate(true, false, assignedUser.id, pos); roleExpanded = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
