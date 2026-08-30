package com.google.refereeschedule.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.refereeschedule.domain.model.PointAward
import com.google.refereeschedule.domain.model.Team
import com.google.refereeschedule.domain.repository.TeamRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TeamRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : TeamRepository {

    private val teamsCollection = firestore.collection("teams")
    private val awardsCollection = firestore.collection("point_awards")

    override suspend fun getTeam(id: String): Team? {
        return try {
            val doc = teamsCollection.document(id).get().await()
            doc.toObject(Team::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveTeam(team: Team) {
        if (team.id.isEmpty()) {
            val docRef = teamsCollection.document()
            docRef.set(team.copy(id = docRef.id)).await()
        } else {
            teamsCollection.document(team.id).set(team).await()
        }
    }

    override fun getTeamsFlow(): Flow<List<Team>> {
        return teamsCollection.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Team::class.java)?.copy(id = doc.id)
            }
        }
    }

    override fun getTeamsForOrganizationFlow(organizationId: String): Flow<List<Team>> {
        return teamsCollection.whereEqualTo("organizationId", organizationId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Team::class.java)?.copy(id = doc.id)
                }
            }
    }

    override fun getTeamsForSeasonFlow(seasonId: String): Flow<List<Team>> {
        return teamsCollection.whereEqualTo("seasonId", seasonId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Team::class.java)?.copy(id = doc.id)
                }
            }
    }

    override suspend fun awardPoints(award: PointAward) {
        // Prevent duplicate awards for this specific team/game/referee combo
        if (hasBeenAwarded(award.gameId, award.refereeId, award.teamId)) return

        val docRef = awardsCollection.document()
        val finalAward = award.copy(id = docRef.id)
        
        firestore.runTransaction { transaction ->
            // 1. Create the award record
            transaction.set(docRef, finalAward)
            
            // 2. Update the team's total points (skip if unassigned)
            if (award.teamId != "unassigned" && award.teamId.isNotEmpty()) {
                val teamRef = teamsCollection.document(award.teamId)
                    val teamSnap = transaction.get(teamRef)
                val currentPoints = (teamSnap.get("totalPoints") as? Number)?.toInt() ?: 0
                transaction.update(teamRef, "totalPoints", currentPoints + award.points)
            }
        }.await()
    }

    override suspend fun hasBeenAwarded(gameId: String, refereeId: String, teamId: String): Boolean {
        return try {
            val query = awardsCollection
                .whereEqualTo("gameId", gameId)
                .whereEqualTo("refereeId", refereeId)
                .whereEqualTo("teamId", teamId)
                .get()
                .await()
            !query.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getPointsForTeamOnWeekend(teamId: String, seasonId: String, date: java.util.Date): Int {
        val calendar = java.util.Calendar.getInstance()
        calendar.time = date
        
        // Find the Friday of that week (start of weekend)
        while (calendar.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.FRIDAY) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        val start = calendar.time

        // Find the Sunday of that week (end of weekend)
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 2)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        val end = calendar.time

        return try {
            val query = awardsCollection
                .whereEqualTo("teamId", teamId)
                .whereEqualTo("seasonId", seasonId)
                .whereGreaterThanOrEqualTo("timestamp", start)
                .whereLessThanOrEqualTo("timestamp", end)
                .get()
                .await()
            
            query.documents.sumOf { it.getLong("points")?.toInt() ?: 0 }
        } catch (e: Exception) {
            0
        }
    }

    override fun getUnassignedPointsFlow(organizationId: String): Flow<Int> {
        return awardsCollection.whereEqualTo("organizationId", organizationId)
            .whereEqualTo("teamId", "unassigned")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.sumOf { (it.get("points") as? Number)?.toInt() ?: 0 }
            }
    }

    override fun getPointAwardsForOrganizationFlow(organizationId: String): Flow<List<PointAward>> {
        return awardsCollection.whereEqualTo("organizationId", organizationId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(PointAward::class.java)?.copy(id = it.id) }
            }
    }

    override fun getPointAwardsForRefereeFlow(refereeId: String): Flow<List<PointAward>> {
        return awardsCollection.whereEqualTo("refereeId", refereeId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(PointAward::class.java)?.copy(id = it.id) }
            }
    }

    override suspend fun deleteTeam(id: String) {
        if (id.isEmpty()) return
        teamsCollection.document(id).delete().await()
    }
}
