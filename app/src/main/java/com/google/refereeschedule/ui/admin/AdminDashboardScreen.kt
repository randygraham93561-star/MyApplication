package com.google.refereeschedule.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.refereeschedule.domain.model.*
import com.google.refereeschedule.ui.components.AppDrawer
import com.google.refereeschedule.util.TimeUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminDashboardViewModel,
    initialView: Int = 0,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToPrintQueue: () -> Unit,
    onEditReport: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onChatWithReferee: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onNavigateToCanvas: (PrintJobType) -> Unit,
    onNavigateToQuizBank: () -> Unit,
    onNavigateToScheduler: () -> Unit,
    onNavigateToDivisions: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToMessagingHub: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 0: DashboardHome, 1: OrgSetup, 2: RefSetup, 3: MatchSetup, 4: PrintSetup, 5: Standings
    var currentView by remember(initialView) { mutableIntStateOf(initialView) }
    
    var editingSeason by remember { mutableStateOf<Season?>(null) }
    var editingReferee by remember { mutableStateOf<RefereeProfile?>(null) }
    var showSeasonDialog by remember { mutableStateOf(false) }
    var showBroadcastDialog by remember { mutableStateOf(false) }
    var orgDropdownExpanded by remember { mutableStateOf(false) }

    // Bubble State
    var activeBubbleTitle by remember { mutableStateOf<String?>(null) }
    var bubbleContent by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                userRole = uiState.userRole,
                activeTier = uiState.activeTier,
                isSubscriptionExpired = uiState.isSubscriptionExpired,
                onNavigateToAdmin = { currentView = 0; scope.launch { drawerState.close() } },
                onNavigateToSystemAdmin = { /* MainActivity handles this */ },
                onNavigateToUserManagement = { /* MainActivity handles this */ },
                onNavigateToPrintTemplates = { /* MainActivity handles this */ },
                onNavigateToOrgSetup = { currentView = 1; scope.launch { drawerState.close() } },
                onNavigateToRefSetup = { currentView = 2; scope.launch { drawerState.close() } },
                onNavigateToMatchSetup = { currentView = 3; scope.launch { drawerState.close() } },
                onNavigateToPrintSetup = { currentView = 4; scope.launch { drawerState.close() } },
                onNavigateToStandings = { currentView = 5; scope.launch { drawerState.close() } },
                onNavigateToInbox = onNavigateToInbox,
                onNavigateToMessagingHub = onNavigateToMessagingHub,
                onNavigateToProfile = { onNavigateToProfile(uiState.selectedOrgId) },
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        val title = when(currentView) {
                            1 -> "Organization Set-up"
                            2 -> "Referee Setup"
                            3 -> "Match Set-up"
                            4 -> "Print Set-up"
                            5 -> "League Standings"
                            else -> "Admin Dashboard"
                        }
                        Text(title)
                    },
                    actions = {
                        if (uiState.managedOrganizations.size > 1) {
                            Box {
                                val selectedOrgName = uiState.managedOrganizations.find { it.id == uiState.selectedOrgId }?.name ?: "Admin Dashboard"
                                TextButton(onClick = { orgDropdownExpanded = true }) {
                                    Text(selectedOrgName, style = MaterialTheme.typography.titleLarge)
                                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(expanded = orgDropdownExpanded, onDismissRequest = { orgDropdownExpanded = false }) {
                                    uiState.managedOrganizations.forEach { org ->
                                        DropdownMenuItem(
                                            text = { Text(org.name) },
                                            onClick = {
                                                viewModel.selectOrganization(org.id)
                                                orgDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        IconButton(onClick = { showBroadcastDialog = true }) {
                            Icon(Icons.Rounded.Campaign, contentDescription = "Broadcast")
                        }
                    }
                )
            }
        ) { padding ->
            val contentModifier = Modifier.padding(padding).fillMaxSize()
            
            when (currentView) {
                0 -> DashboardHomeView(
                    uiState = uiState,
                    viewModel = viewModel,
                    onEditReport = onEditReport,
                    onNavigateToChat = onNavigateToChat,
                    modifier = contentModifier
                )
                1 -> OrganizationSetupHub(
                    uiState = uiState,
                    viewModel = viewModel,
                    onNavigateToDivisions = onNavigateToDivisions,
                    onEditSeason = { editingSeason = it; showSeasonDialog = true },
                    onMakeLive = { viewModel.markSeasonLive(it) },
                    onOpenBubble = { title, content ->
                        activeBubbleTitle = title
                        bubbleContent = content
                    },
                    modifier = contentModifier
                )
                2 -> RefereeSetupHub(
                    uiState = uiState,
                    viewModel = viewModel,
                    onEditReferee = { editingReferee = it },
                    onEditSeason = { editingSeason = it; showSeasonDialog = true },
                    onNavigateToQuizBank = onNavigateToQuizBank,
                    onNavigateToMessagingHub = onNavigateToMessagingHub,
                    onOpenBubble = { title, content ->
                        activeBubbleTitle = title
                        bubbleContent = content
                    },
                    modifier = contentModifier
                )
                3 -> MatchSetupHub(
                    uiState = uiState,
                    viewModel = viewModel,
                    onNavigateToScheduler = onNavigateToScheduler,
                    onEditReport = onEditReport,
                    onOpenBubble = { title, content ->
                        activeBubbleTitle = title
                        bubbleContent = content
                    },
                    modifier = contentModifier
                )
                4 -> PrintSetupHub(
                    uiState = uiState,
                    viewModel = viewModel,
                    onNavigateToPrintQueue = onNavigateToPrintQueue,
                    onNavigateToCanvas = onNavigateToCanvas,
                    onOpenBubble = { title, content ->
                        activeBubbleTitle = title
                        bubbleContent = content
                    },
                    modifier = contentModifier
                )
                5 -> LeagueStandingsHubView(
                    uiState = uiState,
                    modifier = contentModifier
                )
            }

            // POP-UP BUBBLE (Shared)
            activeBubbleTitle?.let { title ->
                AlertDialog(
                    onDismissRequest = { activeBubbleTitle = null; bubbleContent = null },
                    title = { Text(title) },
                    text = {
                        Box(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
                            bubbleContent?.invoke()
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { activeBubbleTitle = null; bubbleContent = null }) { Text("Done") }
                    }
                )
            }

            if (showSeasonDialog) {
                SeasonDialog(
                    season = editingSeason,
                    onDismiss = { 
                        showSeasonDialog = false
                        editingSeason = null
                    },
                    onConfirm = { updatedSeason ->
                        if (editingSeason != null) {
                            viewModel.updateSeason(updatedSeason)
                        } else {
                            viewModel.createSeason(updatedSeason)
                        }
                        showSeasonDialog = false
                        editingSeason = null
                    }
                )
            }

            if (editingReferee != null) {
                RefereeEditDialog(
                    profile = editingReferee!!,
                    teams = uiState.teams,
                    onDismiss = { editingReferee = null },
                    onConfirm = { 
                        viewModel.updateRefereeProfile(it)
                        editingReferee = null
                    }
                )
            }

            if (showBroadcastDialog) {
                BroadcastDialog(
                    onDismiss = { showBroadcastDialog = false },
                    onConfirm = { title, msg ->
                        viewModel.sendBroadcast(title, msg)
                        showBroadcastDialog = false
                    }
                )
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// HUB VIEWS
// ------------------------------------------------------------------------------------------------

@Composable
fun DashboardHomeView(
    uiState: AdminDashboardUiState,
    viewModel: AdminDashboardViewModel,
    onEditReport: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { StatsSection(uiState) }
        item { Text("Reports Needing Review", style = MaterialTheme.typography.titleMedium) }

        val pendingGames = uiState.gamesWithAssignments.filter { 
            it.first.status == GameStatus.PendingReview || it.first.status == GameStatus.NeedsRevision 
        }

        if (pendingGames.isEmpty()) {
            item { Text("No reports pending review.", style = MaterialTheme.typography.bodySmall) }
        } else {
            items(pendingGames) { (game, assignments) ->
                GameAdminCard(
                    game = game,
                    assignments = assignments,
                    onApproveReport = { viewModel.approveReport(game) },
                    onEditReport = { onEditReport(game.id) },
                    onPushBackReport = { feedback -> viewModel.pushBackReport(game, feedback) },
                    onChat = { onNavigateToChat(game.id) }
                )
            }
        }
    }
}

@Composable
fun LeagueStandingsHubView(
    uiState: AdminDashboardUiState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Text("Organization Standings", style = MaterialTheme.typography.headlineSmall) }
        val groupedTeams = uiState.teams.groupBy { "${it.divisionName} ${it.gender}" }
        groupedTeams.forEach { (group, teams) ->
            item {
                Text(group, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            }
            items(teams.sortedByDescending { it.totalPoints }) { team ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(team.name)
                        Text("${team.totalPoints} pts", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun OrganizationSetupHub(
    uiState: AdminDashboardUiState,
    viewModel: AdminDashboardViewModel,
    onNavigateToDivisions: () -> Unit,
    onEditSeason: (Season?) -> Unit,
    onMakeLive: (Season) -> Unit,
    onOpenBubble: (String, @Composable () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val org = uiState.managedOrganizations.find { it.id == uiState.selectedOrgId } ?: return
    val isAdmin = uiState.userRole == UserRole.Admin || uiState.userRole == UserRole.SystemAdmin
    
    LazyColumn(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isAdmin) {
            item { SetupOptionCard("League Branding", "Logo and App Colors", Icons.Rounded.Brush) { onOpenBubble("League Branding") { BrandingBubbleContent(org, viewModel) } } }
        }
        item { SetupOptionCard("League Divisions", "Manage age-group divisions", Icons.Rounded.Category) { onOpenBubble("League Divisions") { DivisionsBubbleContent(onNavigateToDivisions) } } }
        item { SetupOptionCard("Season Management", "Create or edit league seasons", Icons.Rounded.CalendarMonth) { 
            onOpenBubble("Season Settings") { 
                SeasonManagementBubbleContent(uiState, viewModel, onEditSeason, onMakeLive) 
            } 
        } }
        if (isAdmin) {
            item { SetupOptionCard("Printer Hardware", "IP and connection settings", Icons.Rounded.Print) { onOpenBubble("Hardware Settings") { PrinterHardwareBubbleContent(org, viewModel) } } }
        }
    }
}

@Composable
fun RefereeSetupHub(
    uiState: AdminDashboardUiState,
    viewModel: AdminDashboardViewModel,
    onEditReferee: (RefereeProfile) -> Unit,
    onEditSeason: (Season) -> Unit,
    onNavigateToQuizBank: () -> Unit,
    onNavigateToMessagingHub: () -> Unit,
    onOpenBubble: (String, @Composable () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SetupOptionCard("Messaging Hub", "Send messages to referees", Icons.Rounded.Campaign) { onNavigateToMessagingHub() } }
        item { SetupOptionCard("Points & Voucher Rules", "Configure season point values", Icons.Rounded.Star) { 
            onOpenBubble("Rules") { 
                val activeSeason = uiState.seasons.find { it.active }
                if (activeSeason != null) SeasonCard(season = activeSeason, onEdit = { onEditSeason(activeSeason) })
                else Text("No active season found.")
            }
        } }
        item { SetupOptionCard("Quiz Management", "Knowledge base and tests", Icons.Rounded.Quiz) { 
            onOpenBubble("Knowledge Base") { 
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Manage referee questions.", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onNavigateToQuizBank, modifier = Modifier.fillMaxWidth()) { Text("Open Quiz Bank") }
                }
            }
        } }
        item { SetupOptionCard("Referee Roster", "Manage all league referees", Icons.Rounded.People) { 
            onOpenBubble("Active Roster") { 
                var ageFilter by remember { mutableStateOf<String?>(null) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "Adult", "Youth").forEach { label ->
                            FilterChip(
                                selected = (ageFilter == null && label == "All") || ageFilter == label,
                                onClick = { ageFilter = if (label == "All") null else label },
                                label = { Text(label) }
                            )
                        }
                    }
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val filteredRoster = uiState.refereeProfiles.filter { 
                            when (ageFilter) {
                                "Adult" -> !it.isMinor
                                "Youth" -> it.isMinor
                                else -> true
                            }
                        }
                        items(filteredRoster) { profile -> RefereeAdminCard(profile) { onEditReferee(profile) } }
                    }
                }
            }
        } }
        item {
            val org = uiState.managedOrganizations.find { it.id == uiState.selectedOrgId }
            SetupOptionCard("Youth Restrictions", "Configure age gap requirements", Icons.Rounded.Security) { 
                if (org != null) {
                    onOpenBubble("Youth Referee Rules") { YouthRestrictionsBubbleContent(org, viewModel) }
                }
            }
        }
    }
}

@Composable
fun MatchSetupHub(
    uiState: AdminDashboardUiState,
    viewModel: AdminDashboardViewModel,
    onNavigateToScheduler: () -> Unit,
    onEditReport: (String) -> Unit,
    onOpenBubble: (String, @Composable () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SetupOptionCard("Team Management", "Assign teams to divisions", Icons.Rounded.Groups) { onOpenBubble("League Teams") { TeamBubbleContent(uiState, viewModel) } } }
        item { SetupOptionCard("Scheduling Hub", "Daily match calendar", Icons.Rounded.CalendarMonth) { 
            onOpenBubble("Scheduler") { 
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Access the scheduling environment.", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onNavigateToScheduler, modifier = Modifier.fillMaxWidth()) { Text("Open Hub") }
                }
            }
        } }
        item { SetupOptionCard("Match Reports", "Search and edit approved reports", Icons.Rounded.Description) { 
            onOpenBubble("Search Reports") { 
                ApprovedReportsSearchBubble(
                    uiState = uiState,
                    onSearch = { date, div, gen, team -> viewModel.searchApprovedReports(date, div, gen, team) },
                    onEdit = onEditReport
                )
            }
        } }
        item { SetupOptionCard("Game Day Finalization", "Final review and point awarding", Icons.Rounded.CheckCircle) { onOpenBubble("Processing") { FinalizeDaySection(viewModel) { viewModel.finalizeGameDay(it) } } } }
    }
}

@Composable
fun PrintSetupHub(
    uiState: AdminDashboardUiState,
    viewModel: AdminDashboardViewModel,
    onNavigateToPrintQueue: () -> Unit,
    onNavigateToCanvas: (PrintJobType) -> Unit,
    onOpenBubble: (String, @Composable () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val org = uiState.managedOrganizations.find { it.id == uiState.selectedOrgId } ?: return
    LazyColumn(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SetupOptionCard("Print Requirements", "Eligibility and reprint rules", Icons.AutoMirrored.Rounded.HelpOutline) { 
            onOpenBubble("Print Logic") { PrintLogicBubbleContent(org, viewModel, onNavigateToPrintQueue) } 
        } }
        item { SetupOptionCard("Preview Canvas", "View physical print layouts", Icons.Rounded.Brush) { 
            onOpenBubble("Preview Canvas") { 
                val isSystemAdmin = uiState.userRole == UserRole.SystemAdmin
                val isRefereeAdmin = uiState.userRole == UserRole.Admin
                val canView = isSystemAdmin || isRefereeAdmin
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!canView) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.extraSmall) { 
                            Text("RESTRICTED: Admin only.", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall) 
                        }
                    }
                    
                    PrintTemplateSummaryCard("Lunch Voucher", canView, isRefereeAdmin) { onNavigateToCanvas(PrintJobType.LunchVoucher) }
                    PrintTemplateSummaryCard("Schedule", canView, isRefereeAdmin) { onNavigateToCanvas(PrintJobType.MatchSchedule) }
                    PrintTemplateSummaryCard("Match Report", canView, isRefereeAdmin) { onNavigateToCanvas(PrintJobType.MatchReport) }
                    
                    if (isRefereeAdmin) {
                        Text(
                            "Note: You can preview these templates. Layout changes can only be made by System Admin.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        } }
    }
}

// ------------------------------------------------------------------------------------------------
// BUBBLE CONTENTS
// ------------------------------------------------------------------------------------------------

@Composable
fun BrandingBubbleContent(org: Organization, viewModel: AdminDashboardViewModel) {
    val isUploading by viewModel.isUploadingLogo.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> 
        uri?.let { viewModel.uploadLogo(it) } 
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("League Logo", style = MaterialTheme.typography.titleMedium)
        
        if (uploadError != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(uploadError!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.clearUploadError() }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (org.logoUrl != null) AsyncImage(model = org.logoUrl, contentDescription = "Logo", modifier = Modifier.size(100.dp).background(Color.White, MaterialTheme.shapes.small))
            else Icon(Icons.Rounded.Business, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
        }
        
        Button(onClick = { logoPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth(), enabled = !isUploading) {
            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Upload Logo")
            }
        }
        HorizontalDivider()
        Text("Primary Theme Color", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Default", "Red", "Blue", "Yellow", "Brown", "Black", "Orange", "Green").forEach { color ->
                FilterChip(selected = org.themeColor == color, onClick = { viewModel.updateOrganization(org.copy(themeColor = color)) }, label = { Text(color) } )
            }
        }
    }
}

@Composable
fun DivisionsBubbleContent(onNavigateToDivisions: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Configure your league's age group divisions (e.g. 10U, 12U).", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onNavigateToDivisions, modifier = Modifier.fillMaxWidth()) { Text("Open Manager") }
    }
}

@Composable
fun SeasonManagementBubbleContent(
    uiState: AdminDashboardUiState,
    viewModel: AdminDashboardViewModel,
    onEditSeason: (Season?) -> Unit,
    onMakeLive: (Season) -> Unit
) {
    val isProcessing by viewModel.isProcessingSeason.collectAsState()
    var seasonToConfirm by remember { mutableStateOf<Season?>(null) }
    
    val canMakeLive = uiState.userRole == UserRole.Admin || uiState.userRole == UserRole.SystemAdmin

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isProcessing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Updating seasons...", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        Button(
            onClick = { onEditSeason(null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isProcessing
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Create New Season")
        }

        Text("Existing Seasons", style = MaterialTheme.typography.titleMedium)

        if (uiState.seasons.isEmpty()) {
            Text("No seasons found.", style = MaterialTheme.typography.bodySmall)
        } else {
            uiState.seasons.forEach { season ->
                SeasonCard(
                    season = season,
                    onEdit = { onEditSeason(season) },
                    onMakeLive = if (canMakeLive) ({ seasonToConfirm = season }) else null
                )
            }
        }
    }

    if (seasonToConfirm != null) {
        AlertDialog(
            onDismissRequest = { seasonToConfirm = null },
            title = { Text("Confirm Season Activation") },
            text = { Text("Making '${seasonToConfirm!!.name}' LIVE will automatically archive any other active seasons. This will also clear the active team list for the new season pool. Continue?") },
            confirmButton = {
                Button(onClick = { 
                    onMakeLive(seasonToConfirm!!)
                    seasonToConfirm = null
                }) { Text("Yes, Make Live") }
            },
            dismissButton = {
                TextButton(onClick = { seasonToConfirm = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun PrinterHardwareBubbleContent(org: Organization, viewModel: AdminDashboardViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(value = org.printerIp, onValueChange = { viewModel.updateOrganization(org.copy(printerIp = it)) }, label = { Text("IP Address") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = org.printerMacAddress, onValueChange = { viewModel.updateOrganization(org.copy(printerMacAddress = it)) }, label = { Text("MAC Address") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun YouthRestrictionsBubbleContent(org: Organization, viewModel: AdminDashboardViewModel) {
    val reqs = org.youthRefereeRequirements
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Division Gap Requirements", style = MaterialTheme.typography.titleMedium)
        Text("Ensure youth referees are in a higher division than the players they officiate. (Each step represents 2 years/1 division)", style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = reqs.minDivisionGapHead.toString(),
            onValueChange = { v ->
                viewModel.updateOrganization(org.copy(youthRefereeRequirements = reqs.copy(minDivisionGapHead = v.toIntOrNull() ?: 2)))
            },
            label = { Text("Min Division Gap: Head Referee") },
            supportingText = { Text("Example: Gap of 2 means a 12U referee can Head 8U, but not 10U.") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        OutlinedTextField(
            value = reqs.minDivisionGapAR.toString(),
            onValueChange = { v ->
                viewModel.updateOrganization(org.copy(youthRefereeRequirements = reqs.copy(minDivisionGapAR = v.toIntOrNull() ?: 1)))
            },
            label = { Text("Min Division Gap: Assistant Referee") },
            supportingText = { Text("Example: Gap of 1 means a 12U referee can AR 10U.") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
fun TeamBubbleContent(uiState: AdminDashboardUiState, viewModel: AdminDashboardViewModel) {
    var showAddTeamDialog by remember { mutableStateOf(false) }
    val activeSeason = uiState.seasons.find { it.active }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { showAddTeamDialog = true }, modifier = Modifier.fillMaxWidth(), enabled = activeSeason != null) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Add Team ID") }
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.teams) { team ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) { 
                            Text(team.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${team.divisionName} ${team.gender}${team.subDivision?.let { " | $it" } ?: ""}", 
                                style = MaterialTheme.typography.labelSmall
                            )
                            if (team.coachEmail != null) {
                                Text("Coach: ${team.coachEmail}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        IconButton(onClick = { viewModel.deleteTeam(team.id) }) { Icon(Icons.Rounded.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
    if (showAddTeamDialog && activeSeason != null) {
        AddTeamToDivisionDialog(activeSeason.divisions, { showAddTeamDialog = false }) { id, div, gen, sub, email -> 
            viewModel.addTeamToDivision(id, div, gen, sub, email)
            showAddTeamDialog = false 
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovedReportsSearchBubble(
    uiState: AdminDashboardUiState,
    onSearch: (Date, String?, String?, String?) -> Unit,
    onEdit: (String) -> Unit
) {
    var date by remember { mutableStateOf(Date()) }
    var division by remember { mutableStateOf<String?>(null) }
    var gender by remember { mutableStateOf<String?>(null) }
    var teamId by remember { mutableStateOf("") }
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date.time)

    var divExpanded by remember { mutableStateOf(false) }
    val divisions = uiState.seasons.find { it.active }?.divisions?.map { it.name } ?: emptyList()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(date)
            Text("Date: $dateStr")
        }

        Box {
            OutlinedButton(onClick = { divExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(division ?: "All Divisions")
            }
            DropdownMenu(divExpanded, { divExpanded = false }) {
                DropdownMenuItem(text = { Text("All Divisions") }, onClick = { division = null; divExpanded = false })
                divisions.forEach { div ->
                    DropdownMenuItem(text = { Text(div) }, onClick = { division = div; divExpanded = false })
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Boys", "Girls", "Coed").forEach { g ->
                FilterChip(selected = gender == g, onClick = { gender = if (gender == g) null else g }, label = { Text(g) })
            }
        }

        OutlinedTextField(value = teamId, onValueChange = { teamId = it }, label = { Text("Team ID/Name (Opt)") }, modifier = Modifier.fillMaxWidth())

        Button(onClick = { onSearch(date, division, gender, teamId.ifBlank { null }) }, modifier = Modifier.fillMaxWidth()) {
            Text("Search")
        }

        HorizontalDivider()

        if (uiState.approvedReportsSearchResults.isEmpty()) {
            Text("No results.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            uiState.approvedReportsSearchResults.forEach { (game, _) ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onEdit(game.id) }) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${game.homeTeamName} vs ${game.awayTeamName}", fontWeight = FontWeight.Bold)
                        Text("${game.ageGroup} ${game.gender} | ${TimeUtils.formatTo12h(game.time)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
            TextButton(onClick = {
                date = Date(datePickerState.selectedDateMillis ?: date.time)
                showDatePicker = false
            }) { Text("OK") }
        }) { DatePicker(datePickerState) }
    }
}

@Composable
fun PrintLogicBubbleContent(org: Organization, viewModel: AdminDashboardViewModel, onPrintQueue: () -> Unit) {
    val reqs = org.printRequirements
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
        Button(onClick = onPrintQueue, modifier = Modifier.fillMaxWidth()) { Text("Open Print Queue") }
        
        Text("1. Lunch Vouchers", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        reqs.lunchVouchers.forEachIndexed { index, voucher ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(voucher.label, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = voucher.assignedGamesRequired.toString(), onValueChange = { v ->
                            val newList = reqs.lunchVouchers.toMutableList().apply { this[index] = voucher.copy(assignedGamesRequired = v.toIntOrNull() ?: 0) }
                            viewModel.updateOrganization(org.copy(printRequirements = reqs.copy(lunchVouchers = newList)))
                        }, label = { Text("Assigned") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = voucher.completedGamesRequired.toString(), onValueChange = { v ->
                            val newList = reqs.lunchVouchers.toMutableList().apply { this[index] = voucher.copy(completedGamesRequired = v.toIntOrNull() ?: 0) }
                            viewModel.updateOrganization(org.copy(printRequirements = reqs.copy(lunchVouchers = newList)))
                        }, label = { Text("Completed") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    OutlinedTextField(value = voucher.maxPrints.toString(), onValueChange = { v ->
                        val newList = reqs.lunchVouchers.toMutableList().apply { this[index] = voucher.copy(maxPrints = v.toIntOrNull() ?: 1) }
                        viewModel.updateOrganization(org.copy(printRequirements = reqs.copy(lunchVouchers = newList)))
                    }, label = { Text("Max Prints") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            }
        }
        TextButton(onClick = {
            val newList = reqs.lunchVouchers + LunchVoucherRequirement(label = "Additional Voucher")
            viewModel.updateOrganization(org.copy(printRequirements = reqs.copy(lunchVouchers = newList)))
        }) { Icon(Icons.Rounded.Add, null); Text("Add Voucher Level") }

        HorizontalDivider()
        Text("2. Match Schedule", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(value = reqs.matchSchedule.maxPrints.toString(), onValueChange = { v ->
            viewModel.updateOrganization(org.copy(printRequirements = reqs.copy(matchSchedule = reqs.matchSchedule.copy(maxPrints = v.toIntOrNull() ?: 3))))
        }, label = { Text("Max Schedule Prints per Referee") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

        HorizontalDivider()
        Text("3. Match Reports", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = reqs.matchReport.headRefereeOnly, onCheckedChange = { v -> viewModel.updateOrganization(org.copy(printRequirements = reqs.copy(matchReport = reqs.matchReport.copy(headRefereeOnly = v)))) })
            Text("Head Referee assignments only", style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = reqs.matchReport.includeReferenceSheet, onCheckedChange = { v -> viewModel.updateOrganization(org.copy(printRequirements = reqs.copy(matchReport = reqs.matchReport.copy(includeReferenceSheet = v)))) })
            Text("Include Separated Reference Sheet", style = MaterialTheme.typography.bodySmall)
        }
        if (reqs.matchReport.includeReferenceSheet) {
            Row(modifier = Modifier.padding(start = 32.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Reprint Mode:", style = MaterialTheme.typography.labelSmall)
                listOf("Once", "EveryTime").forEach { mode ->
                    FilterChip(selected = reqs.matchReport.referenceSheetMode == mode, onClick = { viewModel.updateOrganization(org.copy(printRequirements = reqs.copy(matchReport = reqs.matchReport.copy(referenceSheetMode = mode)))) }, label = { Text(if (mode == "Once") "Once" else "Every Request") })
                }
            }
        }
        
        HorizontalDivider()
        Text("4. Paper Width", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(value = org.printerSettings["paper_width"] ?: "80mm", onValueChange = { v -> viewModel.updateOrganization(org.copy(printerSettings = org.printerSettings.toMutableMap().apply { put("paper_width", v) })) }, label = { Text("Required Width (e.g. 80mm)") }, modifier = Modifier.fillMaxWidth())
    }
}

// ------------------------------------------------------------------------------------------------
// SHARED COMPONENTS
// ------------------------------------------------------------------------------------------------

@Composable
fun SetupOptionCard(title: String, description: String, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) { Icon(icon, null, modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            Column { Text(title, style = MaterialTheme.typography.titleMedium); Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
        }
    }
}

@Composable
fun StatsSection(uiState: AdminDashboardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Total Games", uiState.totalGames.toString(), Modifier.weight(1f))
            StatCard("Fully Covered", uiState.fullyCovered.toString(), Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Missing Refs", uiState.missingRefs.toString(), Modifier.weight(1f))
            StatCard("Pending Appr.", uiState.pendingApprovals.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
fun GameAdminCard(game: Game, assignments: List<Assignment>, onApproveReport: () -> Unit, onEditReport: () -> Unit, onPushBackReport: (String) -> Unit, onChat: () -> Unit) {
    var showPushBack by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${game.homeTeamName} vs ${game.awayTeamName}", style = MaterialTheme.typography.titleMedium)
            Text("Crew: ${assignments.size}/${game.requiredCrewSize} | Status: ${game.status}", style = MaterialTheme.typography.bodySmall)
            if (game.status == GameStatus.PendingReview || game.status == GameStatus.NeedsRevision) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onApproveReport, modifier = Modifier.weight(1f)) { Text("Approve") }
                    Button(onEditReport, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Edit") }
                }
                OutlinedButton({ showPushBack = true }, Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Push Back") }
                OutlinedButton(onChat, Modifier.fillMaxWidth()) { Icon(Icons.AutoMirrored.Rounded.Chat, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Chat with Crew") }
            }
        }
    }
    if (showPushBack) AlertDialog({ showPushBack = false }, title = { Text("Push Back") }, text = { OutlinedTextField(feedback, { feedback = it }, label = { Text("Feedback") }, modifier = Modifier.fillMaxWidth()) }, confirmButton = { Button({ onPushBackReport(feedback); showPushBack = false }, enabled = feedback.isNotBlank()) { Text("Confirm") } }, dismissButton = { TextButton({ showPushBack = false }) { Text("Cancel") } })
}

@Composable
fun RefereeAdminCard(profile: RefereeProfile, onEdit: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(profile.name, style = MaterialTheme.typography.titleMedium)
                    Surface(
                        color = if (profile.isMinor) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = if (profile.isMinor) "YOUTH" else "ADULT",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Text("Badge: ${profile.badgeLevel} | Points: ${profile.totalPoints}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onEdit) { Icon(Icons.Rounded.Edit, "Edit") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefereeEditDialog(profile: RefereeProfile, teams: List<Team>, onDismiss: () -> Unit, onConfirm: (RefereeProfile) -> Unit) {
    var name by remember { mutableStateOf(profile.name) }
    var dob by remember { mutableStateOf(profile.dateOfBirth) }
    var badgeLevel by remember { mutableStateOf(profile.badgeLevel) }
    var headComfort by remember { mutableFloatStateOf(profile.headRefereeComfortLevel.toFloat()) }
    var badgeExpanded by remember { mutableStateOf(false) }

    var showDobPicker by remember { mutableStateOf(false) }
    val dobState = rememberDatePickerState(initialSelectedDateMillis = dob?.time)

    val badges = listOf("Regional", "Intermediate", "Advanced", "National")
    val minLevel = remember(badgeLevel) { DivisionDifficulty.getMinLevelForBadge(badgeLevel) }
    LaunchedEffect(minLevel) { if (headComfort < minLevel) headComfort = minLevel.toFloat() }

    AlertDialog(onDismiss, title = { Text("Edit Referee") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth()) }
            
            item {
                OutlinedButton(onClick = { showDobPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    val dobStr = dob?.let { 
                        SimpleDateFormat("MMM dd, yyyy", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }.format(it) 
                    } ?: "Set Date of Birth"
                    Text(dobStr)
                }
            }

            item {
                ExposedDropdownMenuBox(badgeExpanded, { badgeExpanded = it }) {
                    OutlinedTextField(badgeLevel, {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(badgeExpanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                    ExposedDropdownMenu(badgeExpanded, { badgeExpanded = false }) { badges.forEach { b -> DropdownMenuItem(text = { Text(b) }, onClick = { badgeLevel = b; badgeExpanded = false }) } }
                }
            }
            item { Text("Head Comfort: ${headComfort.toInt()}"); Slider(headComfort, { headComfort = it }, valueRange = minLevel.toFloat()..9f, steps = (9 - minLevel).coerceAtLeast(0).toInt()) }
        }

        if (showDobPicker) {
            DatePickerDialog(onDismissRequest = { showDobPicker = false }, confirmButton = {
                TextButton(onClick = {
                    dob = dobState.selectedDateMillis?.let { Date(it) }
                    showDobPicker = false
                }) { Text("OK") }
            }) { DatePicker(dobState) }
        }
    }, confirmButton = { TextButton({ onConfirm(profile.copy(name = name, dateOfBirth = dob, badgeLevel = badgeLevel, headRefereeComfortLevel = headComfort.toInt())) }) { Text("Save") } }, dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}

@Composable
fun SeasonDialog(season: Season? = null, onDismiss: () -> Unit, onConfirm: (Season) -> Unit) {
    var name by remember(season) { mutableStateOf(season?.name ?: "") }
    var collectPoints by remember(season) { mutableStateOf(season?.collectPoints ?: true) }
    var maxPoints by remember(season) { mutableStateOf(season?.maxPointsPerWeekend?.toString() ?: "2") }
    val divisions = remember(season) { mutableStateListOf<Division>().apply { addAll(season?.divisions ?: emptyList()) } }

    AlertDialog(onDismiss, title = { Text(if (season != null) "Edit Season" else "New Season") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { OutlinedTextField(name, { name = it }, label = { Text("Season Name") }, modifier = Modifier.fillMaxWidth()) }
            item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Collect Points"); Spacer(Modifier.weight(1f)); Switch(collectPoints, { collectPoints = it }) } }
            if (collectPoints) item { OutlinedTextField(maxPoints, { maxPoints = it }, label = { Text("Max Weekly Points") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) }
            items(divisions.size) { i -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextField(divisions[i].name, { divisions[i] = divisions[i].copy(name = it) }, modifier = Modifier.weight(1f)); IconButton({ divisions.removeAt(i) }) { Icon(Icons.Rounded.Delete, null) } } }
            item { TextButton({ divisions.add(Division(teamsAccumulatePoints = true)) }) { Icon(Icons.Rounded.Add, null); Text("Add Division") } }
        }
    }, confirmButton = { TextButton({ onConfirm((season ?: Season()).copy(name = name, collectPoints = collectPoints, maxPointsPerWeekend = maxPoints.toIntOrNull() ?: 0, divisions = divisions.toList())) }, enabled = name.isNotBlank()) { Text("Save") } }, dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}

@Composable
fun SeasonCard(
    season: Season,
    onMakeLive: (() -> Unit)? = null,
    onEdit: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(season.name, style = MaterialTheme.typography.titleMedium)
                val statusText = when {
                    season.archivedAt != null -> "ARCHIVED"
                    season.live -> "LIVE"
                    else -> "DRAFT"
                }
                val statusColor = when (statusText) {
                    "LIVE" -> MaterialTheme.colorScheme.primary
                    "ARCHIVED" -> MaterialTheme.colorScheme.outline
                    else -> MaterialTheme.colorScheme.secondary
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (!season.live && season.archivedAt == null && onMakeLive != null) {
                Button(onClick = onMakeLive) {
                    Text("Make Live")
                }
            }
            
            IconButton(onEdit) { Icon(Icons.Rounded.Edit, "Edit") }
        }
    }
}

@Composable
fun BroadcastDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var t by remember { mutableStateOf("") }; var m by remember { mutableStateOf("") }
    AlertDialog(onDismiss, title = { Text("Broadcast") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(t, { t = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(m, { m = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth(), minLines = 3) } }, confirmButton = { Button({ onConfirm(t, m) }, enabled = t.isNotBlank() && m.isNotBlank()) { Text("Send") } }, dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalizeDaySection(viewModel: AdminDashboardViewModel, onFinalize: (Date) -> Unit) {
    var showD by remember { mutableStateOf(false) }; var sd by remember { mutableStateOf(Date()) }
    val status by viewModel.distributeStatus.collectAsState()
    val dps = rememberDatePickerState(initialSelectedDateMillis = sd.time)
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Finalize Day", style = MaterialTheme.typography.titleMedium)
            OutlinedButton({ showD = true }, Modifier.fillMaxWidth()) { 
                val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(sd)
                Text("Date: $dateStr") 
            }
            Button({ onFinalize(sd) }, Modifier.fillMaxWidth()) { Text("Award Points") }
            status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
    if (showD) DatePickerDialog({ showD = false }, confirmButton = { TextButton({ sd = Date(dps.selectedDateMillis ?: sd.time); showD = false }) { Text("OK") } }) { DatePicker(dps) }
}

@Composable
fun AddTeamToDivisionDialog(
    divisions: List<Division>, 
    onDismiss: () -> Unit, 
    onConfirm: (String, String, String, String?, String?) -> Unit
) {
    var teamNumber by remember { mutableStateOf("") }
    var selectedDiv by remember { mutableStateOf(divisions.firstOrNull()?.name ?: "12U") }
    var selectedGender by remember { mutableStateOf("Boys") }
    
    var divExpanded by remember { mutableStateOf(false) }

    val generatedId = remember(selectedDiv, selectedGender, teamNumber) {
        if (selectedDiv.isNotEmpty() && selectedGender.isNotEmpty() && teamNumber.isNotEmpty()) {
            val genChar = selectedGender.first().uppercase()
            val formattedNum = if (teamNumber.length == 1) "0$teamNumber" else teamNumber
            "$selectedDiv$genChar-$formattedNum"
        } else {
            "----"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { Text("Add Team (ID Builder)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Preview: $generatedId",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { divExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedDiv)
                        }
                        DropdownMenu(expanded = divExpanded, onDismissRequest = { divExpanded = false }) {
                            val ageGroups = listOf("8U", "10U", "12U", "14U", "16U", "19U")
                            ageGroups.forEach { age ->
                                DropdownMenuItem(text = { Text(age) }, onClick = { selectedDiv = age; divExpanded = false })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = teamNumber,
                        onValueChange = { if (it.length <= 2) teamNumber = it.filter { c -> c.isDigit() } },
                        label = { Text("Number") },
                        placeholder = { Text("01") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Boys", "Girls", "Coed").forEach { g ->
                        FilterChip(
                            selected = selectedGender == g, 
                            onClick = { selectedGender = g }, 
                            label = { Text(g) }
                        )
                    }
                }
            }
        },
        confirmButton = { 
            Button(
                onClick = { 
                    onConfirm(generatedId, selectedDiv, selectedGender, null, null) 
                }, 
                enabled = generatedId != "----"
            ) { 
                Text("Add Team") 
            } 
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PrintTemplateSummaryCard(title: String, enabled: Boolean, isReadOnly: Boolean = false, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = if (enabled) 1f else 0.5f }, onClick = { if (enabled) onClick() }) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (enabled) {
                IconButton(onClick) { 
                    Icon(
                        imageVector = if (isReadOnly) Icons.Rounded.Visibility else Icons.Rounded.Brush, 
                        contentDescription = if (isReadOnly) "Preview" else "Edit"
                    ) 
                }
            } else Icon(Icons.Rounded.Lock, "Restricted")
        }
    }
}
