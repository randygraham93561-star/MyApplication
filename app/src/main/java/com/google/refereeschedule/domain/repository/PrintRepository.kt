package com.google.refereeschedule.domain.repository

import com.google.refereeschedule.domain.model.PrintJob
import com.google.refereeschedule.domain.model.PrintJobStatus
import kotlinx.coroutines.flow.Flow

interface PrintRepository {
    suspend fun submitPrintJob(job: PrintJob)
    fun getPrintJobsForOrganizationFlow(organizationId: String): Flow<List<PrintJob>>
    fun getPrintJobsForRefereeFlow(refereeId: String): Flow<List<PrintJob>>
    suspend fun updatePrintJobStatus(jobId: String, status: PrintJobStatus, errorMessage: String? = null)
}
