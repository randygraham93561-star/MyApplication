package com.google.refereeschedule.domain.repository

import com.google.refereeschedule.domain.model.Organization
import kotlinx.coroutines.flow.Flow

interface OrganizationRepository {
    fun getOrganizationsFlow(): Flow<List<Organization>>
    suspend fun getOrganization(id: String): Organization?
    suspend fun saveOrganization(organization: Organization)
    suspend fun deleteOrganization(id: String)
}
