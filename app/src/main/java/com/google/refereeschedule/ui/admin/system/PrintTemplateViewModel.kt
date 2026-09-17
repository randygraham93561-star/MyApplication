package com.google.refereeschedule.ui.admin.system

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.refereeschedule.domain.model.PrintJobType
import com.google.refereeschedule.domain.model.PrintTemplate
import com.google.refereeschedule.domain.model.PrintComponent
import com.google.refereeschedule.domain.model.PrintComponentType
import com.google.refereeschedule.domain.repository.PrintTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrintTemplateUiState(
    val templates: List<PrintTemplate> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class PrintTemplateViewModel @Inject constructor(
    private val printTemplateRepository: PrintTemplateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrintTemplateUiState())
    val uiState: StateFlow<PrintTemplateUiState> = _uiState.asStateFlow()

    init {
        loadTemplates()
    }

    private fun loadTemplates() {
        printTemplateRepository.getTemplatesFlow()
            .onEach { templates ->
                _uiState.value = _uiState.value.copy(templates = templates, isLoading = false)
            }.launchIn(viewModelScope)
    }

    fun saveTemplate(name: String, type: PrintJobType, components: List<PrintComponent>, isDefault: Boolean, description: String) {
        viewModelScope.launch {
            val template = PrintTemplate(
                name = name,
                type = type,
                components = components,
                isDefault = isDefault,
                description = description
            )
            printTemplateRepository.saveTemplate(template)
        }
    }

    fun updateTemplate(template: PrintTemplate) {
        viewModelScope.launch {
            printTemplateRepository.saveTemplate(template)
        }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch {
            printTemplateRepository.deleteTemplate(id)
        }
    }

    fun createStarterPack() {
        viewModelScope.launch {
            // 1. Lunch Voucher
            saveTemplate(
                name = "Standard Lunch Voucher",
                type = PrintJobType.LunchVoucher,
                components = listOf(
                    PrintComponent(type = PrintComponentType.Text, content = "LUNCH VOUCHER", fontSizeValue = 24f, isBold = true),
                    PrintComponent(type = PrintComponentType.Divider),
                    PrintComponent(type = PrintComponentType.Text, content = "Referee: {{refereeName}}", alignment = "Left"),
                    PrintComponent(type = PrintComponentType.Text, content = "Date: {{date}}", alignment = "Left"),
                    PrintComponent(type = PrintComponentType.Spacer),
                    PrintComponent(type = PrintComponentType.Text, content = "Valid for one meal at snack bar"),
                    PrintComponent(type = PrintComponentType.Barcode, content = "{{voucherCode}}"),
                    PrintComponent(type = PrintComponentType.Divider),
                    PrintComponent(type = PrintComponentType.Text, content = "Thank you for your service!", fontSizeValue = 10f)
                ),
                isDefault = true,
                description = "Standard league lunch voucher with barcode"
            )

            // 2. Match Schedule
            saveTemplate(
                name = "Daily Match Schedule",
                type = PrintJobType.MatchSchedule,
                components = listOf(
                    PrintComponent(type = PrintComponentType.Text, content = "MATCH SCHEDULE", fontSizeValue = 22f, isBold = true),
                    PrintComponent(type = PrintComponentType.Text, content = "{{refereeName}} - {{date}}"),
                    PrintComponent(type = PrintComponentType.Divider),
                    PrintComponent(type = PrintComponentType.Text, content = "YOUR ASSIGNMENTS", isBold = true, alignment = "Left"),
                    PrintComponent(type = PrintComponentType.Text, content = "{{schedule}}", alignment = "Left", fontFamily = "Monospace"),
                    PrintComponent(type = PrintComponentType.Divider),
                    PrintComponent(type = PrintComponentType.Text, content = "AVAILABLE OPEN SLOTS", isBold = true, alignment = "Left"),
                    PrintComponent(type = PrintComponentType.Text, content = "{{available}}", alignment = "Left", fontSizeValue = 10f)
                ),
                isDefault = true,
                description = "Summary of assigned and open games"
            )

            // 3. Match Report
            saveTemplate(
                name = "Official Match Report",
                type = PrintJobType.MatchReport,
                components = listOf(
                    PrintComponent(type = PrintComponentType.Text, content = "MATCH REPORT", fontSizeValue = 20f, isBold = true),
                    PrintComponent(type = PrintComponentType.Divider),
                    PrintComponent(type = PrintComponentType.Text, content = "{{gameTitle}}", isBold = true),
                    PrintComponent(type = PrintComponentType.Text, content = "Field: {{field}}  Time: {{time}}"),
                    PrintComponent(type = PrintComponentType.Spacer),
                    PrintComponent(type = PrintComponentType.Text, content = "[ ] Home Score: _______", alignment = "Left"),
                    PrintComponent(type = PrintComponentType.Text, content = "[ ] Away Score: _______", alignment = "Left"),
                    PrintComponent(type = PrintComponentType.Spacer),
                    PrintComponent(type = PrintComponentType.Text, content = "CARDS / DISCIPLINE", alignment = "Left", isBold = true),
                    PrintComponent(type = PrintComponentType.Text, content = "__________________________", alignment = "Left"),
                    PrintComponent(type = PrintComponentType.Spacer),
                    PrintComponent(type = PrintComponentType.Text, content = "Ref Signature: ________________", alignment = "Left")
                ),
                isDefault = true,
                description = "Game record form for head referees"
            )
        }
    }
}
