package com.google.refereeschedule.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.data.AuthRepository
import com.google.refereeschedule.domain.model.*
import com.google.refereeschedule.domain.repository.*
import com.google.refereeschedule.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class DashboardUiState(
    val userRole: UserRole = UserRole.Referee,
    val userName: String? = null,
    val upcomingAssignments: List<AssignmentDetail> = emptyList(),
    val availableGames: List<Game> = emptyList(),
    val organization: Organization? = null,
    val isGameDayVerified: Boolean = false,
    val eligibleVouchers: List<LunchVoucherRequirement> = emptyList(),
    val printHistory: List<PrintJob> = emptyList(),
    val isLoading: Boolean = true,
    val verificationError: String? = null,
    val isPrinting: Boolean = false,
    val showPrintDialog: Boolean = false,
    val currentUserId: String = ""
)

data class AssignmentDetail(
    val myAssignment: Assignment,
    val game: Game,
    val division: Division?,
    val crew: List<CrewMemberDetail>
)

data class CrewMemberDetail(
    val assignment: Assignment,
    val name: String,
    val isYouth: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val gameRepository: GameRepository,
    private val profileRepository: ProfileRepository,
    private val assignmentRepository: AssignmentRepository,
    private val seasonRepository: SeasonRepository,
    private val organizationRepository: OrganizationRepository,
    private val printRepository: PrintRepository,
    private val printTemplateRepository: PrintTemplateRepository
) : ViewModel() {
    val currentUserEmail: String? = authRepository.currentUser?.email
    private val currentUserId = authRepository.currentUser?.uid

    private val _isGameDayVerified = MutableStateFlow(false)
    private val _verificationError = MutableStateFlow<String?>(null)
    private val _isPrinting = MutableStateFlow(false)
    private val _showPrintDialog = MutableStateFlow(false)

    private val _printFinishedEvent = MutableSharedFlow<Unit>()
    val printFinishedEvent = _printFinishedEvent.asSharedFlow()

    private val _dataState = MutableStateFlow(DashboardUiState())
    
    val uiState: StateFlow<DashboardUiState> = combine(
        _dataState,
        _isGameDayVerified,
        _verificationError,
        _isPrinting,
        _showPrintDialog
    ) { data, verified, error, printing, showDialog ->
        data.copy(
            isGameDayVerified = verified,
            verificationError = error,
            isPrinting = printing,
            showPrintDialog = showDialog
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    init {
        loadData()
    }

    private fun loadData() {
        val uid = currentUserId ?: return

        viewModelScope.launch {
            val userFlow = userRepository.getUserFlow(uid)
            val profileFlow = profileRepository.getProfileFlow(uid)
            val allAssignmentsFlow = assignmentRepository.getAllAssignmentsFlow()
            val gamesFlow = gameRepository.getGamesFlow()
            val seasonsFlow = seasonRepository.getSeasonsFlow()
            val allOrgsFlow = organizationRepository.getOrganizationsFlow()

            // 1. Get initial data to identify the organization
            val baseFlow = combine(
                userFlow,
                profileFlow,
                allOrgsFlow
            ) { user, profile, allOrgs ->
                val orgId = user?.organizationId ?: profile?.organizationId
                val org = allOrgs.find { it.id == orgId }
                org
            }.filterNotNull().distinctUntilChanged()

            // 2. Fetch profiles for that org and combine everything
            baseFlow.flatMapLatest { org ->
                val profilesFlow = profileRepository.getProfilesForOrganizationFlow(org.id)
                val printHistoryFlow = printRepository.getPrintJobsForRefereeFlow(uid)
                
                combine(
                    userFlow,
                    profileFlow,
                    allAssignmentsFlow,
                    gamesFlow,
                    seasonsFlow,
                    profilesFlow,
                    printHistoryFlow
                ) { results ->
                    val user = results[0] as? User
                    val profile = results[1] as? RefereeProfile
                    @Suppress("UNCHECKED_CAST")
                    val allAssignments = results[2] as List<Assignment>
                    @Suppress("UNCHECKED_CAST")
                    val allGames = results[3] as List<Game>
                    @Suppress("UNCHECKED_CAST")
                    val seasons = results[4] as List<Season>
                    @Suppress("UNCHECKED_CAST")
                    val profiles = results[5] as List<RefereeProfile>
                    @Suppress("UNCHECKED_CAST")
                    val printHistory = results[6] as List<PrintJob>

                    val tz = TimeZone.getTimeZone(org.timeZone)
                    val myAssignments = allAssignments.filter { it.refereeId == uid }

                    val now = Date()
                    val calendar = Calendar.getInstance(tz)
                    calendar.time = now
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startOfToday = calendar.time

                    val upcoming = myAssignments.mapNotNull { myAssignment ->
                        val game = allGames.find { it.id == myAssignment.gameId }
                        if (game != null) {
                            val gameDate = game.date ?: return@mapNotNull null
                            val isPastGame = gameDate.before(startOfToday)
                            val isFullyFinished = game.status == GameStatus.Completed
                            if (isPastGame || isFullyFinished) return@mapNotNull null
                            
                            val season = seasons.find { it.id == game.seasonId }
                            val division = season?.divisions?.find { it.name == game.divisionName }
                            
                            val gameCrew = allAssignments.filter { it.gameId == game.id }
                            val crewWithDetails = gameCrew.map { assignment ->
                                val refProfile = profiles.find { it.id == assignment.refereeId }
                                CrewMemberDetail(
                                    assignment = assignment,
                                    name = refProfile?.name ?: "Unknown",
                                    isYouth = refProfile?.isMinor ?: false
                                )
                            }.toMutableList()
                            
                            // Add Mentor if assigned
                            if (game.mentorId != null) {
                                val mentorProfile = profiles.find { it.id == game.mentorId }
                                crewWithDetails.add(CrewMemberDetail(
                                    assignment = Assignment(position = AssignmentPosition.Mentor, refereeId = game.mentorId),
                                    name = mentorProfile?.name ?: "Mentor",
                                    isYouth = false
                                ))
                            }
                            
                            AssignmentDetail(
                                myAssignment = myAssignment,
                                game = game,
                                division = division,
                                crew = crewWithDetails
                            )
                        } else null
                    }.sortedBy { it.game.date }

                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                        timeZone = tz
                    }
                    val todayStr = sdf.format(now)

                    // CALCULATE ELIGIBLE VOUCHERS
                    val eligibleVouchers = mutableListOf<LunchVoucherRequirement>()
                    val todayAssignments = myAssignments.filter { a ->
                        val g = allGames.find { it.id == a.gameId }
                        g != null && sdf.format(g.date) == todayStr
                    }
                    val assignedCount = todayAssignments.size
                    val completedCount = todayAssignments.count { a ->
                        val g = allGames.find { it.id == a.gameId }
                        g != null && (g.status == GameStatus.ReportApproved || g.status == GameStatus.Completed)
                    }

                    org.printRequirements.lunchVouchers.forEach { req ->
                        if (assignedCount >= req.assignedGamesRequired && completedCount >= req.completedGamesRequired) {
                            val printCount = printHistory.count { 
                                it.type == PrintJobType.LunchVoucher.name && 
                                it.data["voucherId"] == req.id &&
                                sdf.format(it.timestamp) == todayStr
                            }
                            if (printCount < req.maxPrints) {
                                eligibleVouchers.add(req)
                            }
                        }
                    }

                    DashboardUiState(
                        userRole = user?.userRole ?: UserRole.Referee,
                        userName = profile?.name,
                        upcomingAssignments = upcoming,
                        availableGames = allGames.filter { game ->
                            val isToday = sdf.format(game.date) == todayStr
                            val isFull = allAssignments.count { it.gameId == game.id } >= game.requiredCrewSize
                            val alreadyAssigned = allAssignments.any { it.gameId == game.id && it.refereeId == uid }
                            val difficultyMatch = game.difficultyLevel <= (profile?.headRefereeComfortLevel ?: 0)
                            val hasTimeConflict = upcoming.any { 
                                TimeUtils.formatTo24h(it.game.time) == TimeUtils.formatTo24h(game.time) 
                            }
                            
                            val isYouthEligible = if (profile?.isMinor == true) {
                                val refereeStep = DivisionDifficulty.getDivisionStepForAge(profile.age)
                                val gameStep = DivisionDifficulty.getDivisionStep(game.ageGroup)
                                val canAR = (refereeStep - gameStep) >= org.youthRefereeRequirements.minDivisionGapAR
                                val canHead = (refereeStep - gameStep) >= org.youthRefereeRequirements.minDivisionGapHead
                                canAR || canHead
                            } else true

                            isToday && !isFull && !alreadyAssigned && difficultyMatch && !hasTimeConflict && isYouthEligible
                        },
                        organization = org,
                        eligibleVouchers = eligibleVouchers,
                        printHistory = printHistory,
                        isLoading = false,
                        currentUserId = uid
                    )
                }
            }.collect {
                _dataState.value = it
            }
        }
    }

    fun openPrintDialog() {
        _showPrintDialog.value = true
        _verificationError.value = null
    }

    fun closePrintDialog() {
        _showPrintDialog.value = false
        _isGameDayVerified.value = false
        _verificationError.value = null
    }

    fun verifyGameDayCode(code: String) {
        val org = _dataState.value.organization
        if (org == null) {
            _verificationError.value = "Waiting for organization data..."
            return
        }
        
        val inputCode = code.trim()
        val targetCode = org.currentGameDayCode.trim()

        if (inputCode == targetCode && inputCode.isNotEmpty()) {
            _isGameDayVerified.value = true
            _verificationError.value = null
            
            // Set printer to busy so GDS shows "PRINTING..."
            viewModelScope.launch {
                organizationRepository.saveOrganization(org.copy(printerStatus = "PRINTING"))
            }
        } else {
            _verificationError.value = "Invalid code. Make sure GDS is showing $targetCode" 
        }
    }

    fun submitPrintRequest(
        selectedVoucherIds: Set<String>,
        printSchedule: Boolean,
        printReports: Boolean,
        reprintAllReports: Boolean
    ) {
        val uid = currentUserId ?: return
        val state = _dataState.value
        val org = state.organization ?: return
        
        _isPrinting.value = true
        _verificationError.value = null

        viewModelScope.launch {
            try {
                val allTemplates = printTemplateRepository.getTemplatesFlow().first()
                if (allTemplates.isEmpty()) {
                    _verificationError.value = "Design templates not found in cloud."
                    _isPrinting.value = false
                    return@launch
                }

                val tz = TimeZone.getTimeZone(org.timeZone)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = tz }
                val todayStr = sdf.format(Date())

                // 1. LUNCH VOUCHERS
                for (voucherId in selectedVoucherIds) {
                    val voucherReq = org.printRequirements.lunchVouchers.find { it.id == voucherId } ?: continue
                    val template = allTemplates.find { it.type == PrintJobType.LunchVoucher && it.isDefault } ?: allTemplates.first()
                    
                    val data = mapOf(
                        "refereeName" to (state.userName ?: "Referee"),
                        "orgName" to org.name,
                        "date" to todayStr,
                        "voucherLabel" to voucherReq.label,
                        "voucherId" to voucherId,
                        "voucherCode" to "V-${System.currentTimeMillis().toString().takeLast(6)}"
                    )
                    submitJobInternal(template, data, PrintJobType.LunchVoucher, org.id, uid)
                }

                // 2. MATCH SCHEDULE
                if (printSchedule) {
                    val template = allTemplates.find { it.type == PrintJobType.MatchSchedule && it.isDefault } ?: allTemplates.first()
                    val schedule = state.upcomingAssignments.joinToString("\n") { 
                        "${TimeUtils.formatTo12h(it.game.time)} - ${it.game.divisionName}: ${it.game.homeTeamName} vs ${it.game.awayTeamName}" 
                    }
                    val data = mapOf(
                        "refereeName" to (state.userName ?: "Referee"),
                        "orgName" to org.name,
                        "date" to todayStr,
                        "schedule" to schedule
                    )
                    submitJobInternal(template, data, PrintJobType.MatchSchedule, org.id, uid)
                }

                // 3. MATCH REPORTS
                if (printReports) {
                    val template = allTemplates.find { it.type == PrintJobType.MatchReport && it.isDefault } ?: allTemplates.first()
                    
                    var gamesToPrint = state.upcomingAssignments.map { it.game }
                    if (org.printRequirements.matchReport.headRefereeOnly) {
                        gamesToPrint = state.upcomingAssignments
                            .filter { it.myAssignment.position == AssignmentPosition.HeadReferee }
                            .map { it.game }
                    }

                    if (!reprintAllReports) {
                        val printedGameIds = state.printHistory
                            .filter { it.type == PrintJobType.MatchReport.name }
                            .mapNotNull { it.data["gameId"] }
                        gamesToPrint = gamesToPrint.filter { it.id !in printedGameIds }
                    }

                    for (game in gamesToPrint) {
                        val data = mapOf(
                            "gameTitle" to "${game.homeTeamName} vs ${game.awayTeamName}",
                            "gameId" to game.id,
                            "homeTeam" to game.homeTeamName,
                            "awayTeam" to game.awayTeamName,
                            "division" to game.divisionName,
                            "gameNumber" to game.gameNumber.toString(),
                            "location" to game.location,
                            "field" to game.fieldNumber,
                            "time" to TimeUtils.formatTo12h(game.time),
                            "orgName" to org.name,
                            "date" to todayStr
                        )
                        submitJobInternal(template, data, PrintJobType.MatchReport, org.id, uid)

                        // 4. GAME REFERENCE SHEET (Conditional)
                        if (org.printRequirements.matchReport.includeReferenceSheet) {
                            val refTemplate = allTemplates.find { it.type == PrintJobType.MatchReport && it.name.contains("Reference", ignoreCase = true) }
                                ?: template // Fallback to same if not found
                            
                            val refPrintCount = state.printHistory.count { 
                                it.type == PrintJobType.MatchReport.name && 
                                it.data["isReferenceSheet"] == "true" 
                            }
                            
                            if (org.printRequirements.matchReport.referenceSheetMode == "EveryTime" || refPrintCount == 0) {
                                val refData = data.toMutableMap().apply { put("isReferenceSheet", "true") }
                                submitJobInternal(refTemplate, refData, PrintJobType.MatchReport, org.id, uid)
                            }
                        }
                    }
                }
                
                _printFinishedEvent.emit(Unit)
                _isGameDayVerified.value = false
                _showPrintDialog.value = false
                
            } catch (e: Exception) {
                _verificationError.value = "Submission Failed: ${e.message}"
            } finally {
                _isPrinting.value = false
            }
        }
    }

    private suspend fun submitJobInternal(
        template: PrintTemplate, 
        data: Map<String, String>, 
        type: PrintJobType,
        orgId: String,
        uid: String
    ) {
        val rendered = renderTemplate(template, data)
        val job = PrintJob(
            organizationId = orgId,
            refereeId = uid,
            type = type.name,
            templateId = template.id,
            renderedContent = rendered,
            data = data,
            status = "Pending",
            timestamp = Date()
        )
        printRepository.submitPrintJob(job)
    }

    private fun renderTemplate(template: PrintTemplate, data: Map<String, String>): String {
        val sb = StringBuilder()
        
        // 1. Configure Paper Width (Standard 80mm = ~42 chars, 58mm = ~32 chars)
        val paperWidthSetting = _dataState.value.organization?.printerSettings?.get("paper_width") ?: "80mm"
        val maxCols = if (paperWidthSetting.contains("58")) 32 else 42
        
        // 2. Handle Orientation
        if (template.orientation == PrintOrientation.Landscape) {
            sb.append("[SET_ORIENTATION_LANDSCAPE]\n")
        }

        // 3. Group components into "Lines" by their Y-Offset
        // We'll treat anything within 10 units of Y as the same line
        val lines = template.components.groupBy { it.yOffset / 10 }
            .toSortedMap()

        lines.forEach { (_, components) ->
            // Sort components on this line by X-Offset
            val lineComponents = components.sortedBy { it.xOffset }
            val lineSb = StringBuilder()
            var currentPos = 0

            lineComponents.forEach { comp ->
                // Map X-Offset (-150 to 150) to column index (0 to maxCols)
                val targetCol = ((comp.xOffset + 150) * maxCols / 300).coerceIn(0, maxCols - 1)
                
                // Pad with spaces to reach target X position
                if (targetCol > currentPos) {
                    lineSb.append(" ".repeat(targetCol - currentPos))
                    currentPos = targetCol
                }

                val content = processPlaceholders(comp.content, data)
                
                when (comp.type) {
                    PrintComponentType.Text -> {
                        if (comp.isBold) lineSb.append("<b>")
                        if (comp.fontSizeValue > 20f) lineSb.append("<font size='big'>")
                        lineSb.append(content)
                        if (comp.fontSizeValue > 20f) lineSb.append("</font>")
                        if (comp.isBold) lineSb.append("</b>")
                        currentPos += content.length
                    }
                    PrintComponentType.Barcode -> {
                        val numeric = content.filter { it.isDigit() }.take(12).ifEmpty { "123456789012" }
                        lineSb.append("<barcode type='ean13' height='8'>$numeric</barcode>")
                        currentPos += 15 // Estimated width
                    }
                    PrintComponentType.Shape -> {
                        if (comp.shapeType == ShapeType.Rectangle) lineSb.append("[RECT]")
                        else if (comp.shapeType == ShapeType.Circle) lineSb.append("(CIRC)")
                        else if (comp.shapeType == ShapeType.Line) lineSb.append("=".repeat(comp.width / 10))
                    }
                    PrintComponentType.Image -> {
                        lineSb.append("<image src='${comp.content}' width='${comp.width}' height='${comp.height}' />")
                        currentPos += 10
                    }
                    PrintComponentType.Pdf -> {
                        lineSb.append("<pdf src='${comp.content}' />")
                        currentPos += 10
                    }
                    else -> {}
                }
            }
            sb.append(lineSb.toString()).append("\n")
        }

        // 4. Handle Max Height Limit
        template.maxHeightInches?.let {
            sb.append("[LIMIT_HEIGHT_INCHES]$it\n")
        }
        
        // 5. Final Cut
        sb.append("[CUT]\n")
        
        return sb.toString()
    }

    private fun processPlaceholders(text: String, data: Map<String, String>): String {
        var result = text
        data.forEach { (key, value) ->
            result = result.replace("{{$key}}", value)
        }
        return result
    }

    fun clearVerification() {
        _isGameDayVerified.value = false
        _verificationError.value = null
    }

    fun checkIn(assignment: Assignment) {
        viewModelScope.launch {
            assignmentRepository.saveAssignment(assignment.copy(checkedIn = true))
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        authRepository.signOut()
        onSignedOut()
    }
}
