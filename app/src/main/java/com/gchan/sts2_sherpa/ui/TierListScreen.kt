package com.gchan.sts2_sherpa.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
            .padding(horizontal = 16.dp, vertical = 64.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "티어리스트",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFFFE0A0),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "초보자 기준으로 정리한 사일런트 카드 티어리스트입니다.",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE8D7A2),
            )
            BrowseFilterRow(
                selectedFilter = filter,
                onFilterSelected = { filter = it },
            )
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "$tier Tier",
            style = MaterialTheme.typography.titleLarge,
            color = tierColor(tier),
            fontWeight = FontWeight.Bold,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 16.dp),
        ) {
            items(cards, key = { it.id }) { card ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCardClick(card) },
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    CardAssetImage(
                        path = card.image,
                        contentDescription = card.browseDisplayName(),
                        fallbackText = card.browseDisplayName(),
                        modifier = Modifier
                            .width(108.dp)
                            .aspectRatio(0.72f),
                    )
                    Text(
                        text = card.browseDisplayName(),
                        modifier = Modifier.width(108.dp),
                        color = Color(0xFFE8D7A2),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun tierColor(tier: String): Color =
    when (tier) {
        "S" -> Color(0xFFFFE68A)
        "A" -> Color(0xFFB8E986)
        "B" -> Color(0xFFD9ECFF)
        "C" -> Color(0xFFE8D7A2)
        else -> Color(0xFFFFB4AB)
    }
