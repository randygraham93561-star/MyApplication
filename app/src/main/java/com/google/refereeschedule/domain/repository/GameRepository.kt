package com.google.refereeschedule.domain.repository

import com.google.refereeschedule.domain.model.Game
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    fun getGamesFlow(): Flow<List<Game>>
    fun getGamesForOrganizationFlow(organizationId: String): Flow<List<Game>>
    suspend fun getLatestGameNumber(seasonId: String): Int
    suspend fun getGame(id: String): Game?
    suspend fun saveGame(game: Game)
    suspend fun bulkSaveGames(games: List<Game>)
    suspend fun deleteGame(id: String)
}
