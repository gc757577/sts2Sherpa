package com.gchan.sts2_sherpa.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gchan.sts2_sherpa.data.SilentCard

@Composable
fun CardDetailDialog(
    card: SilentCard,
    onDismissRequest: () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xE617140F),
            border = BorderStroke(2.dp, Color(0xFFD6B15E)),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CardAssetImage(
                        path = card.image,
                        contentDescription = card.browseDisplayName(),
                        fallbackText = card.browseDisplayName(),
                        modifier = Modifier
                            .weight(0.9f)
                            .aspectRatio(0.68f),
                    )
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = card.browseDisplayName(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFFFFE0A0),
                            fontWeight = FontWeight.Bold,
                        )
                        DetailLine("비용", card.cost?.toString() ?: "-")
                        DetailLine("타입", card.typeKo.ifBlank { card.type })
                        DetailLine("희귀도", card.rarityKo.ifBlank { card.rarity })
                        DetailLine("티어", card.beginnerTier.ifBlank { "-" })
                    }
                }
                Text(text = card.description, color = Color(0xFFE8D7A2), style = MaterialTheme.typography.bodyMedium)
                DetailLine("태그", card.tagsKo.ifEmpty { card.allTags }.joinToString(", ").ifBlank { "-" })
                Text(
                    text = beginnerNote(card),
                    color = Color(0xFFD9ECFF),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Button(onClick = onDismissRequest, modifier = Modifier.fillMaxWidth()) {
                    Text("닫기")
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Text(
        text = "$label: $value",
        color = Color(0xFFE8D7A2),
        style = MaterialTheme.typography.bodyMedium,
    )
}

private fun beginnerNote(card: SilentCard): String =
    when (card.beginnerTier.uppercase()) {
        "S", "A" -> "초보자에게 추천 가치가 높은 카드입니다."
        "B" -> "무난하게 사용할 수 있는 카드입니다."
        "C" -> "상황을 타는 카드입니다."
        "D" -> "초보자에게는 신중히 선택하는 것이 좋습니다."
        else -> "카드 역할과 현재 덱 상황을 함께 보고 선택하세요."
    }
