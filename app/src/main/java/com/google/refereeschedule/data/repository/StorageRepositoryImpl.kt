package com.google.refereeschedule.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.refereeschedule.domain.repository.StorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StorageRepositoryImpl @Inject constructor(
    private val storage: FirebaseStorage,
    @ApplicationContext private val context: Context
) : StorageRepository {

    override suspend fun uploadOrganizationLogo(orgId: String, uri: Uri): String? = withContext(Dispatchers.IO) {
        val cleanOrgId = orgId.trim()
        if (cleanOrgId.isEmpty()) throw Exception("Invalid Organization ID")

        // 1. Determine MimeType and Extension
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val extension = if (mimeType.contains("png")) "png" else "jpg"
        
        // Use a unique path to force a fresh index
        val filePath = "logos/$cleanOrgId/logo_${System.currentTimeMillis()}.$extension"
        val ref = storage.reference.child(filePath)
        
        Log.d("StorageRepository", "Attempting upload to: $filePath")

        // 2. Read bytes
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw Exception("Unreadable image file. Please choose another.")

        try {
            // 3. Upload with metadata
            val metadata = StorageMetadata.Builder()
                .setContentType(mimeType)
                .setCustomMetadata("orgId", cleanOrgId)
                .build()
            
            Log.d("StorageRepository", "Uploading ${bytes.size} bytes...")
            ref.putBytes(bytes, metadata).await()
            
            // 4. Robust Retry Loop for indexing
            var downloadUrl: String? = null
            var lastEx: Exception? = null
            
            // Re-fetch reference
            val resultRef = storage.reference.child(filePath)

            for (i in 1..6) {
                try {
                    // Gradual wait: 1.5s, 3s, 4.5s, 6s...
                    delay(1500L * i)
                    
                    val uriTask = resultRef.downloadUrl.await()
                    downloadUrl = uriTask.toString()
                    if (downloadUrl != null) break
                } catch (e: Exception) {
                    lastEx = e
                    Log.w("StorageRepository", "Indexing retry $i/6: ${e.message}")
                }
            }
            
            if (downloadUrl == null) {
                throw lastEx ?: Exception("Server indexed the file slowly. Try refreshing branding in 30 seconds.")
            }
            
            Log.d("StorageRepository", "Upload complete: $downloadUrl")
            downloadUrl
        } catch (e: Exception) {
            Log.e("StorageRepository", "Critical upload failure", e)
            throw e
        }
    }
}
