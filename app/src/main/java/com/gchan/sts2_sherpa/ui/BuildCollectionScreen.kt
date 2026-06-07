package com.gchan.sts2_sherpa.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.gchan.sts2_sherpa.data.DeckCard
import com.gchan.sts2_sherpa.data.SavedBuild
import com.gchan.sts2_sherpa.data.SilentCard
import com.gchan.sts2_sherpa.logic.BuildAnalyzer

@Composable
fun BuildCollectionScreen(
    savedBuilds: List<SavedBuild>,
    allCards: List<SilentCard>,
    startingDeck: List<DeckCard>,
    onSaveBuild: (String, String, List<DeckCard>) -> Unit,
    onDeleteBuild: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedBuild by remember { mutableStateOf<SavedBuild?>(null) }
    var isEditorOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 64.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
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
            Button(
                onClick = { isEditorOpen = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("새 빌드 만들기")
            }
        }

        if (savedBuilds.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "저장된 빌드가 없습니다.",
                        color = Color(0xFFBEB29A),
                    )
                }
            }
        } else {
            items(savedBuilds, key = { it.id }) { build ->
                SavedBuildCard(
                    build = build,
                    onClick = { selectedBuild = build },
                    onDeleteClick = { onDeleteBuild(build.id) },
                )
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
            onSave = { name, description, deck ->
                onSaveBuild(name, description, deck)
                isEditorOpen = false
            },
            onDismissRequest = { isEditorOpen = false },
        )
    }
}

@Composable
private fun SavedBuildCard(
    build: SavedBuild,
    onClick: () -> Unit,
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
                verticalAlignment = Alignment.CenterVertically,
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
                        color = Color(0xFFBEB29A),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = onDeleteClick) {
                    Text(text = "삭제", color = Color(0xFFFFB4AB))
                }
            }
            if (build.description.isNotBlank()) {
                Text(
                    text = build.description,
                    color = Color(0xFFE8D7A2),
                    style = MaterialTheme.typography.bodyMedium,
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
    onSave: (String, String, List<DeckCard>) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var name by remember { mutableStateOf("실험 덱") }
    var description by remember { mutableStateOf("") }
    var useStartingDeck by remember { mutableStateOf(true) }
    var deck by remember(startingDeck) { mutableStateOf(startingDeck) }
    var isCardPickerOpen by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val direction = remember(deck) { BuildAnalyzer.directionLabel(deck) }
    val completionScore = remember(deck) { BuildAnalyzer.completionScore(deck) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xF017140F),
        titleContentColor = Color(0xFFFFE0A0),
        textContentColor = Color(0xFFE8D7A2),
        title = { Text("새 빌드 만들기", fontWeight = FontWeight.Bold) },
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
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("설명") },
                        minLines = 2,
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
                        text = "완성도 $completionScore% · $direction · 총 ${deck.sumOf { it.count }}장",
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
                Text("저장")
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
        items(deck.take(10), key = { it.card.id }) { deckCard ->
            Box(modifier = Modifier.width(54.dp)) {
                CardAssetImage(
                    path = deckCard.card.image,
                    contentDescription = deckCard.card.browseDisplayName(),
                    fallbackText = deckCard.card.browseDisplayName(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.72f),
                )
                if (deckCard.count > 1) {
                    Text(
                        text = "x${deckCard.count}",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp),
                        color = Color(0xFFFFE0A0),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
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
