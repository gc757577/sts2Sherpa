package com.gchan.sts2_sherpa.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.gchan.sts2_sherpa.data.BuildStatsAnalyzer
import com.gchan.sts2_sherpa.data.CardSynergyStat
import com.gchan.sts2_sherpa.data.CardUsageStat
import com.gchan.sts2_sherpa.data.DeckCard
import com.gchan.sts2_sherpa.data.SavedBuild
import com.gchan.sts2_sherpa.data.SilentCard
import com.gchan.sts2_sherpa.logic.BuildAnalyzer
import kotlin.math.roundToInt

private enum class BuildCollectionTab(
    val title: String,
) {
    BuildList("빌드 목록"),
    CardStats("카드 통계"),
}

@Composable
fun BuildCollectionScreen(
    savedBuilds: List<SavedBuild>,
    allCards: List<SilentCard>,
    startingDeck: List<DeckCard>,
    onSaveBuild: (String, String, List<DeckCard>) -> Unit,
    onUpdateBuild: (SavedBuild, String, String, List<DeckCard>) -> Unit,
    onDeleteBuild: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedBuild by remember { mutableStateOf<SavedBuild?>(null) }
    var isEditorOpen by remember { mutableStateOf(false) }
    var editingBuild by remember { mutableStateOf<SavedBuild?>(null) }
    var selectedTab by remember { mutableStateOf(BuildCollectionTab.BuildList) }
    var selectedStatsCardId by remember { mutableStateOf<String?>(null) }
    val usageStats = remember(savedBuilds, allCards) {
        BuildStatsAnalyzer.calculateCardUsageStats(savedBuilds, allCards)
    }
    val synergyStats = remember(selectedStatsCardId, savedBuilds, allCards) {
        selectedStatsCardId?.let { cardId ->
            BuildStatsAnalyzer.calculateSynergyStatsForCard(cardId, savedBuilds, allCards)
        }.orEmpty()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 88.dp, bottom = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "빌드 모음",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFFFE0A0),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "저장한 플레이 덱과 실험 덱을 한곳에서 확인합니다.",
                modifier = Modifier.padding(top = 4.dp),
                color = Color(0xFFE8D7A2),
            )
        }
        item {
            PrimaryActionButton(
                text = "새 빌드 만들기",
                onClick = { isEditorOpen = true },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.Add,
            )
        }
        item {
            BuildCollectionTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        }

        when (selectedTab) {
            BuildCollectionTab.BuildList -> {
                if (savedBuilds.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.CollectionsBookmark,
                            title = "저장된 빌드가 없습니다",
                            description = "덱 실험실에서 빌드를 저장하거나 새 빌드를 만들어보세요.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 36.dp),
                        )
                    }
                } else {
                    items(savedBuilds, key = { it.id }) { build ->
                        SavedBuildCard(
                            build = build,
                            onClick = { selectedBuild = build },
                            onEditClick = { editingBuild = build },
                            onDeleteClick = { onDeleteBuild(build.id) },
                        )
                    }
                }
            }

            BuildCollectionTab.CardStats -> {
                when {
                    savedBuilds.isEmpty() -> item {
                        EmptyState(
                            icon = Icons.Filled.CollectionsBookmark,
                            title = "저장된 빌드가 없습니다",
                            description = "덱 실험실이나 카드 추천 화면에서 빌드를 저장해보세요.",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    usageStats.isEmpty() -> item {
                        AppPanel(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "시작 카드 외에 저장된 카드가 없습니다.",
                                modifier = Modifier.padding(16.dp),
                                color = Color(0xFFE8D7A2),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    else -> {
                        if (savedBuilds.size == 1) {
                            item {
                                Text(
                                    text = "저장된 빌드가 적어 통계가 제한적입니다.",
                                    color = Color(0xFFBEB29A),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }

                        items(usageStats, key = { it.card.id }) { stat ->
                            val isSelected = stat.card.id == selectedStatsCardId
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CardUsageStatRow(
                                    stat = stat,
                                    isSelected = isSelected,
                                    onClick = {
                                        selectedStatsCardId = if (selectedStatsCardId == stat.card.id) {
                                            null
                                        } else {
                                            stat.card.id
                                        }
                                    },
                                )
                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut(),
                                ) {
                                    SynergyStatsSection(
                                        selectedStat = stat,
                                        synergyStats = synergyStats,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedBuild?.let { build ->
        BuildDetailDialog(
            build = build,
            onDismissRequest = { selectedBuild = null },
        )
    }

    if (isEditorOpen) {
        BuildEditorDialog(
            allCards = allCards,
            startingDeck = startingDeck,
            initialBuild = null,
            onSave = { name, description, deck ->
                onSaveBuild(name, description, deck)
                isEditorOpen = false
            },
            onDismissRequest = { isEditorOpen = false },
        )
    }

    editingBuild?.let { build ->
        BuildEditorDialog(
            allCards = allCards,
            startingDeck = startingDeck,
            initialBuild = build,
            onSave = { name, description, deck ->
                onUpdateBuild(build, name, description, deck)
                editingBuild = null
            },
            onDismissRequest = { editingBuild = null },
        )
    }
}

@Composable
private fun BuildCollectionTabs(
    selectedTab: BuildCollectionTab,
    onTabSelected: (BuildCollectionTab) -> Unit,
) {
    val tabs = BuildCollectionTab.entries
    TabRow(
        selectedTabIndex = tabs.indexOf(selectedTab),
        containerColor = Color(0xAA17140F),
        contentColor = Color(0xFFFFE0A0),
        indicator = {},
        divider = {},
        modifier = Modifier.fillMaxWidth(),
    ) {
        tabs.forEach { tab ->
            val selected = selectedTab == tab
            Tab(
                selected = selected,
                onClick = { onTabSelected(tab) },
                text = {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (selected) Color(0xFFD6B15E) else Color.Transparent,
                    ) {
                        Text(
                            text = tab.title,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            color = if (selected) Color(0xFF17140F) else Color(0xFFD8C8A8),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        )
                    }
                },
                modifier = Modifier.padding(3.dp),
                selectedContentColor = Color(0xFF17140F),
                unselectedContentColor = Color(0xFFD8C8A8),
            )
        }
    }
}

@Composable
private fun CardUsageStatRow(
    stat: CardUsageStat,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    StatCardRow(
        rank = stat.rank,
        card = stat.card,
        percent = stat.usageRate.asPercent(),
        isSelected = isSelected,
        onClick = onClick,
    )
}

@Composable
private fun SynergyStatsSection(
    selectedStat: CardUsageStat,
    synergyStats: List<CardSynergyStat>,
) {
    AppPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp),
        borderColor = Color(0xFF6FB7FF).copy(alpha = 0.65f),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${selectedStat.card.browseDisplayName()}와 자주 엮인 카드",
                color = Color(0xFFD9ECFF),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (synergyStats.isEmpty()) {
                Text(
                    text = "함께 저장된 추가 카드가 없습니다.",
                    color = Color(0xFFBEB29A),
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                synergyStats.take(8).forEach { stat ->
                    StatCardRow(
                        rank = stat.rank,
                        card = stat.card,
                        percent = stat.synergyRate.asPercent(),
                        isSelected = false,
                        onClick = {},
                        compact = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCardRow(
    rank: Int,
    card: SilentCard,
    percent: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    compact: Boolean = false,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF6FB7FF) else Color(0xFF51452F),
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xCC102235) else Color(0xAA17140F),
        ),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 7.dp else 9.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "#$rank",
                color = Color(0xFFFFE0A0),
                style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(if (compact) 30.dp else 38.dp),
            )
            CardAssetImage(
                path = card.image,
                contentDescription = card.browseDisplayName(),
                fallbackText = card.browseDisplayName(),
                modifier = Modifier.size(
                    width = if (compact) 34.dp else 44.dp,
                    height = if (compact) 48.dp else 62.dp,
                ),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = card.browseDisplayName(),
                    color = Color(0xFFFFE0A0),
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${card.typeKo.ifBlank { card.type }} · 티어 ${card.beginnerTier.ifBlank { "-" }}",
                    color = Color(0xFFBEB29A),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "$percent%",
                color = Color(0xFFE8D7A2),
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(if (compact) 48.dp else 62.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
    }
}

private fun Float.asPercent(): Int = (this * 100f).roundToInt()

@Composable
private fun SavedBuildCard(
    build: SavedBuild,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF51452F)),
        colors = CardDefaults.cardColors(containerColor = Color(0xAA17140F)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = build.name,
                        color = Color(0xFFFFE0A0),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${build.source} · ${build.totalCardCount}장 · 완성도 ${build.completionScore}% · ${build.directionLabel}",
                        color = Color(0xFFE8D7A2),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = onEditClick) {
                        Text("수정", color = Color(0xFFFFE0A0))
                    }
                    TextButton(onClick = onDeleteClick) {
                        Text("삭제", color = Color(0xFFFF9A7A))
                    }
                }
            }
            if (build.description.isNotBlank()) {
                Text(
                    text = build.description,
                    color = Color(0xFFBEB29A),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BuildCardPreview(deck = build.deck)
        }
    }
}

@Composable
private fun BuildEditorDialog(
    allCards: List<SilentCard>,
    startingDeck: List<DeckCard>,
    initialBuild: SavedBuild?,
    onSave: (String, String, List<DeckCard>) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val isEditMode = initialBuild != null
    var name by remember(initialBuild) { mutableStateOf(initialBuild?.name ?: "실험 덱") }
    var description by remember(initialBuild) { mutableStateOf(initialBuild?.description.orEmpty()) }
    var useStartingDeck by remember(initialBuild) { mutableStateOf(initialBuild == null) }
    var deck by remember(initialBuild, startingDeck) { mutableStateOf(initialBuild?.deck ?: startingDeck) }
    var isCardPickerOpen by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val direction = remember(deck) { BuildAnalyzer.labDirectionLabel(deck) }
    val completionScore = remember(deck) { BuildAnalyzer.labCompletionScore(deck) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xF017140F),
        titleContentColor = Color(0xFFFFE0A0),
        textContentColor = Color(0xFFE8D7A2),
        title = { Text(if (isEditMode) "빌드 수정" else "새 빌드 만들기", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("빌드 이름") },
                        singleLine = true,
                        colors = darkOutlinedTextFieldColors(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("설명") },
                        minLines = 2,
                        colors = darkOutlinedTextFieldColors(),
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                useStartingDeck = true
                                deck = startingDeck
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (useStartingDeck) "✓ 시작 덱" else "시작 덱")
                        }
                        OutlinedButton(
                            onClick = {
                                useStartingDeck = false
                                deck = emptyList()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (!useStartingDeck) "✓ 빈 덱" else "빈 덱")
                        }
                    }
                }
                item {
                    Text(
                        text = if (completionScore == 0) {
                            "완성도 0% · 아직 시작 덱 상태입니다."
                        } else {
                            "완성도 $completionScore% · $direction · 총 ${deck.sumOf { it.count }}장"
                        },
                        color = Color(0xFFFFE0A0),
                        fontWeight = FontWeight.SemiBold,
                    )
                    LinearProgressIndicator(
                        progress = { completionScore / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        color = Color(0xFFD6B15E),
                        trackColor = Color(0xFF292319),
                    )
                }
                errorMessage?.let { message ->
                    item {
                        Text(
                            text = message,
                            color = Color(0xFFFFB4AB),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                item {
                    EditableDeckRows(
                        deck = deck,
                        onAddCardClick = { isCardPickerOpen = true },
                        onRemoveCard = { cardId -> deck = removeOneFromEditorDeck(deck, cardId) },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        name.isBlank() -> errorMessage = "빌드 이름을 입력해주세요."
                        deck.isEmpty() -> errorMessage = "덱이 비어 있어 저장할 수 없습니다."
                        else -> onSave(name, description, deck)
                    }
                },
            ) {
                Text(if (isEditMode) "수정 완료" else "저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("취소", color = Color(0xFFBEB29A))
            }
        },
    )

    if (isCardPickerOpen) {
        CardPickerDialog(
            cards = allCards,
            onCardPicked = { card ->
                deck = addOneToEditorDeck(deck, card)
                isCardPickerOpen = false
            },
            onDismissRequest = { isCardPickerOpen = false },
        )
    }
}

@Composable
private fun EditableDeckRows(
    deck: List<DeckCard>,
    onAddCardClick: () -> Unit,
    onRemoveCard: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val entries = deck.map<DeckCard, Any> { it } + AddTileMarker
        entries.chunked(4).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { item ->
                    when (item) {
                        is DeckCard -> EditorDeckCardItem(
                            deckCard = item,
                            onRemoveClick = { onRemoveCard(item.card.id) },
                            modifier = Modifier.weight(1f),
                        )

                        AddTileMarker -> DeckAddCardTile(
                            onClick = onAddCardClick,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                repeat(4 - rowItems.size) {
                    Column(modifier = Modifier.weight(1f)) {}
                }
            }
        }
    }
}

@Composable
private fun EditorDeckCardItem(
    deckCard: DeckCard,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f),
        ) {
            CardAssetImage(
                path = deckCard.card.image,
                contentDescription = deckCard.card.browseDisplayName(),
                fallbackText = deckCard.card.browseDisplayName(),
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                text = "×",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(3.dp)
                    .clickable(onClick = onRemoveClick),
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            if (deckCard.count > 1) {
                Text(
                    text = "x${deckCard.count}",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp),
                    color = Color(0xFFFFE0A0),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            text = deckCard.card.browseDisplayName(),
            color = Color(0xFFE8D7A2),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun addOneToEditorDeck(deck: List<DeckCard>, card: SilentCard): List<DeckCard> =
    if (deck.none { it.card.id == card.id }) {
        deck + DeckCard(card = card, count = 1)
    } else {
        deck.map { deckCard ->
            if (deckCard.card.id == card.id) deckCard.copy(count = deckCard.count + 1) else deckCard
        }
    }

private fun removeOneFromEditorDeck(deck: List<DeckCard>, cardId: String): List<DeckCard> =
    deck.mapNotNull { deckCard ->
        if (deckCard.card.id == cardId) {
            val nextCount = deckCard.count - 1
            if (nextCount > 0) deckCard.copy(count = nextCount) else null
        } else {
            deckCard
        }
    }

private object AddTileMarker

@Composable
private fun BuildCardPreview(deck: List<DeckCard>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(deck, key = { it.card.id }) { deckCard ->
            Box(modifier = Modifier.width(58.dp)) {
                CardAssetImage(
                    path = deckCard.card.image,
                    contentDescription = deckCard.card.browseDisplayName(),
                    fallbackText = deckCard.card.browseDisplayName(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.72f),
                )
                if (deckCard.count > 1) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xCC17140F),
                        border = BorderStroke(1.dp, Color(0xFFD6B15E)),
                    ) {
                        Text(
                            text = "x${deckCard.count}",
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            color = Color(0xFFFFE0A0),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildDetailDialog(
    build: SavedBuild,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xF017140F),
        titleContentColor = Color(0xFFFFE0A0),
        textContentColor = Color(0xFFE8D7A2),
        title = {
            Text(text = build.name, fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text("출처: ${build.source}")
                    Text("${build.totalCardCount}장 · 완성도 ${build.completionScore}% · ${build.directionLabel}")
                    if (build.description.isNotBlank()) {
                        Text(
                            text = build.description,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                items(build.deck, key = { it.card.id }) { deckCard ->
                    Text(
                        text = "${deckCard.card.browseDisplayName()} x${deckCard.count} · ${deckCard.card.typeKo.ifBlank { deckCard.card.type }} · 티어 ${deckCard.card.beginnerTier.ifBlank { "-" }}",
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismissRequest) {
                Text("닫기")
            }
        },
    )
}
