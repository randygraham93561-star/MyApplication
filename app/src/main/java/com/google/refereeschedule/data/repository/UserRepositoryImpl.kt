package com.google.refereeschedule.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.refereeschedule.domain.model.User
import com.google.refereeschedule.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    private val usersCollection = firestore.collection("users")

    override suspend fun getUser(id: String): User? {
        if (id.isEmpty()) return null
        return try {
            usersCollection.document(id).get().await().toObject(User::class.java)?.copy(id = id)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveUser(user: User) {
        val userToSave = if (user.id.isEmpty()) {
            val docRef = usersCollection.document()
            user.copy(id = docRef.id)
        } else {
            user
        }
        usersCollection.document(userToSave.id).set(userToSave).await()
    }

    override suspend fun saveUserWithReturn(user: User): String {
        val userToSave = if (user.id.isEmpty()) {
            val docRef = usersCollection.document()
            user.copy(id = docRef.id)
        } else {
            user
        }
        usersCollection.document(userToSave.id).set(userToSave).await()
        return userToSave.id
    }

    override fun getUserFlow(id: String): Flow<User?> {
        if (id.isEmpty()) return flowOf(null)
        return usersCollection.document(id).snapshots().map { snapshot ->
            snapshot.toObject(User::class.java)?.copy(id = id)
        }
    }

    override fun getUsersForOrganizationFlow(organizationId: String): Flow<List<User>> {
        return usersCollection.whereEqualTo("organizationId", organizationId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                }
            }
    }

    override fun getAllUsersFlow(): Flow<List<User>> {
        return usersCollection.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(id = doc.id)
            }
        }
    }
}
