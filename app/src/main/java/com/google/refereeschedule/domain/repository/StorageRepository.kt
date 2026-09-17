package com.google.refereeschedule.domain.repository

import android.net.Uri

interface StorageRepository {
    suspend fun uploadOrganizationLogo(orgId: String, uri: Uri): String?
}
