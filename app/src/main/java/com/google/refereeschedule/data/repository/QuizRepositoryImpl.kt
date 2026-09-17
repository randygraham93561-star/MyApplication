package com.google.refereeschedule.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.refereeschedule.domain.model.QuizAttempt
import com.google.refereeschedule.domain.model.QuizQuestion
import com.google.refereeschedule.domain.model.RefresherQuiz
import com.google.refereeschedule.domain.repository.QuizRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class QuizRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : QuizRepository {

    private val questionsCollection = firestore.collection("quiz_questions")
    private val quizzesCollection = firestore.collection("quizzes")
    private val attemptsCollection = firestore.collection("quiz_attempts")

    override fun getAllGlobalQuestions(): Flow<List<QuizQuestion>> {
        return questionsCollection.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { it.toObject(QuizQuestion::class.java)?.copy(id = it.id) }
        }
    }

    override fun getQuizForSeason(seasonId: String): Flow<RefresherQuiz?> {
        return quizzesCollection.whereEqualTo("seasonId", seasonId)
            .whereEqualTo("active", true)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.firstOrNull()?.toObject(RefresherQuiz::class.java)?.copy(id = snapshot.documents.first().id)
            }
    }

    override fun getAllQuizzesFlow(): Flow<List<RefresherQuiz>> {
        return quizzesCollection.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { it.toObject(RefresherQuiz::class.java)?.copy(id = it.id) }
        }
    }

    override suspend fun saveQuestion(question: QuizQuestion) {
        if (question.id.isEmpty()) {
            val doc = questionsCollection.document()
            doc.set(question.copy(id = doc.id)).await()
        } else {
            questionsCollection.document(question.id).set(question).await()
        }
    }

    override suspend fun saveQuiz(quiz: RefresherQuiz) {
        if (quiz.id.isEmpty()) {
            val doc = quizzesCollection.document()
            doc.set(quiz.copy(id = doc.id)).await()
        } else {
            quizzesCollection.document(quiz.id).set(quiz).await()
        }
    }

    override suspend fun saveAttempt(attempt: QuizAttempt) {
        val doc = attemptsCollection.document()
        doc.set(attempt.copy(id = doc.id)).await()
    }

    override suspend fun hasPassedQuizForSeason(refereeId: String, seasonId: String): Boolean {
        return try {
            val query = attemptsCollection
                .whereEqualTo("refereeId", refereeId)
                .whereEqualTo("seasonId", seasonId)
                .whereEqualTo("passed", true)
                .get()
                .await()
            !query.isEmpty
        } catch (e: Exception) {
            false
        }
    }
}
