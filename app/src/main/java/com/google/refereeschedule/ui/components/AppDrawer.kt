package com.google.refereeschedule.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.refereeschedule.domain.model.UserRole

@Composable
fun AppDrawer(
    userRole: UserRole,
    onNavigateToAdmin: () -> Unit,
    onNavigateToTeams: () -> Unit,
    onNavigateToDivisions: () -> Unit,
    onNavigateToGameScheduler: () -> Unit,
    onNavigateToRefereePoints: () -> Unit,
    onNavigateToSystemAdmin: () -> Unit,
    onNavigateToUserManagement: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet {
        Spacer(Modifier.height(12.dp))
        
        Text(
            "League Information",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
        )
        NavigationDrawerItem(
            label = { Text("Referee Points") },
            selected = false,
            onClick = { onNavigateToRefereePoints(); onCloseDrawer() },
            icon = { Icon(Icons.Rounded.Star, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        if (userRole == UserRole.SystemAdmin) {
            Text(
                "System Admin",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
            )
            NavigationDrawerItem(
                label = { Text("Organizations") },
                selected = false,
                onClick = { onNavigateToSystemAdmin(); onCloseDrawer() },
                icon = { Icon(Icons.Rounded.Business, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            NavigationDrawerItem(
                label = { Text("User Access") },
                selected = false,
                onClick = { onNavigateToUserManagement(); onCloseDrawer() },
                icon = { Icon(Icons.Rounded.People, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
        }

        if (userRole == UserRole.Admin || userRole == UserRole.SystemAdmin) {
            Text(
                "League Admin",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
            )
            NavigationDrawerItem(
                label = { Text("Stats & Seasons") },
                selected = false,
                onClick = { onNavigateToAdmin(); onCloseDrawer() },
                icon = { Icon(Icons.Rounded.Dashboard, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            NavigationDrawerItem(
                label = { Text("Teams") },
                selected = false,
                onClick = { onNavigateToTeams(); onCloseDrawer() },
                icon = { Icon(Icons.Rounded.People, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            NavigationDrawerItem(
                label = { Text("Divisions") },
                selected = false,
                onClick = { onNavigateToDivisions(); onCloseDrawer() },
                icon = { Icon(Icons.Rounded.GroupWork, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            NavigationDrawerItem(
                label = { Text("Scheduler") },
                selected = false,
                onClick = { onNavigateToGameScheduler(); onCloseDrawer() },
                icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}
