package com.google.refereeschedule.ui.admin.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.refereeschedule.domain.model.QuizQuestion
import com.google.refereeschedule.ui.admin.AdminDashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizManagementScreen(
    viewModel: AdminDashboardViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddQuestion by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val activeSeason = uiState.seasons.find { it.active }
    val quiz = uiState.activeSeasonQuiz

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiz Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (activeSeason != null) {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Quiz Settings")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddQuestion = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Question")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (activeSeason != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Current Season Quiz", style = MaterialTheme.typography.titleMedium)
                            Text("Season: ${activeSeason.name}", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Passing Grade", style = MaterialTheme.typography.labelSmall)
                                    Text("${quiz?.passingPercentage ?: 80}%", fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Quiz Length", style = MaterialTheme.typography.labelSmall)
                                    Text("${quiz?.quizLength ?: 10} questions", fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Pool Size", style = MaterialTheme.typography.labelSmall)
                                    Text("${quiz?.questionIds?.size ?: 0} questions", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Question Pool", style = MaterialTheme.typography.headlineSmall)
                Text("Select questions to include in this season's randomized quiz.", style = MaterialTheme.typography.bodySmall)
            }

            items(uiState.globalQuizQuestions) { question ->
                val isSelected = quiz?.questionIds?.contains(question.id) == true
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (activeSeason != null) {
                            val currentIds = quiz?.questionIds ?: emptyList()
                            val newIds = if (isSelected) currentIds - question.id else currentIds + question.id
                            viewModel.configureSeasonQuiz(
                                seasonId = activeSeason.id,
                                questionIds = newIds,
                                quizLength = quiz?.quizLength ?: 10,
                                passingPercentage = quiz?.passingPercentage ?: 80
                            )
                        }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(question.text, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            question.options.forEachIndexed { index, option ->
                                Text(
                                    text = "${index + 1}. $option",
                                    color = if (index == question.correctAnswerIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (index == question.correctAnswerIndex) FontWeight.Bold else null,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        if (showSettings && activeSeason != null) {
            QuizSettingsDialog(
                currentLength = quiz?.quizLength ?: 10,
                currentPassing = quiz?.passingPercentage ?: 80,
                onDismiss = { showSettings = false },
                onConfirm = { length, passing ->
                    viewModel.configureSeasonQuiz(
                        seasonId = activeSeason.id,
                        questionIds = quiz?.questionIds ?: emptyList(),
                        quizLength = length,
                        passingPercentage = passing
                    )
                    showSettings = false
                }
            )
        }

        if (showAddQuestion) {
            AddQuestionDialog(
                onDismiss = { showAddQuestion = false },
                onConfirm = { text, opts, correct ->
                    viewModel.createQuizQuestion(text, opts, correct)
                    showAddQuestion = false
                }
            )
        }
    }
}

@Composable
fun QuizSettingsDialog(
    currentLength: Int,
    currentPassing: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var length by remember { mutableStateOf(currentLength.toString()) }
    var passing by remember { mutableStateOf(currentPassing.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Season Quiz Parameters") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = length,
                    onValueChange = { length = it },
                    label = { Text("Quiz Length (Questions to show)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("How many random questions from the pool should be asked?") }
                )
                OutlinedTextField(
                    value = passing,
                    onValueChange = { passing = it },
                    label = { Text("Passing Grade (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("%") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(length.toIntOrNull() ?: 10, passing.toIntOrNull() ?: 80)
            }) {
                Text("Save Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddQuestionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>, Int) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var opt1 by remember { mutableStateOf("") }
    var opt2 by remember { mutableStateOf("") }
    var opt3 by remember { mutableStateOf("") }
    var opt4 by remember { mutableStateOf("") }
    var correctIndex by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Multiple Choice Question") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Question Text") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = opt1, onValueChange = { opt1 = it }, label = { Text("Option 1") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = opt2, onValueChange = { opt2 = it }, label = { Text("Option 2") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = opt3, onValueChange = { opt3 = it }, label = { Text("Option 3") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = opt4, onValueChange = { opt4 = it }, label = { Text("Option 4") }, modifier = Modifier.fillMaxWidth())
                
                Text("Correct Answer:", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    (0..3).forEach { i ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = correctIndex == i, onClick = { correctIndex = i })
                            Text("${i + 1}")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                onConfirm(text, listOf(opt1, opt2, opt3, opt4), correctIndex)
            }, enabled = text.isNotBlank() && opt1.isNotBlank() && opt2.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
