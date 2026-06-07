package com.gchan.sts2_sherpa.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class AppScreen(
    val title: String,
) {
    Recommender("카드 추천"),
    Encyclopedia("카드 백과사전"),
    TierList("티어리스트"),
    DeckAnalysis("덱 실험실"),
    BuildCollection("빌드 모음"),
    Help("도움말"),
}

@Composable
fun AppDrawerContent(
    selectedScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
) {
    ModalDrawerSheet(
        modifier = Modifier.fillMaxHeight(),
        drawerContainerColor = Color(0xFF17140F),
        drawerContentColor = Color(0xFFE8D7A2),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "STS2 Sherpa",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFFFE0A0),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "사일런트 초보자를 위한 카드 선택 도우미",
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFBEB29A),
            )
            AppScreen.entries.forEach { screen ->
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = null,
                            tint = if (screen == selectedScreen) Color(0xFFFFE0A0) else Color(0xFFBEB29A),
                        )
                    },
                    label = {
                        Text(
                            text = screen.title,
                            fontWeight = if (screen == selectedScreen) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    selected = screen == selectedScreen,
                    onClick = { onScreenSelected(screen) },
                )
                Spacer(modifier = Modifier.padding(2.dp))
            }
        }
    }
}

private val AppScreen.icon
    get() = when (this) {
        AppScreen.Recommender -> Icons.Filled.AutoAwesome
        AppScreen.Encyclopedia -> Icons.AutoMirrored.Filled.MenuBook
        AppScreen.TierList -> Icons.Filled.Leaderboard
        AppScreen.DeckAnalysis -> Icons.Filled.Science
        AppScreen.BuildCollection -> Icons.Filled.CollectionsBookmark
        AppScreen.Help -> Icons.AutoMirrored.Filled.HelpOutline
    }
