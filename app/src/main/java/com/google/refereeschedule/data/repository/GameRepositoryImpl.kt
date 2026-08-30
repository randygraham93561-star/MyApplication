package com.google.refereeschedule.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.refereeschedule.data.local.dao.GameDao
import com.google.refereeschedule.data.local.entity.toDomain
import com.google.refereeschedule.data.local.entity.toEntity
import com.google.refereeschedule.domain.model.Game
import com.google.refereeschedule.domain.repository.GameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GameRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val gameDao: GameDao
) : GameRepository {

    private val gamesCollection = firestore.collection("games")
    private val externalScope = CoroutineScope(Dispatchers.IO)

    init {
        // Observe firestore and update room
        gamesCollection.snapshots().onEach { snapshot ->
            try {
                val games = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Game::class.java)?.copy(id = doc.id)
                }
                gameDao.insertGames(games.map { it.toEntity() })
            } catch (e: Exception) {
            }
        }.launchIn(externalScope)
    }

    override fun getGamesFlow(): Flow<List<Game>> {
        // Return room as source of truth
        return gameDao.getAllGames().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getGamesForOrganizationFlow(organizationId: String): Flow<List<Game>> {
        return gameDao.getGamesForOrganization(organizationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getLatestGameNumber(seasonId: String): Int {
        return try {
            val query = gamesCollection.whereEqualTo("seasonId", seasonId)
                .orderBy("gameNumber", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
            val snapshot = query.get().await()
            val latestGame = snapshot.toObjects(Game::class.java).firstOrNull()
            latestGame?.gameNumber ?: 0
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun getGame(id: String): Game? {
        return try {
            val game = gamesCollection.document(id).get().await().toObject(Game::class.java)
            game?.let { gameDao.insertGames(listOf(it.toEntity())) }
            game
        } catch (e: Exception) {
            gameDao.getGameById(id)?.toDomain()
        }
    }

    override suspend fun saveGame(game: Game) {
        val gameToSave = if (game.id.isEmpty()) {
            val docRef = gamesCollection.document()
            game.copy(id = docRef.id)
        } else {
            game
        }
        
        try {
            gamesCollection.document(gameToSave.id).set(gameToSave).await()
        } catch (e: Exception) {
            // If offline, Firestore will queue it if persistence is enabled, 
            // but we also want it in Room immediately.
        }
        gameDao.insertGames(listOf(gameToSave.toEntity()))
    }

    override suspend fun deleteGame(id: String) {
        try {
            gamesCollection.document(id).delete().await()
        } catch (e: Exception) {
        }
        // Ideally we'd delete from Room too
    }
}
