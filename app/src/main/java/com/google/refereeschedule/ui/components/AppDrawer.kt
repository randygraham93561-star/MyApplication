package com.google.refereeschedule.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.refereeschedule.domain.model.SubscriptionTier
import com.google.refereeschedule.domain.model.UserRole

@Composable
fun AppDrawer(
    userRole: UserRole,
    activeTier: SubscriptionTier,
    isSubscriptionExpired: Boolean,
    onNavigateToAdmin: () -> Unit,
    onNavigateToSystemAdmin: () -> Unit,
    onNavigateToUserManagement: () -> Unit,
    onNavigateToPrintTemplates: () -> Unit,
    onNavigateToOrgSetup: (() -> Unit)? = null,
    onNavigateToRefSetup: (() -> Unit)? = null,
    onNavigateToMatchSetup: (() -> Unit)? = null,
    onNavigateToPrintSetup: (() -> Unit)? = null,
    onNavigateToStandings: (() -> Unit)? = null,
    onNavigateToInbox: () -> Unit,
    onNavigateToMessagingHub: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet {
        Spacer(Modifier.height(12.dp))
        
        Text(
            "My Account",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
        )
        NavigationDrawerItem(
            label = { Text("My Profile") },
            selected = false,
            onClick = { onNavigateToProfile(); onCloseDrawer() },
            icon = { Icon(Icons.Rounded.Person, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        if (userRole != UserRole.SystemAdmin) {
            Text(
                "Navigation",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
            )
            NavigationDrawerItem(
                label = { Text("Main Dashboard") },
                selected = false,
                onClick = { onNavigateToAdmin(); onCloseDrawer() },
                icon = { Icon(Icons.Rounded.Dashboard, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                label = { Text("Mailbox") },
                selected = false,
                onClick = { onNavigateToInbox(); onCloseDrawer() },
                icon = { Icon(Icons.Rounded.Mail, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
        }

        if (userRole == UserRole.Admin || userRole == UserRole.CoachAdmin) {
            Text(
                text = "League Management",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
            )

            NavigationDrawerItem(
                label = { Text("Organization Set-up") },
                selected = false,
                onClick = { onNavigateToOrgSetup?.invoke(); onCloseDrawer() },
                icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            if (userRole == UserRole.Admin) {
                NavigationDrawerItem(
                    label = { Text("Messaging Hub") },
                    selected = false,
                    onClick = { onNavigateToMessagingHub(); onCloseDrawer() },
                    icon = { Icon(Icons.Rounded.Campaign, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Referee Setup") },
                    selected = false,
                    onClick = { onNavigateToRefSetup?.invoke(); onCloseDrawer() },
                    icon = { Icon(Icons.Rounded.People, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }

            NavigationDrawerItem(
                label = { Text("Match Set-up") },
                selected = false,
                onClick = { onNavigateToMatchSetup?.invoke(); onCloseDrawer() },
                icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            if (userRole == UserRole.Admin) {
                NavigationDrawerItem(
                    label = { Text("Print Set-up") },
                    selected = false,
                    onClick = { onNavigateToPrintSetup?.invoke(); onCloseDrawer() },
                    icon = { Icon(Icons.Rounded.Print, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }

            NavigationDrawerItem(
                label = { Text("League Standings") },
                selected = false,
                onClick = { onNavigateToStandings?.invoke(); onCloseDrawer() },
                icon = { Icon(Icons.Rounded.Star, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
        }

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
            NavigationDrawerItem(
                label = { Text("Messaging Hub") },
                selected = false,
                onClick = { onNavigateToMessagingHub(); onCloseDrawer() },
                icon = { Icon(Icons.Rounded.Campaign, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            NavigationDrawerItem(
                label = { Text("Design Print Canvas") },
                selected = false,
                onClick = { onNavigateToPrintTemplates(); onCloseDrawer() },
                icon = { Icon(Icons.Rounded.Brush, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}
