package com.google.refereeschedule.ui.admin.divisions

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.model.Division
import com.google.refereeschedule.domain.model.Season
import com.google.refereeschedule.domain.repository.SeasonRepository
import com.google.refereeschedule.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DivisionUiState(
    val seasons: List<Season> = emptyList(),
    val selectedSeason: Season? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class DivisionManagementViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val seasonRepository: SeasonRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DivisionUiState())
    val uiState: StateFlow<DivisionUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid ?: return@launch
            val user = userRepository.getUser(uid)
            val orgId = user?.organizationId ?: return@launch

            seasonRepository.getSeasonsForOrganizationFlow(orgId)
                .onEach { seasons ->
                    val currentSelectedId = _uiState.value.selectedSeason?.id
                    val updatedSelected = seasons.find { it.id == currentSelectedId }
                        ?: seasons.find { it.active }
                        ?: seasons.firstOrNull()

                    _uiState.update { it.copy(
                        seasons = seasons,
                        selectedSeason = updatedSelected,
                        isLoading = false
                    )}
                }.launchIn(viewModelScope)
        }
    }

    fun selectSeason(season: Season) {
        _uiState.update { it.copy(selectedSeason = season) }
    }

    fun addDivision(division: Division) {
        viewModelScope.launch {
            val season = _uiState.value.selectedSeason ?: return@launch
            val updatedDivisions = season.divisions + division
            try {
                seasonRepository.saveSeason(season.copy(divisions = updatedDivisions))
            } catch (e: Exception) {
                Log.e("DivisionManagement", "Error adding division", e)
            }
        }
    }

    fun updateDivision(oldName: String, updatedDivision: Division) {
        viewModelScope.launch {
            val season = _uiState.value.selectedSeason ?: return@launch
            val updatedDivisions = season.divisions.map { 
                if (it.name == oldName) updatedDivision else it
            }
            try {
                seasonRepository.saveSeason(season.copy(divisions = updatedDivisions))
            } catch (e: Exception) {
                Log.e("DivisionManagement", "Error adding division", e)
            }
        }
    }

    fun deleteDivision(divisionName: String) {
        viewModelScope.launch {
            val season = _uiState.value.selectedSeason ?: return@launch
            val updatedDivisions = season.divisions.filterNot { it.name == divisionName }
            seasonRepository.saveSeason(season.copy(divisions = updatedDivisions))
        }
    }
}
