package com.google.refereeschedule.ui.admin.system

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.refereeschedule.domain.model.PrintComponent
import com.google.refereeschedule.domain.model.PrintJobType
import com.google.refereeschedule.domain.model.PrintTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintTemplateManagementScreen(
    viewModel: PrintTemplateViewModel,
    onNavigateToCanvas: (String?) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingTemplate by remember { mutableStateOf<PrintTemplate?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Print Templates") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingTemplate = null
                showDialog = true
            }) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Template")
            }
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
                if (uiState.templates.isEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("No Templates Found", style = MaterialTheme.typography.titleMedium)
                                Text("The printing system requires at least one template per type. Tap the button below to initialize the standard league templates.", style = MaterialTheme.typography.bodySmall)
                                Button(
                                    onClick = { viewModel.createStarterPack() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Create Starter Templates")
                                }
                            }
                        }
                    }
                }

                items(uiState.templates) { template ->
                    TemplateCard(
                        template = template,
                        onEdit = {
                            onNavigateToCanvas(template.id)
                        },
                        onEditMeta = {
                            editingTemplate = template
                            showDialog = true
                        },
                        onDelete = { viewModel.deleteTemplate(template.id) }
                    )
                }
            }
        }

        if (showDialog) {
            TemplateEditDialog(
                template = editingTemplate,
                onDismiss = { showDialog = false },
                onConfirm = { name, type, isDefault, description ->
                    if (editingTemplate != null) {
                        viewModel.updateTemplate(editingTemplate!!.copy(
                            name = name,
                            type = type,
                            isDefault = isDefault,
                            description = description
                        ))
                    } else {
                        viewModel.saveTemplate(name, type, emptyList<PrintComponent>(), isDefault, description)
                    }
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun TemplateCard(
    template: PrintTemplate,
    onEdit: () -> Unit,
    onEditMeta: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(template.name, style = MaterialTheme.typography.titleMedium)
                    Text("Type: ${template.type}", style = MaterialTheme.typography.bodySmall)
                }
                Row {
                    IconButton(onClick = onEditMeta) { Icon(Icons.Rounded.Settings, contentDescription = "Edit Meta") }
                    IconButton(onClick = onEdit) { Icon(Icons.Rounded.Brush, contentDescription = "Open Canvas") }
                    IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
                }
            }
            // ...
            if (template.isDefault) {
                SuggestionChip(
                    onClick = { },
                    label = { Text("Default", style = MaterialTheme.typography.labelSmall) },
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                )
            }
            Text(template.description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditDialog(
    template: PrintTemplate?,
    onDismiss: () -> Unit,
    onConfirm: (String, PrintJobType, Boolean, String) -> Unit
) {
    var name by remember { mutableStateOf(template?.name ?: "") }
    var type by remember { mutableStateOf(template?.type ?: PrintJobType.LunchVoucher) }
    var isDefault by remember { mutableStateOf(template?.isDefault ?: false) }
    var description by remember { mutableStateOf(template?.description ?: "") }
    
    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (template != null) "Edit Template Meta" else "New Template Meta") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Template Name") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = type.name,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Job Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                            PrintJobType.entries.forEach { jobType ->
                                DropdownMenuItem(
                                    text = { Text(jobType.name) },
                                    onClick = {
                                        type = jobType
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                        Text("Default Template")
                    }
                }
                item {
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, type, isDefault, description) }, enabled = name.isNotBlank()) {
                Text("Save Meta")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
