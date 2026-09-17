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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.google.refereeschedule.domain.model.Organization
import com.google.refereeschedule.domain.model.SubscriptionTier
import java.util.Calendar
import java.util.Date

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
                    Text("Platform Analytics", style = MaterialTheme.typography.headlineSmall)
                }

                item {
                    AnalyticsSection(uiState.analytics)
                }

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
                onConfirm = { name, email, tier, expiresAt, model, ip, mac, settings ->
                    if (editingOrganization != null) {
                        viewModel.updateOrganization(editingOrganization!!.copy(
                            name = name, 
                            contactEmail = email,
                            tier = tier.name,
                            subscriptionExpiresAt = expiresAt,
                            printerModel = model,
                            printerIp = ip,
                            printerMacAddress = mac,
                            printerSettings = settings
                        ))
                    } else {
                        viewModel.createOrganization(name, email, tier, expiresAt, model, ip, mac, settings)
                    }
                    showAddDialog = false
                    editingOrganization = null
                }
            )
        }
    }
}

@Composable
fun AnalyticsSection(analytics: SystemAdminAnalytics) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AnalyticsCard(
                label = "Total Platform Users",
                value = analytics.totalUsers.toString(),
                modifier = Modifier.weight(1f)
            )
            AnalyticsCard(
                label = "Active Usage",
                value = "${analytics.activeUsersPercentage.toInt()}%",
                modifier = Modifier.weight(1f)
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Most Used Features", style = MaterialTheme.typography.titleMedium)
                analytics.mostUsedFeatures.forEach { (feature, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(feature, style = MaterialTheme.typography.bodyMedium)
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = count.toString(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Organization Load", style = MaterialTheme.typography.titleMedium)
                analytics.organizationUsagePercentage.forEach { (org, percent) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(org, style = MaterialTheme.typography.labelSmall)
                            Text("${percent.toInt()}%", style = MaterialTheme.typography.labelSmall)
                        }
                        LinearProgressIndicator(
                            progress = { percent / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = "Tier: ${organization.subscriptionTier}",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                organization.subscriptionExpiresAt?.let { expiry ->
                    val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(expiry)
                    Text("Expires: $dateStr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }

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
    onConfirm: (String, String, SubscriptionTier, Date?, String, String, String, Map<String, String>) -> Unit
) {
    var name by remember(organization) { mutableStateOf(organization?.name ?: "") }
    var email by remember(organization) { mutableStateOf(organization?.contactEmail ?: "") }
    var tier by remember(organization) { mutableStateOf(organization?.subscriptionTier ?: com.google.refereeschedule.domain.model.SubscriptionTier.Free) }
    var printerModel by remember(organization) { mutableStateOf(organization?.printerModel ?: "Generic Thermal") }
    var printerIp by remember(organization) { mutableStateOf(organization?.printerIp ?: "") }
    var printerMac by remember(organization) { mutableStateOf(organization?.printerMacAddress ?: "") }
    var printerSettings by remember(organization) { mutableStateOf(organization?.printerSettings ?: emptyMap()) }
    
    // Default to July 31st of the current or next year
    val defaultExpiry = Calendar.getInstance().apply {
        val currentMonth = get(Calendar.MONTH)
        if (currentMonth > 6) { // August or later -> ends next year
            add(Calendar.YEAR, 1)
        }
        set(Calendar.MONTH, 6) // July
        set(Calendar.DAY_OF_MONTH, 31)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
    }.time
    
    var expiresAt by remember(organization) { mutableStateOf(organization?.subscriptionExpiresAt ?: defaultExpiry) }

    var tierExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (organization != null) "Edit Organization" else "Add Organization") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Organization Name") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Contact Email") }, modifier = Modifier.fillMaxWidth()) }
                
                // Tier Selector
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { tierExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Tier: ${tier.name}")
                        }
                        DropdownMenu(expanded = tierExpanded, onDismissRequest = { tierExpanded = false }) {
                            com.google.refereeschedule.domain.model.SubscriptionTier.entries.forEach { t ->
                                DropdownMenuItem(text = { Text(t.name) }, onClick = { tier = t; tierExpanded = false })
                            }
                        }
                    }
                }

                item { Text("Subscription Expires: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(expiresAt)}", style = MaterialTheme.typography.bodySmall) }
                
                item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                item { Text("Printer Settings", style = MaterialTheme.typography.titleSmall) }
                item { OutlinedTextField(value = printerModel, onValueChange = { printerModel = it }, label = { Text("Printer Model") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = printerIp, onValueChange = { printerIp = it }, label = { Text("Printer IP (Network)") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = printerMac, onValueChange = { printerMac = it }, label = { Text("Printer MAC (BT)") }, modifier = Modifier.fillMaxWidth()) }
                
                if (printerSettings.isNotEmpty()) {
                    item { Text("Advanced Settings", style = MaterialTheme.typography.labelSmall) }
                    printerSettings.forEach { (key, value) ->
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(key, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                OutlinedTextField(
                                    value = value,
                                    onValueChange = { newValue ->
                                        printerSettings = printerSettings.toMutableMap().apply { put(key, newValue) }
                                    },
                                    modifier = Modifier.weight(2f),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, email, tier, expiresAt, printerModel, printerIp, printerMac, printerSettings) }, enabled = name.isNotBlank() && email.isNotBlank()) {
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
