package com.gchan.sts2_sherpa.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HelpScreen(
    modifier: Modifier = Modifier,
) {
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
                text = "도움말 / 추천 방식 설명",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFFFE0A0),
                fontWeight = FontWeight.Bold,
            )
        }
        item { HelpInfoCard("앱 사용 방법", "카드 보상 3장을 직접 선택하거나 카메라로 인식합니다. OCR 결과가 틀리면 확인 화면에서 직접 수정할 수 있고, 추천 카드나 후보 카드를 누르면 덱에 추가됩니다.") }
        item { HelpInfoCard("카메라 OCR", "촬영한 보상 화면에서 카드명을 읽어 후보 슬롯에 넣습니다. 인식이 완벽하지 않을 수 있으니 적용 전 확인 화면에서 수정하세요.") }
        item { HelpInfoCard("추천 방식", "카드 티어, 카드 태그, 현재 덱의 공격/방어/드로우/파워/중독/단도/교활 구성을 기반으로 점수를 계산합니다.") }
        item { HelpInfoCard("초반 공격 3장 규칙", "초반에는 시너지보다 즉시 피해를 줄 수 있는 카드가 중요하므로 추가 공격 역할 카드 3장을 우선 확보합니다.") }
        item { HelpInfoCard("공격 3장 이후", "공격 카드가 3장 확보된 뒤에는 일반 공격 카드를 강하게 감점하고 방어, 드로우, 파워, 에너지, 시너지 카드를 더 높게 봅니다.") }
        item { HelpInfoCard("피니셔 조건", "살해, 메멘토 모리, 칼날 함정, 마무리, 부식성 파도는 관련 단도/교활 시너지가 충분할 때만 추천합니다.") }
        item { HelpInfoCard("중복 카드 감점", "이미 덱에 있는 카드는 초보자 기준으로 감점합니다. 일부 핵심 카드는 1장 중복까지 감점을 약하게 적용합니다.") }
        item { HelpInfoCard("스킵 추천", "후보 카드가 현재 덱에 잘 맞지 않거나 덱이 커진 상태에서 가치가 낮으면 스킵을 추천합니다.") }
        item { HelpInfoCard("한계점", "유물, 포션, 체력, 보스, 경로 정보는 아직 반영하지 않습니다. 추천은 절대 정답이 아니라 현재 덱 기준의 초보자용 참고 판단입니다.") }
    }
}

@Composable
private fun HelpInfoCard(
    title: String,
    body: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
        }
    }
}
