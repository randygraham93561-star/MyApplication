package com.google.refereeschedule.ui.admin.system

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.refereeschedule.domain.model.User
import com.google.refereeschedule.domain.model.UserRole
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    viewModel: UserManagementViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("User Access Management") },
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.users) { user ->
                    UserRoleCard(
                        user = user,
                        organizations = uiState.organizations,
                        onUpdateRole = { role, orgId ->
                            viewModel.updateUserRole(user, role, orgId)
                            scope.launch {
                                snackbarHostState.showSnackbar("Updating ${user.email} to $role...")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun UserRoleCard(
    user: User,
    organizations: List<com.google.refereeschedule.domain.model.Organization>,
    onUpdateRole: (UserRole, String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(user.email, style = MaterialTheme.typography.titleMedium)
            Text("Current Role: ${user.role}", style = MaterialTheme.typography.bodyMedium)
            if (user.organizationId != null) {
                val orgName = organizations.find { it.id == user.organizationId }?.name ?: "Unknown"
                Text("Organization: $orgName", style = MaterialTheme.typography.bodySmall)
            }

            Button(onClick = { expanded = !expanded }) {
                Text("Change Role")
            }

            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    UserRole.entries.forEach { role ->
                        if (role == UserRole.Admin) {
                            // Show org selector for Admin role
                            organizations.forEach { org ->
                                TextButton(onClick = { 
                                    onUpdateRole(role, org.id)
                                    expanded = false
                                }) {
                                    Text("Make Admin of ${org.name}")
                                }
                            }
                        } else {
                            TextButton(onClick = { 
                                onUpdateRole(role, null)
                                expanded = false
                            }) {
                                Text("Make $role")
                            }
                        }
                    }
                }
            }
        }
    }
}
