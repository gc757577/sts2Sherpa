package com.gchan.sts2_sherpa.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gchan.sts2_sherpa.data.SilentCard

@Composable
fun TierListScreen(
    cards: List<SilentCard>,
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf(CardBrowseFilter.All) }
    var selectedCard by remember { mutableStateOf<SilentCard?>(null) }
    val filteredCards = remember(cards, filter) {
        cards.filter { filter.matches(it) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 88.dp, bottom = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "티어리스트",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFFFFE0A0),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "초보자 기준으로 정리한 사일런트 카드 티어리스트입니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE8D7A2),
                )
                BrowseFilterRow(
                    selectedFilter = filter,
                    onFilterSelected = { filter = it },
                )
            }
        }

        listOf("S", "A", "B", "C", "D").forEach { tier ->
            val tierCards = filteredCards
                .filter { it.beginnerTier.equals(tier, ignoreCase = true) }
                .sortedBy { it.browseDisplayName() }
            if (tierCards.isNotEmpty()) {
                item {
                    TierSection(
                        tier = tier,
                        cards = tierCards,
                        onCardClick = { selectedCard = it },
                    )
                }
            }
        }
    }

    selectedCard?.let { card ->
        CardDetailDialog(card = card, onDismissRequest = { selectedCard = null })
    }
}

@Composable
private fun TierSection(
    tier: String,
    cards: List<SilentCard>,
    onCardClick: (SilentCard) -> Unit,
) {
    val style = tierStyle(tier)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, style.accent.copy(alpha = 0.72f)),
        colors = CardDefaults.cardColors(containerColor = style.container),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(style.header, RoundedCornerShape(9.dp))
                    .border(1.dp, style.accent.copy(alpha = 0.52f), RoundedCornerShape(9.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "$tier Tier",
                        style = MaterialTheme.typography.titleLarge,
                        color = style.accent,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = style.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE8D7A2),
                    )
                }
                Text(
                    text = "${cards.size}장",
                    modifier = Modifier
                        .background(style.accent.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                        .border(1.dp, style.accent.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    color = style.accent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            LazyHorizontalGrid(
                rows = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(228.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 8.dp),
            ) {
                items(cards, key = { it.id }) { card ->
                    TierCardItem(
                        card = card,
                        tier = tier,
                        accent = style.accent,
                        onClick = { onCardClick(card) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TierCardItem(
    card: SilentCard,
    tier: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box {
            CardAssetImage(
                path = card.image,
                contentDescription = card.browseDisplayName(),
                fallbackText = card.browseDisplayName(),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f),
            )
            Text(
                text = tier,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .background(Color(0xDD17140F), RoundedCornerShape(7.dp))
                    .border(1.dp, accent, RoundedCornerShape(7.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                color = accent,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = card.browseDisplayName(),
            color = Color(0xFFE8D7A2),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class TierVisualStyle(
    val accent: Color,
    val header: Color,
    val container: Color,
    val subtitle: String,
)

private fun tierStyle(tier: String): TierVisualStyle =
    when (tier) {
        "S" -> TierVisualStyle(
            accent = Color(0xFFFFE68A),
            header = Color(0x443C3111),
            container = Color(0xAA19150F),
            subtitle = "핵심 카드",
        )

        "A" -> TierVisualStyle(
            accent = Color(0xFF77E0C5),
            header = Color(0x33204B43),
            container = Color(0xAA111B18),
            subtitle = "매우 우수한 카드",
        )

        "B" -> TierVisualStyle(
            accent = Color(0xFF8FC8FF),
            header = Color(0x3320344B),
            container = Color(0xAA101821),
            subtitle = "무난하게 사용 가능",
        )

        "C" -> TierVisualStyle(
            accent = Color(0xFFC7B5FF),
            header = Color(0x332D2645),
            container = Color(0xAA171421),
            subtitle = "상황을 타는 카드",
        )

        else -> TierVisualStyle(
            accent = Color(0xFFFF9B85),
            header = Color(0x333C1B15),
            container = Color(0xAA1D1110),
            subtitle = "초보자 주의 카드",
        )
    }
