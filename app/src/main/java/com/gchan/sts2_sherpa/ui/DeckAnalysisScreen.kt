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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gchan.sts2_sherpa.data.DeckCard
import com.gchan.sts2_sherpa.logic.BuildAnalyzer
import com.gchan.sts2_sherpa.logic.RecommendationEngine

@Composable
fun DeckAnalysisScreen(
    deckCards: List<DeckCard>,
    defaultBuildName: String,
    onAddCardClick: () -> Unit,
    onRemoveDeckCard: (String) -> Unit,
    onResetDeck: () -> Unit,
    onClearDeck: () -> Unit,
    onSaveBuild: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isSaveDialogOpen by remember { mutableStateOf(false) }
    val analysis = remember(deckCards) { RecommendationEngine.analyzeDeck(deckCards) }
    val direction = remember(deckCards) { BuildAnalyzer.directionLabel(deckCards) }
    val completionScore = remember(deckCards) { BuildAnalyzer.completionScore(deckCards) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 64.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "덱 실험실",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFFFE0A0),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "추천 플레이 덱과 분리된 실험 덱을 직접 만들고 저장합니다.",
                modifier = Modifier.padding(top = 4.dp),
                color = Color(0xFFE8D7A2),
            )
        }
        item {
            InfoCard(
                title = "완성도",
                body = "$completionScore% · $direction · 총 ${analysis.deckSize}장",
            ) {
                LinearProgressIndicator(
                    progress = { completionScore / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFD6B15E),
                    trackColor = Color(0xFF292319),
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onResetDeck,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, Color(0xFFD6B15E)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFE0A0)),
                ) {
                    Text("시작 덱")
                }
                OutlinedButton(
                    onClick = onClearDeck,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, Color(0xFFB36A4A)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFC0A8)),
                ) {
                    Text("빈 덱")
                }
                Button(
                    onClick = { isSaveDialogOpen = true },
                    modifier = Modifier.weight(1f),
                    enabled = deckCards.isNotEmpty(),
                ) {
                    Text("저장")
                }
            }
        }
        item {
            StatGrid(
                stats = listOf(
                    "공격 역할" to analysis.pickedAttackCount,
                    "방어 역할" to analysis.pickedBlockCount,
                    "파워" to analysis.powerCount,
                    "드로우" to analysis.drawCount,
                    "에너지" to analysis.energyCount,
                    "중독" to analysis.tagCounts.getOrDefault("poison", 0),
                    "단도" to analysis.tagCounts.getOrDefault("shiv", 0),
                    "교활" to analysis.tagCounts.getOrDefault("sly", 0),
                ),
            )
        }
        item {
            LabDeckGrid(
                deckCards = deckCards,
                onAddCardClick = onAddCardClick,
                onRemoveDeckCard = onRemoveDeckCard,
            )
        }
    }

    if (isSaveDialogOpen) {
        SaveBuildDialog(
            defaultName = defaultBuildName.ifBlank { "실험 덱" },
            onSave = { name, description ->
                onSaveBuild(name, description)
                isSaveDialogOpen = false
            },
            onDismissRequest = { isSaveDialogOpen = false },
        )
    }
}

@Composable
private fun LabDeckGrid(
    deckCards: List<DeckCard>,
    onAddCardClick: () -> Unit,
    onRemoveDeckCard: (String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val entries = deckCards.map<DeckCard, Any> { it } + LabAddTileMarker
        entries.chunked(4).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { item ->
                    when (item) {
                        is DeckCard -> EditableDeckCardItem(
                            deckCard = item,
                            onRemoveClick = { onRemoveDeckCard(item.card.id) },
                            modifier = Modifier.weight(1f),
                        )

                        LabAddTileMarker -> DeckAddCardTile(
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
private fun EditableDeckCardItem(
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
                    .background(Color(0xDD05070B), RoundedCornerShape(999.dp))
                    .border(1.dp, Color(0xAAFFFFFF), RoundedCornerShape(999.dp))
                    .clickable(onClick = onRemoveClick)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            if (deckCard.count > 1) {
                Text(
                    text = "x${deckCard.count}",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .background(Color(0xDD21180D), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFD6B15E), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
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
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private object LabAddTileMarker

@Composable
private fun StatGrid(stats: List<Pair<String, Int>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.chunked(2).forEach { rowStats ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowStats.forEach { (label, value) ->
                    InfoCard(
                        title = label,
                        body = "${value}장",
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowStats.size == 1) {
                    Column(modifier = Modifier.weight(1f)) {}
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    extraContent: @Composable () -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF51452F)),
        colors = CardDefaults.cardColors(containerColor = Color(0xAA17140F)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, color = Color(0xFFFFE0A0), fontWeight = FontWeight.Bold)
            Text(body, color = Color(0xFFE8D7A2), style = MaterialTheme.typography.bodyMedium)
            extraContent()
        }
    }
}
