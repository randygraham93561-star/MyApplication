package com.google.refereeschedule.domain.repository

import com.google.refereeschedule.domain.model.PointAward
import com.google.refereeschedule.domain.model.Team
import kotlinx.coroutines.flow.Flow

interface TeamRepository {
    suspend fun getTeam(id: String): Team?
    suspend fun saveTeam(team: Team)
    fun getTeamsFlow(): Flow<List<Team>>
    fun getTeamsForOrganizationFlow(organizationId: String): Flow<List<Team>>
    fun getTeamsForSeasonFlow(seasonId: String): Flow<List<Team>>
    suspend fun awardPoints(award: PointAward)
    suspend fun hasBeenAwarded(gameId: String, refereeId: String, teamId: String): Boolean
    suspend fun getPointsForTeamOnWeekend(teamId: String, seasonId: String, date: java.util.Date): Int
    fun getUnassignedPointsFlow(organizationId: String): Flow<Int>
    fun getPointAwardsForOrganizationFlow(organizationId: String): Flow<List<PointAward>>
    fun getPointAwardsForRefereeFlow(refereeId: String): Flow<List<PointAward>>
    suspend fun deleteTeam(id: String)
}
