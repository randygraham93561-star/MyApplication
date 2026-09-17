package com.google.refereeschedule.ui.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.refereeschedule.domain.model.QuizAttempt
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefresherQuizScreen(
    seasonId: String,
    viewModel: RefresherQuizViewModel,
    onFinish: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val answers = remember { mutableStateMapOf<String, Int>() }

    LaunchedEffect(seasonId) {
        viewModel.loadQuiz(seasonId)
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    if (uiState.quiz == null) {
        NoQuizAvailable(onFinish)
        return
    }

    if (uiState.lockoutRemainingMillis > 0) {
        LockoutScreen(uiState.lockoutRemainingMillis)
        return
    }

    if (uiState.isFinished) {
        ResultScreen(
            attempt = uiState.lastAttempt!!,
            required = uiState.quiz!!.passingPercentage,
            onFinish = onFinish,
            onRetry = {
                answers.clear()
                viewModel.resetQuiz()
            }
        )
        return
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Season Refresher Quiz") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                "Complete this quiz to unlock the new season. Required score: ${uiState.quiz!!.passingPercentage}%",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(uiState.questions) { question ->
                    Column {
                        Text(question.text, style = MaterialTheme.typography.titleMedium)
                        question.options.forEachIndexed { index, option ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = answers[question.id] == index,
                                    onClick = { answers[question.id] = index }
                                )
                                Text(option)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.submitQuiz(answers) },
                modifier = Modifier.fillMaxWidth(),
                enabled = answers.size == uiState.questions.size
            ) {
                Text("Submit Quiz")
            }
        }
    }
}

@Composable
fun LockoutScreen(remainingMillis: Long) {
    val hours = remainingMillis / (1000 * 60 * 60)
    val minutes = (remainingMillis / (1000 * 60)) % 60
    val seconds = (remainingMillis / 1000) % 60

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Timer,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text("Account Temporarily Locked", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "You have failed the season refresher quiz. To ensure all referees are properly prepared, there is a mandatory 24-hour study period before you can attempt the quiz again.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(32.dp))
        Text("Retry available in:", style = MaterialTheme.typography.labelLarge)
        Text(
            text = java.lang.String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun NoQuizAvailable(onFinish: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No quiz is required for this season yet.", textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onFinish) { Text("Continue") }
    }
}

@Composable
fun ResultScreen(
    attempt: QuizAttempt,
    required: Int,
    onFinish: () -> Unit,
    onRetry: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (attempt.passed) "Congratulations!" else "Not Quite Passed", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        Text("Your Score: ${attempt.score} / ${attempt.totalQuestions}", style = MaterialTheme.typography.titleLarge)
        
        if (attempt.passed) {
            Text("You have successfully unlocked the new season.", textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Finish") }
        } else {
            val percent = (attempt.score.toFloat() / attempt.totalQuestions * 100).toInt()
            Text("You earned $percent%, but $required% is required. Please try again.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(32.dp))
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Try Again") }
        }
    }
}
