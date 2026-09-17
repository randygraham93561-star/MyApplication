package com.google.refereeschedule.domain.repository

import com.google.refereeschedule.domain.model.PrintTemplate
import kotlinx.coroutines.flow.Flow

interface PrintTemplateRepository {
    fun getTemplatesFlow(): Flow<List<PrintTemplate>>
    suspend fun getTemplate(id: String): PrintTemplate?
    suspend fun saveTemplate(template: PrintTemplate)
    suspend fun deleteTemplate(id: String)
}
