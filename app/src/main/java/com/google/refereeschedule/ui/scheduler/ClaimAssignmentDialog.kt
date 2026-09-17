package com.google.refereeschedule.ui.scheduler

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.refereeschedule.domain.model.Assignment
import com.google.refereeschedule.domain.model.AssignmentPosition
import com.google.refereeschedule.domain.model.Game
import com.google.refereeschedule.domain.model.RefereeProfile
import kotlinx.coroutines.launch

@Composable
fun ClaimAssignmentDialog(
    game: Game,
    referee: RefereeProfile,
    viewModel: ClaimAssignmentViewModel,
    onDismiss: () -> Unit
) {
    var selectedPosition by remember { 
        mutableStateOf(if (referee.isMentor && game.isMentorRequested) AssignmentPosition.Mentor else AssignmentPosition.AssistantReferee) 
    }
    var mentorRequested by remember { mutableStateOf(false) }
    var validationResult by remember { mutableStateOf<ClaimValidationResult?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedPosition) {
        validationResult = viewModel.validateClaim(referee, game, selectedPosition)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Claim Assignment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select your position for the game between ${game.homeTeamName} and ${game.awayTeamName}:")
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedPosition == AssignmentPosition.HeadReferee,
                            onClick = { selectedPosition = AssignmentPosition.HeadReferee }
                        )
                        Text("Head")
                    }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedPosition == AssignmentPosition.AssistantReferee,
                            onClick = { selectedPosition = AssignmentPosition.AssistantReferee }
                        )
                        Text("Assistant")
                    }
                    if (referee.isMentor || game.isMentorRequested) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedPosition == AssignmentPosition.Mentor,
                                onClick = { selectedPosition = AssignmentPosition.Mentor }
                            )
                            Text("Mentor")
                        }
                    }
                }

                if (game.mentorsAllowed) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = mentorRequested,
                            onCheckedChange = { mentorRequested = it }
                        )
                        Text("I would like a mentor for this game")
                    }
                }

                validationResult?.let { result ->
                    when (result) {
                        is ClaimValidationResult.Ok -> {
                            Text("Validation: OK (Auto-Approved)", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        }
                        is ClaimValidationResult.Warning -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                            ) {
                                Text(
                                    text = result.message,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(8.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        is ClaimValidationResult.Error -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Text(
                                    text = result.message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(8.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        is ClaimValidationResult.RequiresAdmin -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Text(
                                    text = result.message,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(8.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.claimAssignment(
                            Assignment(
                                gameId = game.id,
                                refereeId = referee.id,
                                position = selectedPosition,
                                organizationId = game.organizationId,
                                mentorRequested = mentorRequested
                            ),
                            validationResult ?: ClaimValidationResult.Ok
                        )
                        onDismiss()
                    }
                },
                enabled = validationResult !is ClaimValidationResult.Error
            ) {
                Text(
                    text = when(validationResult) {
                        is ClaimValidationResult.Warning -> "Confirm & Auto-Approve"
                        is ClaimValidationResult.RequiresAdmin -> "Submit for Admin Approval"
                        else -> "Confirm Assignment"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
