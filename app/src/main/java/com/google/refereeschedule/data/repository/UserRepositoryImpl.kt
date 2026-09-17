package com.google.refereeschedule.data.repository

import android.util.Log
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
            // Log and return null instead of crashing
            Log.e("UserRepository", "Error fetching user $id: ${e.message}")
            null
        }
    }

    override suspend fun saveUser(user: User) {
        // 1. Identify roles map (handling migration)
        val rolesMap = if (user.roles.isEmpty() && user.legacyRole != null) {
            mapOf("referee" to user.legacyRole!!)
        } else if (user.roles.isNotEmpty()) {
            user.roles
        } else {
            mapOf("referee" to "Referee")
        }

        // 2. Clear legacy string and update roles map
        val userToSave = user.copy(
            roles = rolesMap,
            legacyRole = null
        )

        // 3. Save
        val finalUser = if (userToSave.id.isEmpty()) {
            val docRef = usersCollection.document()
            userToSave.copy(id = docRef.id)
        } else {
            userToSave
        }
        usersCollection.document(finalUser.id).set(finalUser).await()
    }

    override suspend fun saveUserWithReturn(user: User): String {
        val rolesMap = if (user.roles.isEmpty() && user.legacyRole != null) {
            mapOf("referee" to user.legacyRole!!)
        } else if (user.roles.isNotEmpty()) {
            user.roles
        } else {
            mapOf("referee" to "Referee")
        }

        val userToSave = user.copy(
            roles = rolesMap,
            legacyRole = null
        )

        val finalUser = if (userToSave.id.isEmpty()) {
            val docRef = usersCollection.document()
            userToSave.copy(id = docRef.id)
        } else {
            userToSave
        }
        usersCollection.document(finalUser.id).set(finalUser).await()
        return finalUser.id
    }

    override fun getUserFlow(id: String): Flow<User?> {
        if (id.isEmpty()) return flowOf(null)
        return usersCollection.document(id).snapshots().map { snapshot ->
            try {
                snapshot.toObject(User::class.java)?.copy(id = id)
            } catch (e: Exception) {
                // If the document is fundamentally broken, return null instead of crashing the app
                Log.e("UserRepository", "Error deserializing user $id: ${e.message}")
                null
            }
        }
    }

    override fun getUsersForOrganizationFlow(organizationId: String): Flow<List<User>> {
        return usersCollection.whereEqualTo("organizationId", organizationId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(User::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }

    override fun getAllUsersFlow(): Flow<List<User>> {
        return usersCollection.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}
