package com.google.refereeschedule.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.refereeschedule.domain.model.DivisionDifficulty
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinish: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    
    // Form State
    var refereeName by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf<Date?>(null) }
    var selectedOrgId by remember { mutableStateOf("") }
    var selectedTeamIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var headComfort by remember { mutableIntStateOf(0) }
    var assistantComfort by remember { mutableIntStateOf(0) }
    var isNewReferee by remember { mutableStateOf<Boolean?>(null) }
    var hasTakenCourse by remember { mutableStateOf<Boolean?>(null) }

    val isReturningReferee = remember(uiState.profile) {
        uiState.profile?.isOnboardingCompleted == true
    }

    var step by remember(isReturningReferee) { 
        mutableIntStateOf(1) 
    }

    // Initialize from existing profile
    LaunchedEffect(uiState.profile) {
        uiState.profile?.let { profile ->
            refereeName = profile.name
            dateOfBirth = profile.dateOfBirth
            selectedOrgId = profile.organizationId
            if (profile.organizationId.isNotEmpty()) {
                viewModel.selectOrganization(profile.organizationId)
            }
            headComfort = profile.headRefereeComfortLevel
            assistantComfort = profile.assistantRefereeComfortLevel
            // If it's a new season, we don't pre-fill teams so they HAVE to pick new ones
            if (!isReturningReferee) {
                selectedTeamIds = profile.teamIdsForPoints
            }
        }
    }

    var orgExpanded by remember { mutableStateOf(false) }
    var teamExpanded by remember { mutableStateOf(false) }
    var showDobPicker by remember { mutableStateOf(false) }
    val dobState = rememberDatePickerState()

    val isActuallyLocked = uiState.isAccountLocked || (isNewReferee == true && hasTakenCourse == false && step > 6)

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isActuallyLocked) {
                LockScreen(onFinish)
            } else {
                when (step) {
                    1 -> {
                        OnboardingHeader("Your Name", "Please enter your full name as you'd like it to appear on match reports.")
                        OutlinedTextField(
                            value = refereeName,
                            onValueChange = { refereeName = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showDobPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val dobStr = dateOfBirth?.let { 
                                SimpleDateFormat("MMM dd, yyyy", Locale.US).apply {
                                    timeZone = TimeZone.getTimeZone("UTC")
                                }.format(it) 
                            } ?: "Select Date of Birth"
                            Text(dobStr)
                        }

                        if (showDobPicker) {
                            DatePickerDialog(
                                onDismissRequest = { showDobPicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        dateOfBirth = dobState.selectedDateMillis?.let { Date(it) }
                                        showDobPicker = false
                                    }) { Text("OK") }
                                }
                            ) {
                                DatePicker(state = dobState)
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { 
                                if (isReturningReferee) step = 3 // Skip Org for returning
                                else step = 2 
                            },
                            enabled = refereeName.isNotBlank() && dateOfBirth != null,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Next") }
                    }
                    2 -> {
                        OnboardingHeader("Welcome!", "Next, let's get you connected to your league.")
                        ExposedDropdownMenuBox(
                            expanded = orgExpanded,
                            onExpandedChange = { orgExpanded = it }
                        ) {
                            val orgName = uiState.organizations.find { it.id == selectedOrgId }?.name ?: "Select Organization"
                            OutlinedTextField(
                                value = orgName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Organization") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orgExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = orgExpanded, onDismissRequest = { orgExpanded = false }) {
                                uiState.organizations.forEach { org ->
                                    DropdownMenuItem(
                                        text = { Text(org.name) },
                                        onClick = { 
                                            selectedOrgId = org.id
                                            viewModel.selectOrganization(org.id)
                                            orgExpanded = false 
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { step = 3 },
                            enabled = selectedOrgId.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Next") }
                    }
                    3 -> {
                        OnboardingHeader("Points", "Select up to 3 teams you are officiating for this season.")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedTeamIds.forEachIndexed { index, id ->
                                val team = uiState.teams.find { it.id == id }
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${team?.name} (${team?.divisionName} ${team?.gender})", modifier = Modifier.weight(1f))
                                        IconButton(onClick = { 
                                            selectedTeamIds = selectedTeamIds.toMutableList().apply { removeAt(index) }
                                        }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Remove")
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))

                        if (selectedTeamIds.size < 3) {
                            ExposedDropdownMenuBox(
                                expanded = teamExpanded,
                                onExpandedChange = { teamExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = "",
                                    onValueChange = {},
                                    readOnly = true,
                                    placeholder = { Text("Add a team...") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teamExpanded) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = teamExpanded, onDismissRequest = { teamExpanded = false }) {
                                    val available = uiState.teams.filter { it.organizationId == selectedOrgId && it.id !in selectedTeamIds }
                                    available.forEach { team ->
                                        DropdownMenuItem(
                                            text = { Text("${team.name} (${team.divisionName} ${team.gender})") },
                                            onClick = { 
                                                if (selectedTeamIds.size < 3) selectedTeamIds = selectedTeamIds + team.id
                                                teamExpanded = false 
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { step = 4 }, 
                            enabled = selectedTeamIds.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Next") }
                    }
                    4 -> {
                        OnboardingHeader("Comfort Levels", "What is the highest level you feel comfortable officiating?")
                        Text("Head Referee: ${DivisionDifficulty.headRefereeLevels[headComfort]}", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = headComfort.toFloat(),
                            onValueChange = { headComfort = it.toInt() },
                            valueRange = 0f..9f,
                            steps = 8
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Assistant Referee: ${DivisionDifficulty.headRefereeLevels[assistantComfort]}", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = assistantComfort.toFloat(),
                            onValueChange = { assistantComfort = it.toInt() },
                            valueRange = 0f..9f,
                            steps = 8
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { 
                                if (isReturningReferee) {
                                    viewModel.submitOnboarding(refereeName, dateOfBirth, selectedOrgId, selectedTeamIds, headComfort, assistantComfort, false, true) {
                                        onFinish()
                                    }
                                } else {
                                    step = 5 
                                }
                            }, 
                            enabled = !uiState.isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) { 
                            if (uiState.isSaving) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            else Text(if (isReturningReferee) "Submit" else "Next") 
                        }
                    }
                    5 -> {
                        OnboardingHeader("Experience", "Are you a brand new referee?")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = isNewReferee == true, onClick = { isNewReferee = true })
                            Text("Yes, I'm new")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = isNewReferee == false, onClick = { isNewReferee = false })
                            Text("No, I have experience")
                        }
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { 
                                if (isNewReferee == true) step = 6 
                                else {
                                    viewModel.submitOnboarding(refereeName, dateOfBirth, selectedOrgId, selectedTeamIds, headComfort, assistantComfort, false, true) {
                                        onFinish()
                                    }
                                }
                            },
                            enabled = isNewReferee != null && !uiState.isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) { 
                            if (uiState.isSaving) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            else Text("Next") 
                        }
                    }
                    6 -> {
                        OnboardingHeader("Referee Course", "Have you taken the required referee course yet?")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = hasTakenCourse == true, onClick = { hasTakenCourse = true })
                            Text("Yes, I completed it")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = hasTakenCourse == false, onClick = { hasTakenCourse = false })
                            Text("No, not yet")
                        }
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { 
                                viewModel.submitOnboarding(refereeName, dateOfBirth, selectedOrgId, selectedTeamIds, headComfort, assistantComfort, true, hasTakenCourse!!) {
                                    if (hasTakenCourse == true) onFinish()
                                    else step = 7 // Show lock screen
                                }
                            },
                            enabled = hasTakenCourse != null && !uiState.isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) { 
                            if (uiState.isSaving) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            else Text("Submit") 
                        }
                    }
                    7 -> {
                        LockScreen(onFinish)
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingHeader(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary)
    Spacer(Modifier.height(32.dp))
}

@Composable
fun LockScreen(onFinish: () -> Unit) {
    Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
    Spacer(Modifier.height(24.dp))
    Text("Account Pending", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(16.dp))
    Text(
        "Your account is currently blocked because you haven't taken the referee course yet. Please reach out to your Referee Admin for information on how to complete the course.",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge
    )
    Spacer(Modifier.height(32.dp))
    Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
        Text("Done")
    }
}
