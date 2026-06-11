package com.gchan.sts2_sherpa.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.gchan.sts2_sherpa.util.KoreanInitialUtils

@Composable
fun EncyclopediaScreen(
    cards: List<SilentCard>,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(CardBrowseFilter.All) }
    var selectedCard by remember { mutableStateOf<SilentCard?>(null) }
    val visibleCards = remember(cards, query, filter) {
        cards
            .filter { filter.matches(it) }
            .filter { it.matchesBrowseSearch(query) }
            .sortedWith(compareBy<SilentCard> { it.beginnerTier.tierRank() }.thenBy { it.browseDisplayName() })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 88.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "카드 백과사전",
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFFFFE0A0),
            fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("카드 이름 또는 초성 검색") },
            singleLine = true,
            colors = darkOutlinedTextFieldColors(),
        )
        BrowseFilterRow(selectedFilter = filter, onFilterSelected = { filter = it })

        if (visibleCards.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.SearchOff,
                title = "검색 결과가 없습니다",
                description = "검색어나 필터를 바꿔 다시 찾아보세요.",
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(118.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(visibleCards, key = { it.id }) { card ->
                    BrowseCardItem(card = card, onClick = { selectedCard = card })
                }
            }
        }
    }

    selectedCard?.let { card ->
        CardDetailDialog(card = card, onDismissRequest = { selectedCard = null })
    }
}

@Composable
fun BrowseFilterRow(
    selectedFilter: CardBrowseFilter,
    onFilterSelected: (CardBrowseFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CardBrowseFilter.entries.forEach { filter ->
            TextButton(onClick = { onFilterSelected(filter) }) {
                Text(
                    text = filter.label,
                    color = if (filter == selectedFilter) Color(0xFFFFE0A0) else Color(0xFFBEB29A),
                    fontWeight = if (filter == selectedFilter) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
fun BrowseCardItem(
    card: SilentCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF51452F)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF19150F)),
    ) {
        Column(
            modifier = Modifier.padding(7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            CardAssetImage(
                path = card.image,
                contentDescription = card.browseDisplayName(),
                fallbackText = card.browseDisplayName(),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f),
            )
            Text(
                text = card.browseDisplayName(),
                color = Color(0xFFE8D7A2),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "비용 ${card.cost ?: "-"} · ${card.typeKo.ifBlank { card.type }} · ${card.beginnerTier.ifBlank { "-" }}",
                color = Color(0xFFBEB29A),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

enum class CardBrowseFilter(val label: String) {
    All("전체"),
    Attack("공격"),
    Skill("스킬"),
    Power("파워"),
    Block("방어"),
    Poison("중독"),
    Shiv("단도"),
    Draw("드로우"),
    Aoe("광역");

    fun matches(card: SilentCard): Boolean =
        when (this) {
            All -> true
            Attack -> card.type == "Attack"
            Skill -> card.type == "Skill"
            Power -> card.type == "Power"
            Block -> "block" in card.allTags
            Poison -> "poison" in card.allTags
            Shiv -> "shiv" in card.allTags
            Draw -> "draw" in card.allTags
            Aoe -> "aoe" in card.allTags
        }
}

fun SilentCard.matchesBrowseSearch(query: String): Boolean {
    val normalizedQuery = KoreanInitialUtils.normalizeSearchText(query)
    if (normalizedQuery.isBlank()) return true

    val searchableValues = listOf(
        nameKo,
        normalizedName,
        KoreanInitialUtils.getKoreanInitials(nameKo),
    ).map(KoreanInitialUtils::normalizeSearchText)

    return searchableValues.any { it.contains(normalizedQuery) }
}

fun SilentCard.browseDisplayName(): String = nameKo.ifBlank { id }

private fun String.tierRank(): Int =
    when (uppercase()) {
        "S" -> 0
        "A" -> 1
        "B" -> 2
        "C" -> 3
        "D" -> 4
        else -> 5
    }
