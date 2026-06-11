package com.gchan.sts2_sherpa.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private data class OnboardingStep(
    val title: String,
    val description: String,
)

private val OnboardingSteps = listOf(
    OnboardingStep(
        title = "카드 3장을 선택하세요",
        description = "전투 보상으로 나온 카드 3장을 직접 선택하거나 카메라로 인식할 수 있습니다.",
    ),
    OnboardingStep(
        title = "현재 덱을 분석합니다",
        description = "내 덱의 공격, 방어, 드로우, 파워, 시너지 상태를 바탕으로 초보자에게 적합한 카드를 추천합니다.",
    ),
    OnboardingStep(
        title = "추천 이유까지 확인하세요",
        description = "단순히 카드 하나만 고르는 것이 아니라, 왜 이 카드가 좋은지 점수와 이유를 함께 보여줍니다.",
    ),
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingDialog(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val isLastStep = stepIndex == OnboardingSteps.lastIndex

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xF214100C),
                border = BorderStroke(1.dp, Color(0xFFD6B15E)),
                tonalElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF211A0F),
                                    Color(0xFF0B1110),
                                ),
                            ),
                        )
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "STS2 Sherpa",
                        color = Color(0xFFFFE0A0),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )

                    AnimatedContent(
                        targetState = stepIndex,
                        transitionSpec = {
                            (fadeIn() + slideInHorizontally { it / 5 })
                                .togetherWith(fadeOut() + slideOutHorizontally { -it / 5 })
                                .using(SizeTransform(clip = false))
                        },
                        label = "onboarding-step",
                    ) { index ->
                        val step = OnboardingSteps[index]
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = step.title,
                                color = Color(0xFFFFE0A0),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = step.description,
                                color = Color(0xFFE8D7A2),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OnboardingSteps.forEachIndexed { index, _ ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (index == stepIndex) {
                                            Color(0xFFFFE0A0)
                                        } else {
                                            Color(0xFF6B5A35)
                                        },
                                        shape = RoundedCornerShape(999.dp),
                                    )
                                    .padding(
                                        horizontal = if (index == stepIndex) 12.dp else 5.dp,
                                        vertical = 4.dp,
                                    ),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onFinish) {
                            Text(text = "건너뛰기", color = Color(0xFFBEB29A))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (stepIndex > 0) {
                                OutlinedButton(
                                    onClick = { stepIndex -= 1 },
                                    border = BorderStroke(1.dp, Color(0xFFD6B15E).copy(alpha = 0.65f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0x6617140F),
                                        contentColor = Color(0xFFFFE0A0),
                                    ),
                                ) {
                                    Text(text = "이전")
                                }
                            }

                            Button(
                                onClick = {
                                    if (isLastStep) {
                                        onFinish()
                                    } else {
                                        stepIndex += 1
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD6B15E),
                                    contentColor = Color(0xFF17140F),
                                ),
                            ) {
                                Text(text = if (isLastStep) "시작하기" else "다음")
                            }
                        }
                    }
                }
            }
        }
    }
}
