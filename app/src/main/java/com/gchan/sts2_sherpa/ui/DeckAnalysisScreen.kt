package com.gchan.sts2_sherpa.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gchan.sts2_sherpa.data.DeckCard
import com.gchan.sts2_sherpa.logic.RecommendationEngine

@Composable
fun DeckAnalysisScreen(
    deckCards: List<DeckCard>,
    onDeckClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val analysis = remember(deckCards) { RecommendationEngine.analyzeDeck(deckCards) }
    val direction = remember(analysis.tagCounts) {
        val candidates = listOf("poison" to "중독 방향", "shiv" to "단도 방향", "sly" to "교활 방향")
        candidates.maxByOrNull { analysis.tagCounts.getOrDefault(it.first, 0) }
            ?.takeIf { analysis.tagCounts.getOrDefault(it.first, 0) >= 2 }
            ?.second ?: "아직 방향 미정"
    }
    val missingRoles = remember(analysis.tagCounts, analysis.deckSize) {
        buildList {
            if (analysis.tagCounts.getOrDefault("aoe", 0) == 0) add("광역기 부족")
            if (analysis.tagCounts.getOrDefault("draw", 0) <= 1 && analysis.deckSize >= 18) add("드로우 부족")
            if (analysis.tagCounts.getOrDefault("block", 0) <= 5) add("방어 부족")
            if (analysis.tagCounts.getOrDefault("energy", 0) == 0 && analysis.deckSize >= 22) add("에너지 보조 부족")
        }.ifEmpty { listOf("큰 부족 요소 없음") }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 64.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "덱 분석",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFFFE0A0),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "현재 덱의 카드 구성과 부족한 역할을 요약합니다.",
                modifier = Modifier.padding(top = 4.dp),
                color = Color(0xFFE8D7A2),
            )
        }
        item {
            Button(onClick = onDeckClick, modifier = Modifier.fillMaxWidth()) {
                Text("덱 보기")
            }
        }
        item {
            StatGrid(
                stats = listOf(
                    "총 카드" to analysis.deckSize,
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
            InfoCard(title = "현재 덱 방향", body = direction)
        }
        item {
            InfoCard(
                title = "주요 태그 Top 5",
                body = analysis.tagCounts.entries
                    .sortedByDescending { it.value }
                    .take(5)
                    .joinToString("\n") { "${it.key} ${it.value}" }
                    .ifBlank { "아직 태그 정보가 부족합니다." },
            )
        }
        item {
            InfoCard(title = "부족한 역할", body = missingRoles.joinToString("\n"))
        }
    }
}

@Composable
private fun StatGrid(stats: List<Pair<String, Int>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.chunked(3).forEach { rowStats ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowStats.forEach { (label, value) ->
                    InfoCard(
                        title = label,
                        body = "${value}장",
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - rowStats.size) {
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
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF51452F)),
        colors = CardDefaults.cardColors(containerColor = Color(0xAA17140F)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(title, color = Color(0xFFFFE0A0), fontWeight = FontWeight.Bold)
            Text(body, color = Color(0xFFE8D7A2), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
