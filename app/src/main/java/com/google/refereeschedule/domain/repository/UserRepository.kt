package com.google.refereeschedule.domain.repository

import com.google.refereeschedule.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUser(id: String): User?
    suspend fun saveUser(user: User)
    suspend fun saveUserWithReturn(user: User): String
    fun getUserFlow(id: String): Flow<User?>
    fun getUsersForOrganizationFlow(organizationId: String): Flow<List<User>>
    fun getAllUsersFlow(): Flow<List<User>>
}
