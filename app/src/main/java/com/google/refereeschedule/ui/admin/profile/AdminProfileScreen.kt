package com.google.refereeschedule.ui.admin.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.refereeschedule.domain.model.Organization
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProfileScreen(
    organizationId: String,
    viewModel: AdminProfileViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(organizationId) {
        viewModel.setOrganizationId(organizationId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Organization Profile") },
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
            uiState.organization?.let { organization ->
                AdminProfileContent(
                    organization = organization,
                    isScanning = uiState.isScanning,
                    isUploading = uiState.isUploading,
                    discoveredPrinters = uiState.discoveredPrinters,
                    scope = scope,
                    snackbarHostState = snackbarHostState,
                    onStartScan = { viewModel.startPrinterDiscovery() },
                    onUploadLogo = { viewModel.uploadLogo(it) },
                    onSave = { updatedOrg -> 
                        viewModel.updateOrganization(updatedOrg)
                        scope.launch {
                            snackbarHostState.showSnackbar("Organization settings saved to cloud.")
                        }
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProfileContent(
    organization: Organization,
    isScanning: Boolean,
    isUploading: Boolean = false,
    discoveredPrinters: List<DiscoveredPrinter>,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onStartScan: () -> Unit,
    onUploadLogo: (Uri) -> Unit,
    onSave: (Organization) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(organization.name) }
    var email by remember { mutableStateOf(organization.contactEmail) }
    var printerModel by remember { mutableStateOf(organization.printerModel) }
    var printerIp by remember { mutableStateOf(organization.printerIp) }
    var printerMac by remember { mutableStateOf(organization.printerMacAddress) }
    var printerSettings by remember { mutableStateOf(organization.printerSettings) }
    var timeZone by remember { mutableStateOf(organization.timeZone) }
    var themeColor by remember { mutableStateOf(organization.themeColor) }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onUploadLogo(it) }
    }

    var tzExpanded by remember { mutableStateOf(false) }
    val allTimeZones = remember { TimeZone.getAvailableIDs().sorted() }
    var tzSearch by remember { mutableStateOf("") }
    val filteredTimeZones = remember(tzSearch) {
        if (tzSearch.isBlank()) allTimeZones.take(50) 
        else allTimeZones.filter { it.contains(tzSearch, ignoreCase = true) }.take(50)
    }

    var isNetworkPrinter by remember { mutableStateOf(organization.printerIp.isNotEmpty() || organization.printerMacAddress.isEmpty()) }
    var showAdvancedSettings by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Organization Details", style = MaterialTheme.typography.titleLarge)
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Organization Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Contact Email") },
            modifier = Modifier.fillMaxWidth()
        )

        // Logo Upload Section
        Text("League Logo", style = MaterialTheme.typography.titleMedium)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (organization.logoUrl != null) {
                    AsyncImage(
                        model = organization.logoUrl,
                        contentDescription = "League Logo",
                        modifier = Modifier.size(120.dp).background(Color.White, MaterialTheme.shapes.small),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(Icons.Rounded.Business, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Text("No logo uploaded", style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = { logoPicker.launch("image/*") },
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (organization.logoUrl != null) "Change Logo" else "Upload Logo")
                    }
                }
            }
        }

        // Time Zone Selector
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = timeZone,
                onValueChange = { },
                readOnly = true,
                label = { Text("Organization Time Zone") },
                trailingIcon = {
                    IconButton(onClick = { tzExpanded = !tzExpanded }) {
                        Icon(if (tzExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable { tzExpanded = true }
            )
            
            DropdownMenu(
                expanded = tzExpanded,
                onDismissRequest = { tzExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 300.dp)
            ) {
                OutlinedTextField(
                    value = tzSearch,
                    onValueChange = { tzSearch = it },
                    placeholder = { Text("Search Timezone...") },
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    singleLine = true
                )
                
                filteredTimeZones.forEach { tz ->
                    DropdownMenuItem(
                        text = { Text(tz) },
                        onClick = {
                            timeZone = tz
                            tzExpanded = false
                            tzSearch = ""
                        }
                    )
                }
            }
        }

        HorizontalDivider()

        Text("App Branding", style = MaterialTheme.typography.titleLarge)
        Text("Select the primary background color for all league apps. Text will automatically adjust for contrast.", style = MaterialTheme.typography.bodySmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val colors = listOf("Default", "Red", "Blue", "Yellow")
            colors.forEach { color ->
                FilterChip(
                    selected = themeColor == color,
                    onClick = { themeColor = color },
                    label = { Text(color) }
                )
            }
        }

        HorizontalDivider()
        
        Text("Printer Configuration", style = MaterialTheme.typography.titleLarge)
        Text("Linking a printer allows the Game Day app to print items automatically using these cloud settings.", style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = printerModel,
            onValueChange = { printerModel = it },
            label = { Text("Printer Model Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Connection Type:", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("Bluetooth", style = MaterialTheme.typography.labelSmall)
            Switch(checked = isNetworkPrinter, onCheckedChange = { isNetworkPrinter = it })
            Text("Network", style = MaterialTheme.typography.labelSmall)
        }

        if (isNetworkPrinter) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = printerIp,
                    onValueChange = { printerIp = it },
                    label = { Text("Printer IP Address") },
                    placeholder = { Text("e.g. 192.168.1.100") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = onStartScan,
                    enabled = !isScanning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Searching Network...")
                    } else {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Scan for Network Printers")
                    }
                }

                if (discoveredPrinters.isNotEmpty()) {
                    Text("Discovered Devices (Tap + to select):", style = MaterialTheme.typography.labelSmall)
                    discoveredPrinters.forEach { printer ->
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp), 
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(printer.name, style = MaterialTheme.typography.bodyMedium)
                                    Text("IP: ${printer.ip}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                                IconButton(
                                    onClick = {
                                        printerIp = printer.ip
                                        printerModel = printer.name
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Linked: ${printer.name}")
                                        }
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Link Printer", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = printerMac,
                onValueChange = { printerMac = it },
                label = { Text("Bluetooth MAC Address") },
                placeholder = { Text("e.g. 00:11:22:33:44:55") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedCard(
            onClick = { showAdvancedSettings = !showAdvancedSettings },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Advanced Printer Settings", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Icon(
                        if (showAdvancedSettings) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
                
                if (showAdvancedSettings) {
                    Text("Customize how the thermal printer behaves on Game Day.", style = MaterialTheme.typography.bodySmall)
                    
                    printerSettings.forEach { (key, value) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = key.replace("_", " ").uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.weight(1f)
                            )
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
                    
                    TextButton(
                        onClick = { },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Add Custom Setting")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                isSaving = true
                onSave(organization.copy(
                    name = name, 
                    contactEmail = email,
                    printerModel = printerModel,
                    printerIp = if (isNetworkPrinter) printerIp else "",
                    printerMacAddress = if (!isNetworkPrinter) printerMac else "",
                    timeZone = timeZone,
                    themeColor = themeColor,
                    printerSettings = printerSettings
                ))
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Save Cloud Printer Link")
            }
        }
    }
}
