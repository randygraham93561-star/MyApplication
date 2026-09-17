package com.google.refereeschedule.domain.repository

import com.google.refereeschedule.domain.model.Season
import kotlinx.coroutines.flow.Flow

interface SeasonRepository {
    suspend fun getActiveSeason(): Season?
    suspend fun getAllSeasons(): List<Season>
    fun getSeasonsFlow(): Flow<List<Season>>
    fun getSeasonsForOrganizationFlow(organizationId: String): Flow<List<Season>>
    suspend fun saveSeason(season: Season)
    suspend fun deleteSeason(id: String)
}
