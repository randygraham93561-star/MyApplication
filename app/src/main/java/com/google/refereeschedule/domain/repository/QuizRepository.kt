package com.google.refereeschedule.domain.repository

import com.google.refereeschedule.domain.model.QuizAttempt
import com.google.refereeschedule.domain.model.QuizQuestion
import com.google.refereeschedule.domain.model.RefresherQuiz
import kotlinx.coroutines.flow.Flow

interface QuizRepository {
    fun getAllGlobalQuestions(): Flow<List<QuizQuestion>>
    fun getQuizForSeason(seasonId: String): Flow<RefresherQuiz?>
    fun getAllQuizzesFlow(): Flow<List<RefresherQuiz>>
    suspend fun saveQuestion(question: QuizQuestion)
    suspend fun saveQuiz(quiz: RefresherQuiz)
    suspend fun saveAttempt(attempt: QuizAttempt)
    suspend fun hasPassedQuizForSeason(refereeId: String, seasonId: String): Boolean
}
