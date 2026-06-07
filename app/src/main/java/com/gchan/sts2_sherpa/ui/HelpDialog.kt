package com.gchan.sts2_sherpa.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun HelpDialog(
    onDismissRequest: () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xE617140F),
            border = BorderStroke(2.dp, Color(0xFFD6B15E)),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "도움말",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFE0A0),
                    )
                    Button(onClick = onDismissRequest) {
                        Text(text = "닫기")
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HelpSection(
                        title = "앱 사용 방법",
                        items = listOf(
                            "카드 보상 3장을 직접 선택하거나 카메라로 인식할 수 있습니다.",
                            "인식 결과가 틀리면 확인 화면에서 직접 수정할 수 있습니다.",
                            "추천 결과를 보고 원하는 카드를 누르면 현재 덱에 추가됩니다.",
                            "덱 아이콘을 누르면 현재 카드 풀을 확인하고 잘못 넣은 카드를 제거할 수 있습니다.",
                        ),
                    )
                    HelpSection(
                        title = "추천 방식",
                        items = listOf(
                            "이 앱은 LLM이 임의로 추천하는 방식이 아니라, 카드 티어와 태그, 현재 덱 상태를 기반으로 점수를 계산합니다.",
                            "현재 덱의 공격, 방어, 드로우, 파워, 중독, 단도, 교활 태그를 분석해 후보 카드의 점수를 계산합니다.",
                            "상황에 따라 특정 카드를 추천하거나, 카드가 좋지 않으면 스킵을 추천합니다.",
                        ),
                    )
                    HelpSection(
                        title = "초반 추천 규칙",
                        items = listOf(
                            "초반에는 시너지보다 당장 전투에 도움이 되는 카드가 중요합니다.",
                            "추가 공격 카드 3장을 먼저 확보하는 것을 우선합니다.",
                            "공격 카드 3장을 확보한 뒤에는 일반 공격 카드를 강하게 감점합니다.",
                            "초반 드로우/에너지 카드는 기본 타격/수비를 뽑을 가능성이 높아 낮게 평가합니다.",
                        ),
                    )
                    HelpSection(
                        title = "중반 이후 추천 규칙",
                        items = listOf(
                            "덱이 커질수록 드로우, 에너지, 파워 카드의 가치가 올라갑니다.",
                            "파워 카드가 많아지면 첫 카드 사이클이 약해질 수 있어 드로우/에너지 카드로 보완합니다.",
                            "24장 이후부터는 애매한 일반 공격/방어 카드보다 핵심 시너지 카드나 스킵을 더 높게 평가합니다.",
                        ),
                    )
                    HelpSection(
                        title = "특수 규칙",
                        items = listOf(
                            "첫 발놀림은 매우 높은 우선순위를 가집니다.",
                            "피니셔 카드는 관련 시너지가 충분할 때만 추천합니다.",
                            "이미 덱에 있는 중복 카드는 초보자 기준으로 감점합니다.",
                            "D티어 카드는 특별한 상황이 아니면 추천하지 않습니다.",
                        ),
                    )
                    HelpSection(
                        title = "주의",
                        items = listOf(
                            "이 앱은 초보자용 보조 추천 도구입니다.",
                            "실제 게임에서는 유물, 체력, 보스, 포션, 경로에 따라 선택이 달라질 수 있습니다.",
                            "추천은 절대 정답이 아니라 현재 덱만 기준으로 한 참고용 판단입니다.",
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpSection(
    title: String,
    items: List<String>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0x661B2028),
                shape = RoundedCornerShape(10.dp),
            )
            .border(
                width = 1.dp,
                color = Color(0x44514A36),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFE0A0),
        )
        items.forEach { item ->
            Text(
                text = "- $item",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE8D7A2),
            )
        }
    }
}
