package com.google.refereeschedule.ui.admin.scheduler

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.domain.model.DivisionDifficulty
import com.google.refereeschedule.domain.model.Game
import com.google.refereeschedule.domain.model.GameStatus
import com.google.refereeschedule.domain.model.Season
import com.google.refereeschedule.domain.repository.GameRepository
import com.google.refereeschedule.domain.repository.OrganizationRepository
import com.google.refereeschedule.domain.repository.SeasonRepository
import com.google.refereeschedule.domain.repository.TeamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class ImportUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val validationErrors: List<String> = emptyList()
)

@HiltViewModel
class ScheduleImportViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val seasonRepository: SeasonRepository,
    private val teamRepository: TeamRepository,
    private val organizationRepository: OrganizationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    fun importFromFile(csvData: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, validationErrors = emptyList()) }
            try {
                // 1. Fetch current league data for validation
                val activeSeason = seasonRepository.getAllSeasons().find { it.active && it.live }
                if (activeSeason == null) {
                    _uiState.update { it.copy(isLoading = false, error = "No active/live season found. Please create one first.") }
                    return@launch
                }
                
                val org = organizationRepository.getOrganization(activeSeason.organizationId)
                val tz = TimeZone.getTimeZone(org?.timeZone ?: "UTC")
                
                val allTeams = teamRepository.getTeamsForSeasonFlow(activeSeason.id).first()
                val divisions = activeSeason.divisions

                // 2. Parse and Validate
                val rows = csvData.lines().filter { it.isNotBlank() }
                if (rows.size <= 1) {
                    _uiState.update { it.copy(isLoading = false, error = "The file appears to be empty.") }
                    return@launch
                }

                val gamesToSave = mutableListOf<Game>()
                val errors = mutableListOf<String>()
                var latestNumber = gameRepository.getLatestGameNumber(activeSeason.id)

                rows.drop(1).forEachIndexed { index, line ->
                    val columns = parseCsvRow(line)
                    if (columns.size < 5) return@forEachIndexed

                    val dateStr = columns[0].trim().removeSurrounding("\"")
                    val time = columns[1].trim().removeSurrounding("\"")
                    val divCol = columns[2].trim().removeSurrounding("\"")
                    val homeName = columns[3].trim().removeSurrounding("\"")
                    val awayName = columns[4].trim().removeSurrounding("\"")
                    val location = columns.getOrNull(5)?.trim()?.removeSurrounding("\"") ?: "Main Complex"
                    val field = columns.getOrNull(6)?.trim()?.removeSurrounding("\"") ?: "1"
                    val friendly = columns.getOrNull(7)?.trim()?.removeSurrounding("\"")?.lowercase() == "yes"
                    val crewSize = columns.getOrNull(8)?.trim()?.removeSurrounding("\"")?.toIntOrNull() ?: 3

                    // Detect Gender from division name if present (e.g. "12U Boys" -> "12U" + "Boys")
                    var divName = divCol
                    var gender = "Boys"
                    if (divCol.lowercase().contains("girl")) {
                        gender = "Girls"
                        divName = divCol.lowercase().replace("girls", "").trim().uppercase()
                    } else if (divCol.lowercase().contains("boy")) {
                        gender = "Boys"
                        divName = divCol.lowercase().replace("boys", "").trim().uppercase()
                    }

                    // VALIDATION
                    val division = divisions.find { it.name.trim().lowercase() == divName.trim().lowercase() }
                    if (division == null) {
                        errors.add("Row ${index + 2}: Division '$divName' not found.")
                        return@forEachIndexed
                    }

                    val homeTeam = allTeams.find { it.name.trim().lowercase() == homeName.trim().lowercase() && it.divisionName == division.name && it.gender == gender }
                    if (homeTeam == null) {
                        errors.add("Row ${index + 2}: Home Team ID '$homeName' not found in division '${division.name}' ($gender).")
                        return@forEachIndexed
                    }

                    val awayTeam = allTeams.find { it.name.trim().lowercase() == awayName.trim().lowercase() && it.divisionName == division.name && it.gender == gender }
                    if (awayTeam == null) {
                        errors.add("Row ${index + 2}: Away Team ID '$awayName' not found in division '${division.name}' ($gender).")
                        return@forEachIndexed
                    }

                    val parsedDate = parseDate(dateStr, tz)
                    if (parsedDate == null) {
                        errors.add("Row ${index + 2}: Invalid date format '$dateStr'. Use YYYY-MM-DD.")
                        return@forEachIndexed
                    }

                    latestNumber++
                    gamesToSave.add(Game(
                        gameNumber = latestNumber,
                        date = parsedDate,
                        time = time,
                        divisionName = division.name,
                        gender = gender,
                        homeTeamName = homeTeam.name,
                        homeTeamId = homeTeam.id,
                        awayTeamName = awayTeam.name,
                        awayTeamId = awayTeam.id,
                        location = location,
                        fieldNumber = field,
                        isFriendly = friendly,
                        requiredCrewSize = crewSize,
                        seasonId = activeSeason.id,
                        organizationId = activeSeason.organizationId,
                        difficultyLevel = DivisionDifficulty.getLevelForDivision(division.name, gender),
                        status = GameStatus.Open
                    ))
                }

                if (errors.isNotEmpty()) {
                    _uiState.update { it.copy(isLoading = false, validationErrors = errors) }
                } else {
                    // 3. Save
                    gameRepository.bulkSaveGames(gamesToSave)
                    _uiState.update { it.copy(isLoading = false, successMessage = "Successfully imported ${gamesToSave.size} games!") }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to import: ${e.message}") }
            }
        }
    }

    private fun extractSheetId(url: String): String? {
        val pattern = "/spreadsheets/d/([a-zA-Z0-9-_]+)".toRegex()
        return pattern.find(url)?.groupValues?.get(1)
    }

    private fun parseCsvRow(line: String): List<String> {
        // Basic CSV parser that handles commas inside quotes
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when {
                char == '\"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().trim())
        return result
    }

    private fun parseDate(dateStr: String, tz: TimeZone): Date? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.timeZone = tz
            sdf.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }

    fun clearState() {
        _uiState.update { ImportUiState() }
    }
}
