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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
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
    val addedDeckCards = remember(deckCards) { BuildAnalyzer.addedCardsOnly(deckCards) }
    val analysis = remember(addedDeckCards) { RecommendationEngine.analyzeDeck(addedDeckCards) }
    val totalCardCount = remember(deckCards) { deckCards.sumOf { it.count } }
    val direction = remember(deckCards) { BuildAnalyzer.labDirectionLabel(deckCards) }
    val completionScore = remember(deckCards) { BuildAnalyzer.labCompletionScore(deckCards) }
    val animatedCompletion by animateFloatAsState(
        targetValue = completionScore / 100f,
        label = "lab-completion-progress",
    )
    val roleStats = remember(analysis) {
        listOf(
            RoleProgress("공격", analysis.pickedAttackCount, 3),
            RoleProgress("수비", analysis.pickedBlockCount, 3),
            RoleProgress("드로우", analysis.drawCount, 3),
            RoleProgress("에너지", analysis.energyCount, 1),
            RoleProgress("파워", analysis.powerCount, 2),
            RoleProgress(
                "시너지",
                listOf(
                    analysis.tagCounts.getOrDefault("poison", 0),
                    analysis.tagCounts.getOrDefault("shiv", 0),
                    analysis.tagCounts.getOrDefault("sly", 0),
                ).maxOrNull() ?: 0,
                3,
            ),
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 88.dp),
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
                text = "기본 12장 덱을 기준으로, 추가한 카드들이 공격·수비·드로우·파워·시너지를 얼마나 보강하는지 확인합니다.",
                modifier = Modifier.padding(top = 4.dp),
                color = Color(0xFFE8D7A2),
            )
        }
        item {
            CompletionCard(
                completionScore = completionScore,
                progress = animatedCompletion,
                direction = direction,
                totalCardCount = totalCardCount,
                addedCardCount = analysis.deckSize,
            )
        }
        item {
            LabActionButtons(
                onAddCardClick = onAddCardClick,
                onSaveClick = { isSaveDialogOpen = true },
                onResetDeck = onResetDeck,
                onClearDeck = onClearDeck,
                canSave = deckCards.isNotEmpty(),
            )
        }
        item {
            CompactRoleGrid(roleStats = roleStats)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "실험 덱 카드",
                    color = Color(0xFFFFE0A0),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                LabDeckGrid(
                    deckCards = deckCards,
                    onRemoveDeckCard = onRemoveDeckCard,
                )
            }
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
private fun CompletionCard(
    completionScore: Int,
    progress: Float,
    direction: String,
    totalCardCount: Int,
    addedCardCount: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF51452F)),
        colors = CardDefaults.cardColors(containerColor = Color(0xAA17140F)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "덱 완성도",
                        color = Color(0xFFFFE0A0),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "$direction · 총 ${totalCardCount}장 · 추가 ${addedCardCount}장",
                        color = Color(0xFFE8D7A2),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = "$completionScore%",
                    color = Color(0xFFFFE0A0),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFD6B15E),
                trackColor = Color(0xFF292319),
            )
            if (completionScore == 0) {
                Text(
                    text = "아직 시작 덱 상태입니다. 카드를 추가해 덱을 실험해보세요.",
                    color = Color(0xFFBEB29A),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun LabActionButtons(
    onAddCardClick: () -> Unit,
    onSaveClick: () -> Unit,
    onResetDeck: () -> Unit,
    onClearDeck: () -> Unit,
    canSave: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryActionButton(
                text = "카드 추가",
                onClick = onAddCardClick,
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Add,
            )
            PrimaryActionButton(
                text = "저장",
                onClick = onSaveClick,
                modifier = Modifier.weight(1f),
                enabled = canSave,
                icon = Icons.Filled.Save,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryActionButton(
                text = "시작 덱",
                onClick = onResetDeck,
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.RestartAlt,
            )
            DangerActionButton(
                text = "빈 덱",
                onClick = onClearDeck,
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.DeleteSweep,
            )
        }
    }
}

@Composable
private fun CompactRoleGrid(roleStats: List<RoleProgress>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        roleStats.chunked(2).forEach { rowStats ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowStats.forEach { stat ->
                    CompactRoleCard(
                        stat = stat,
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
private fun CompactRoleCard(
    stat: RoleProgress,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = stat.progress,
        label = "role-progress-${stat.label}",
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF51452F)),
        colors = CardDefaults.cardColors(containerColor = Color(0xAA17140F)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stat.label,
                    color = Color(0xFFFFE0A0),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${stat.value}/${stat.target}",
                    color = Color(0xFFE8D7A2),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFD6B15E),
                trackColor = Color(0xFF292319),
            )
        }
    }
}

private data class RoleProgress(
    val label: String,
    val value: Int,
    val target: Int,
) {
    val progress: Float = (value / target.toFloat()).coerceIn(0f, 1f)
}

@Composable
private fun LabDeckGrid(
    deckCards: List<DeckCard>,
    onRemoveDeckCard: (String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (deckCards.isEmpty()) {
            Text(
                text = "실험 덱이 비어 있습니다.",
                color = Color(0xFFBEB29A),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        deckCards.chunked(4).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { deckCard ->
                    EditableDeckCardItem(
                        deckCard = deckCard,
                        onRemoveClick = { onRemoveDeckCard(deckCard.card.id) },
                        modifier = Modifier.weight(1f),
                    )
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
