package com.google.refereeschedule.ui.admin.print

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.refereeschedule.domain.model.PrintJob
import com.google.refereeschedule.domain.model.PrintJobStatus
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintManagementScreen(
    viewModel: PrintManagementViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Print Queue Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("Pending Jobs", style = MaterialTheme.typography.titleLarge)
                }

                if (uiState.pendingJobs.isEmpty()) {
                    item {
                        Text("No pending print requests.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    items(uiState.pendingJobs) { job ->
                        PrintJobCard(
                            job = job,
                            onComplete = { viewModel.markJobCompleted(job.id) },
                            onFail = { viewModel.markJobFailed(job.id, "Manually failed by Admin") }
                        )
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Recent Activity", style = MaterialTheme.typography.titleLarge)
                }

                items(uiState.completedJobs.take(10)) { job ->
                    CompletedJobItem(job)
                }
            }
        }
    }
}

@Composable
fun PrintJobCard(
    job: PrintJob,
    onComplete: () -> Unit,
    onFail: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(job.type, style = MaterialTheme.typography.titleMedium)
                val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(job.timestamp)
                Text(timeStr, style = MaterialTheme.typography.labelSmall)
            }
            
            Text("Referee ID: ${job.refereeId}", style = MaterialTheme.typography.bodySmall)
            
            // Show a preview of the rendered content if possible
            Surface(
                color = Color.Black,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = job.renderedContent.take(100) + "...",
                    color = Color.Green,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(8.dp),
                    maxLines = 3
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onComplete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Mark Printed")
                }
                OutlinedButton(
                    onClick = onFail,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel Job")
                }
            }
        }
    }
}

@Composable
fun CompletedJobItem(job: PrintJob) {
    ListItem(
        headlineContent = { Text("${job.type} - ${job.status}") },
        supportingContent = { 
            val dateStr = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(job.timestamp)
            Text("Finished: $dateStr") 
        },
        trailingContent = {
            Icon(
                imageVector = if (job.status == PrintJobStatus.Completed.name) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                contentDescription = null,
                tint = if (job.status == PrintJobStatus.Completed.name) Color.Green else Color.Red
            )
        }
    )
}
