package com.google.refereeschedule.ui.admin.scheduler

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleImportScreen(
    viewModel: ScheduleImportViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFormatDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val reader = InputStreamReader(stream)
                        val content = reader.readText()
                        viewModel.importFromFile(content)
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulk Game Upload") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Upload a .CSV file (exported from Excel or Google Sheets) to batch-import your games.",
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = { showFormatDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.FileOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Select CSV File to Upload")
                }
            }

            if (uiState.successMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.successMessage!!,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            if (uiState.error != null) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }

            if (uiState.validationErrors.isNotEmpty()) {
                Text("Validation Issues Found:", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    items(uiState.validationErrors) { error ->
                        Text("• $error", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (showFormatDialog) {
            AlertDialog(
                onDismissRequest = { showFormatDialog = false },
                title = { Text("Confirm File Format") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Is your file in the correct order? To export from Excel/Sheets, choose 'Save As .CSV'.", style = MaterialTheme.typography.bodyMedium)
                        Text("Required Columns (Skip row 1):", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "1. Date (YYYY-MM-DD)\n2. Time (24h format, e.g. 14:30)\n3. Division Name\n4. Home Team\n5. Away Team\n6. Location\n7. Field #\n8. Friendly (Yes/No)\n9. Crew Size",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { 
                        showFormatDialog = false
                        filePickerLauncher.launch(arrayOf("text/comma-separated-values", "text/csv")) 
                    }) {
                        Text("Yes, Upload File")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFormatDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
