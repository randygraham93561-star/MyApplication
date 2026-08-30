package com.google.refereeschedule.ui.resources

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laws of the Game") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "FIFA Laws of the Game 2026/27",
                style = MaterialTheme.typography.headlineSmall
            )
            
            LawSection(
                title = "Law 1: The Field of Play",
                content = "The field of play must be a wholly natural or, if competition rules permit, a wholly artificial playing surface, except where competition rules permit an integrated combination of artificial and natural materials (hybrid system)."
            )
            
            LawSection(
                title = "Law 2: The Ball",
                content = "All balls used in matches played in an official competition organised under the auspices of FIFA or confederations must bear one of the following: FIFA Quality PRO, FIFA Quality, IMS - INTERNATIONAL MATCH STANDARD."
            )
            
            LawSection(
                title = "Law 3: The Players",
                content = "A match is played by two teams, each with a maximum of eleven players; one must be the goalkeeper. A match may not start or continue if either team has fewer than seven players."
            )
            
            LawSection(
                title = "Law 4: The Players' Equipment",
                content = "A player must not use equipment or wear anything that is dangerous. All items of jewellery (necklaces, rings, bracelets, earrings, leather bands, rubber bands, etc.) are forbidden and must be removed. Using tape to cover jewellery is not permitted."
            )

            LawSection(
                title = "Law 5: The Referee",
                content = "Each match is controlled by a referee who has full authority to enforce the Laws of the Game in connection with the match."
            )

            LawSection(
                title = "Law 6: The Other Match Officials",
                content = "Other match officials (two assistant referees, fourth official, two additional assistant referees, reserve assistant referee, video assistant referee (VAR) and at least one assistant VAR (AVAR)) may be appointed to matches."
            )

            LawSection(
                title = "Law 7: The Duration of the Match",
                content = "A match lasts for two equal halves of 45 minutes, which may only be reduced if agreed between the referee and the two teams before the start of the match and is in accordance with competition rules."
            )

            LawSection(
                title = "Law 12: Fouls and Misconduct",
                content = "Direct and indirect free kicks and penalty kicks can only be awarded for offences committed when the ball is in play."
            )

            Text(
                text = "Full resources available offline for quick reference.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun LawSection(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(text = content, style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider()
    }
}
