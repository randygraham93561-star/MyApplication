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
import com.google.refereeschedule.domain.model.Organization
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
                                snackbarHostState.showSnackbar("Updating ${user.email}...")
                            }
                        },
                        onUnlock = {
                            viewModel.unlockUser(user.id)
                            scope.launch {
                                snackbarHostState.showSnackbar("Unlocking ${user.email}...")
                            }
                        },
                        onRemoveAdmin = { orgId -> viewModel.removeAdminAccess(user, orgId) },
                        onRemoveCoach = { orgId -> viewModel.removeCoachAccess(user, orgId) }
                    )
                }
            }
        }
    }
}

@Composable
fun UserRoleCard(
    user: User,
    organizations: List<Organization>,
    onUpdateRole: (UserRole, String?) -> Unit,
    onUnlock: () -> Unit,
    onRemoveAdmin: (String) -> Unit,
    onRemoveCoach: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(user.email, style = MaterialTheme.typography.titleMedium)
                    Text("Current Role: ${user.userRole}", style = MaterialTheme.typography.bodyMedium)
                }
                if (user.isAccountLocked) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            "LOCKED",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (user.organizationId != null) {
                val orgName = organizations.find { it.id == user.organizationId }?.name ?: "Unknown"
                Text("Primary Organization: $orgName", style = MaterialTheme.typography.bodySmall)
            }

            // Admin Organizations
            if (user.adminOrgIds.isNotEmpty()) {
                Text("Admin Access:", style = MaterialTheme.typography.labelSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                user.adminOrgIds.forEach { orgId ->
                    val orgName = organizations.find { it.id == orgId }?.name ?: "Unknown"
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("• $orgName", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onRemoveAdmin(orgId) }, contentPadding = PaddingValues(0.dp)) {
                            Text("Remove", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Coach Organizations
            if (user.coachOrgIds.isNotEmpty()) {
                Text("Coach Access:", style = MaterialTheme.typography.labelSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                user.coachOrgIds.forEach { orgId ->
                    val orgName = organizations.find { it.id == orgId }?.name ?: "Unknown"
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("• $orgName", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onRemoveCoach(orgId) }, contentPadding = PaddingValues(0.dp)) {
                            Text("Remove", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide Options" else "Manage Access")
                }
                if (user.isAccountLocked) {
                    Button(
                        onClick = onUnlock,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Unlock Account")
                    }
                }
            }

            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Divider(Modifier.padding(vertical = 4.dp))
                    Text("Add Organization Access:", style = MaterialTheme.typography.labelSmall)
                    
                    UserRole.entries.forEach { role ->
                        if (role == UserRole.Admin || role == UserRole.CoachAdmin) {
                            val prefix = if (role == UserRole.Admin) "Referee Admin" else "Coach Admin"
                            organizations.forEach { org ->
                                // Don't show if already has access
                                val hasAccess = if (role == UserRole.Admin) user.adminOrgIds.contains(org.id) else user.coachOrgIds.contains(org.id)
                                if (!hasAccess) {
                                    TextButton(onClick = { 
                                        onUpdateRole(role, org.id)
                                        expanded = false
                                    }) {
                                        Text("Grant $prefix of ${org.name}")
                                    }
                                }
                            }
                        } else if (role == UserRole.Referee && user.userRole != UserRole.Referee) {
                            TextButton(onClick = { 
                                onUpdateRole(role, user.organizationId)
                                expanded = false
                            }) {
                                Text("Convert to Standard Referee")
                            }
                        }
                    }
                }
            }
        }
    }
}
