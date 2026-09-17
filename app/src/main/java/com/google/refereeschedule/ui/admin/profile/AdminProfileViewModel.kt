package com.google.refereeschedule.ui.admin.profile

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.model.Organization
import com.google.refereeschedule.domain.repository.OrganizationRepository
import com.google.refereeschedule.domain.repository.StorageRepository
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminProfileUiState(
    val organization: Organization? = null,
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
    val isUploading: Boolean = false,
    val discoveredPrinters: List<DiscoveredPrinter> = emptyList()
)

data class DiscoveredPrinter(
    val name: String,
    val ip: String,
    val port: Int
)

@HiltViewModel
class AdminProfileViewModel @Inject constructor(
    private val organizationRepository: OrganizationRepository,
    private val storageRepository: StorageRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminProfileUiState())
    val uiState: StateFlow<AdminProfileUiState> = _uiState.asStateFlow()

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val discoveredPrintersSet = mutableSetOf<DiscoveredPrinter>()

    fun setOrganizationId(orgId: String) {
        viewModelScope.launch {
            val org = organizationRepository.getOrganization(orgId)
            _uiState.update { it.copy(organization = org, isLoading = false) }
        }
    }

    fun uploadLogo(uri: android.net.Uri) {
        val orgId = _uiState.value.organization?.id ?: return
        
        _uiState.update { it.copy(isUploading = true) }
        
        viewModelScope.launch {
            val downloadUrl = storageRepository.uploadOrganizationLogo(orgId, uri)
            if (downloadUrl != null) {
                val updatedOrg = _uiState.value.organization?.copy(logoUrl = downloadUrl)
                if (updatedOrg != null) {
                    updateOrganization(updatedOrg)
                }
            }
            _uiState.update { it.copy(isUploading = false) }
        }
    }

    fun startPrinterDiscovery() {
        if (_uiState.value.isScanning) return
        
        discoveredPrintersSet.clear()
        _uiState.update { it.copy(isScanning = true, discoveredPrinters = emptyList()) }

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}
            override fun onServiceFound(service: NsdServiceInfo) {
                // We found a service, now resolve its IP
                nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        val printer = DiscoveredPrinter(
                            name = serviceInfo.serviceName,
                            ip = serviceInfo.host.hostAddress ?: "Unknown",
                            port = serviceInfo.port
                        )
                        discoveredPrintersSet.add(printer)
                        _uiState.update { it.copy(discoveredPrinters = discoveredPrintersSet.toList()) }
                    }
                })
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                discoveredPrintersSet.removeAll { it.name == service.serviceName }
                _uiState.update { it.copy(discoveredPrinters = discoveredPrintersSet.toList()) }
            }

            override fun onDiscoveryStopped(regType: String) {
                _uiState.update { it.copy(isScanning = false) }
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
                _uiState.update { it.copy(isScanning = false) }
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
                _uiState.update { it.copy(isScanning = false) }
            }
        }

        // Search for common thermal/network printer services
        // _pdl-datastream._tcp is JetDirect (standard for raw TCP printing)
        nsdManager.discoverServices("_pdl-datastream._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        
        // Auto-stop after 10 seconds
        viewModelScope.launch {
            delay(10000)
            stopPrinterDiscovery()
        }
    }

    fun stopPrinterDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {}
        }
        discoveryListener = null
        _uiState.update { it.copy(isScanning = false) }
    }

    fun updateOrganization(organization: Organization) {
        viewModelScope.launch {
            organizationRepository.saveOrganization(organization)
            _uiState.update { it.copy(organization = organization) }
        }
    }

    override fun onCleared() {
        stopPrinterDiscovery()
        super.onCleared()
    }
}
