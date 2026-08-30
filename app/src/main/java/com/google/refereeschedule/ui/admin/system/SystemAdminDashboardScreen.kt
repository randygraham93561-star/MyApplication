package com.google.refereeschedule.ui.admin.system

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.refereeschedule.domain.model.Organization

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemAdminDashboardScreen(
    viewModel: SystemAdminDashboardViewModel,
    onNavigateToUserManagement: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingOrganization by remember { mutableStateOf<Organization?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Admin") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToUserManagement) {
                        Icon(Icons.Rounded.People, contentDescription = "Manage Users")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                editingOrganization = null
                showAddDialog = true 
            }) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Organization")
            }
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
                item {
                    Text("Organizations", style = MaterialTheme.typography.headlineSmall)
                }

                items(uiState.organizations) { organization ->
                    OrganizationCard(
                        organization = organization,
                        onAddAdmin = { email ->
                            viewModel.createAdminForOrganization(organization.id, email)
                        },
                        onEdit = { org ->
                            editingOrganization = org
                            showAddDialog = true
                        }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddOrganizationDialog(
                organization = editingOrganization,
                onDismiss = { 
                    showAddDialog = false
                    editingOrganization = null
                },
                onConfirm = { name, email ->
                    if (editingOrganization != null) {
                        viewModel.updateOrganization(editingOrganization!!.copy(name = name, contactEmail = email))
                    } else {
                        viewModel.createOrganization(name, email)
                    }
                    showAddDialog = false
                    editingOrganization = null
                }
            )
        }
    }
}

@Composable
fun OrganizationCard(
    organization: Organization,
    onAddAdmin: (String) -> Unit,
    onEdit: (Organization) -> Unit
) {
    var showAddAdmin by remember { mutableStateOf(false) }
    var adminEmail by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(organization.name, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { onEdit(organization) }) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Organization")
                }
            }
            Text("Contact: ${organization.contactEmail}", style = MaterialTheme.typography.bodyMedium)
            
            if (showAddAdmin) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = adminEmail,
                        onValueChange = { adminEmail = it },
                        label = { Text("Admin Email") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        onAddAdmin(adminEmail)
                        showAddAdmin = false
                        adminEmail = ""
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Confirm")
                    }
                }
            } else {
                TextButton(onClick = { showAddAdmin = true }) {
                    Text("Add Referee Admin")
                }
            }
        }
    }
}

@Composable
fun AddOrganizationDialog(
    organization: Organization? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember(organization) { mutableStateOf(organization?.name ?: "") }
    var email by remember(organization) { mutableStateOf(organization?.contactEmail ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (organization != null) "Edit Organization" else "Add Organization") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                TextField(value = email, onValueChange = { email = it }, label = { Text("Admin Email") })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, email) }) {
                Text(if (organization != null) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
