package com.google.refereeschedule.ui.admin.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.refereeschedule.domain.model.Division
import com.google.refereeschedule.domain.model.Season

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonSelector(
    seasons: List<Season>,
    selectedSeason: Season?,
    onSeasonSelected: (Season) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedSeason?.name ?: "Select Season",
            onValueChange = {},
            readOnly = true,
            label = { Text("Season") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            seasons.forEach { season ->
                DropdownMenuItem(
                    text = { Text(season.name) },
                    onClick = {
                        onSeasonSelected(season)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonAndDivisionSelectors(
    seasons: List<Season>,
    selectedSeason: Season?,
    selectedDivision: Division?,
    onSeasonSelected: (Season) -> Unit,
    onDivisionSelected: (Division) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SeasonSelector(
            seasons = seasons,
            selectedSeason = selectedSeason,
            onSeasonSelected = onSeasonSelected,
            modifier = Modifier.weight(1f)
        )

        var divisionExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = divisionExpanded,
            onExpandedChange = { divisionExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = selectedDivision?.name ?: "Select Division",
                onValueChange = {},
                readOnly = true,
                label = { Text("Division") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = divisionExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = divisionExpanded,
                onDismissRequest = { divisionExpanded = false }
            ) {
                selectedSeason?.divisions?.forEach { division ->
                    DropdownMenuItem(
                        text = { Text(division.name) },
                        onClick = {
                            onDivisionSelected(division)
                            divisionExpanded = false
                        }
                    )
                }
            }
        }
    }
}
