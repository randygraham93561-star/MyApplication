package com.google.refereeschedule.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.refereeschedule.domain.model.PrintTemplate
import com.google.refereeschedule.domain.repository.PrintTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PrintTemplateRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : PrintTemplateRepository {

    // Use hyphen to match user's explicit mention in the console
    private val templatesCollection = firestore.collection("print-templates")

    override fun getTemplatesFlow(): Flow<List<PrintTemplate>> {
        return templatesCollection.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { it.toObject(PrintTemplate::class.java)?.copy(id = it.id) }
        }
    }

    override suspend fun getTemplate(id: String): PrintTemplate? {
        return try {
            val doc = templatesCollection.document(id).get().await()
            doc.toObject(PrintTemplate::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveTemplate(template: PrintTemplate) {
        val docRef = if (template.id.isEmpty()) templatesCollection.document() else templatesCollection.document(template.id)
        val finalTemplate = if (template.id.isEmpty()) template.copy(id = docRef.id) else template
        docRef.set(finalTemplate).await()
    }

    override suspend fun deleteTemplate(id: String) {
        if (id.isEmpty()) return
        templatesCollection.document(id).delete().await()
    }
}
