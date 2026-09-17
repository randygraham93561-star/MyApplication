package com.google.refereeschedule.domain.model

import com.google.firebase.firestore.PropertyName
import java.util.Date

data class QuizQuestion(
    val id: String = "",
    val text: String = "",
    val options: List<String> = emptyList(),
    val correctAnswerIndex: Int = 0,
    val createdByOrgId: String = "",
    val authorName: String = ""
)

data class RefresherQuiz(
    val id: String = "",
    val organizationId: String = "",
    val seasonId: String = "",
    val questionIds: List<String> = emptyList(),
    val quizLength: Int = 10,
    val passingPercentage: Int = 80,
    @get:PropertyName("active") @set:PropertyName("active")
    var isActive: Boolean = true
)

data class QuizAttempt(
    val id: String = "",
    val refereeId: String = "",
    val seasonId: String = "",
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val passed: Boolean = false,
    val timestamp: Date = Date()
)
