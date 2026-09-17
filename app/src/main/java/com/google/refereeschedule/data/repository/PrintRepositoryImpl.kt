package com.google.refereeschedule.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.refereeschedule.domain.model.PrintJob
import com.google.refereeschedule.domain.model.PrintJobStatus
import com.google.refereeschedule.domain.repository.PrintRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PrintRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : PrintRepository {

    private val printJobsCollection = firestore.collection("print-jobs")

    override suspend fun submitPrintJob(job: PrintJob) {
        val docRef = printJobsCollection.document()
        val finalJob = job.copy(id = docRef.id)
        docRef.set(finalJob).await()
    }

    override fun getPrintJobsForOrganizationFlow(organizationId: String): Flow<List<PrintJob>> {
        // Removed the "Pending" filter to ensure the Admin can see Recent Activity
        // No orderBy to avoid missing index errors; sorting happens in the app
        return printJobsCollection
            .whereEqualTo("organizationId", organizationId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents
                    .mapNotNull { it.toObject(PrintJob::class.java)?.copy(id = it.id) }
                    .sortedByDescending { it.timestamp }
            }
    }

    override fun getPrintJobsForRefereeFlow(refereeId: String): Flow<List<PrintJob>> {
        return printJobsCollection
            .whereEqualTo("refereeId", refereeId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents
                    .mapNotNull { it.toObject(PrintJob::class.java)?.copy(id = it.id) }
            }
    }

    override suspend fun updatePrintJobStatus(jobId: String, status: PrintJobStatus, errorMessage: String?) {
        val updates = mutableMapOf<String, Any>("status" to status.name)
        errorMessage?.let { updates["errorMessage"] = it }
        printJobsCollection.document(jobId).update(updates).await()
    }
}
