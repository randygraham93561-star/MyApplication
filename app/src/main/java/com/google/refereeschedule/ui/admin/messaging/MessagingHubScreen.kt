package com.google.refereeschedule.ui.admin.messaging

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.refereeschedule.domain.model.RefereeProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagingHubScreen(
    viewModel: MessagingHubViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            title = ""
            message = ""
            snackbarHostState.showSnackbar("Message sent successfully!")
            viewModel.clearStatus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Messaging Hub") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Recipient List
            Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp)) {
                Text("Recipients", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { viewModel.selectAll() }) { Text("Select All") }
                    TextButton(onClick = { viewModel.clearSelection() }) { Text("Clear") }
                }
                
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(uiState.referees) { ref ->
                        val isSelected = uiState.selectedRefereeIds.contains(ref.id)
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleRefereeSelection(ref.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(ref.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(ref.badgeLevel, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                
                Text(
                    text = "${uiState.selectedRefereeIds.size} selected",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            VerticalDivider()

            // Composer
            Column(modifier = Modifier.weight(1.5f).fillMaxHeight().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Compose Message", style = MaterialTheme.typography.titleMedium)
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Subject / Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    minLines = 5
                )

                if (uiState.error != null) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = { viewModel.sendMessage(title, message) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = title.isNotBlank() && message.isNotBlank() && uiState.selectedRefereeIds.isNotEmpty() && !uiState.isSending
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Rounded.Send, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Send Announcement")
                    }
                }
            }
        }
    }
}
