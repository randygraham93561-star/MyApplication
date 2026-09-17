package com.google.refereeschedule.ui.admin.print

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.domain.model.PrintJob
import com.google.refereeschedule.domain.model.PrintJobStatus
import com.google.refereeschedule.domain.repository.OrganizationRepository
import com.google.refereeschedule.domain.repository.PrintRepository
import com.google.refereeschedule.domain.repository.UserRepository
import com.google.refereeschedule.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrintManagementUiState(
    val pendingJobs: List<PrintJob> = emptyList(),
    val completedJobs: List<PrintJob> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PrintManagementViewModel @Inject constructor(
    private val printRepository: PrintRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrintManagementUiState())
    val uiState: StateFlow<PrintManagementUiState> = _uiState.asStateFlow()

    init {
        loadJobs()
    }

    private fun loadJobs() {
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid ?: return@launch
            val user = userRepository.getUser(uid)
            val orgId = user?.organizationId ?: return@launch

            printRepository.getPrintJobsForOrganizationFlow(orgId)
                .onEach { allJobs ->
                    _uiState.update { it.copy(
                        pendingJobs = allJobs.filter { it.status == PrintJobStatus.Pending.name },
                        completedJobs = allJobs.filter { it.status != PrintJobStatus.Pending.name },
                        isLoading = false
                    )}
                }.catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }.launchIn(viewModelScope)
        }
    }

    fun markJobCompleted(jobId: String) {
        viewModelScope.launch {
            printRepository.updatePrintJobStatus(jobId, PrintJobStatus.Completed, null)
        }
    }

    fun markJobFailed(jobId: String, reason: String) {
        viewModelScope.launch {
            printRepository.updatePrintJobStatus(jobId, PrintJobStatus.Failed, reason)
        }
    }
    
    fun deleteJob(jobId: String) {
        // We could add a delete method to repository if needed
    }
}
