package com.gchan.sts2_sherpa.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.gchan.sts2_sherpa.data.DeckCard
import com.gchan.sts2_sherpa.data.SilentCard
import com.gchan.sts2_sherpa.logic.CandidateScore
import com.gchan.sts2_sherpa.logic.DeckAnalysis
import com.gchan.sts2_sherpa.logic.RecommendationAction
import com.gchan.sts2_sherpa.logic.RecommendationResult
import com.gchan.sts2_sherpa.util.KoreanInitialUtils
import java.io.File

private val DungeonTop = Color(0xFF05070B)
private val DungeonMiddle = Color(0xFF101B18)
private val DungeonBottom = Color(0xFF071018)
private val PanelDark = Color(0xE617140F)
private val CardDark = Color(0xFF19150F)
private val Gold = Color(0xFFD6B15E)
private val GoldLight = Color(0xFFFFE0A0)
private val BoneText = Color(0xFFE8D7A2)
private val MutedText = Color(0xFFBEB29A)
private val RecommendGlow = Color(0xFF6FB7FF)

@Composable
fun MainScreen(
    uiState: MainUiState,
    onRewardSlotClick: (Int) -> Unit,
    onRewardCardClick: (SilentCard) -> Unit,
    onCardPicked: (SilentCard) -> Unit,
    onDismissCardPicker: () -> Unit,
    onDeckClick: () -> Unit,
    onDismissDeck: () -> Unit,
    onSkipClick: () -> Unit,
    onRemoveDeckCard: (String) -> Unit,
    onResetDeck: () -> Unit,
    onImageSelected: (Uri) -> Unit,
    onOcrMessageShown: () -> Unit,
    onOcrResultSlotClick: (Int) -> Unit,
    onOcrResultCardPicked: (SilentCard) -> Unit,
    onDismissOcrResultCardPicker: () -> Unit,
    onConfirmOcrResult: () -> Unit,
    onCancelOcrResult: () -> Unit,
    onRetryLoad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var pendingCameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var isHelpDialogOpen by remember { mutableStateOf(false) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { isSuccess ->
        val imageUri = pendingCameraImageUri
        if (isSuccess && imageUri != null) {
            onImageSelected(imageUri)
        } else {
            Toast.makeText(context, "촬영이 취소되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            runCatching { createCameraImageUri(context) }
                .onSuccess { imageUri ->
                    pendingCameraImageUri = imageUri
                    cameraLauncher.launch(imageUri)
                }
                .onFailure {
                    Toast.makeText(context, "카메라를 실행하는 중 문제가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.ocrMessage) {
        val message = uiState.ocrMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        onOcrMessageShown()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DungeonTop, DungeonMiddle, DungeonBottom),
                ),
            ),
    ) {
        when {
            uiState.isLoading -> LoadingContent(
                modifier = Modifier.align(Alignment.Center),
            )

            uiState.errorMessage != null -> ErrorContent(
                message = uiState.errorMessage,
                onRetryClick = onRetryLoad,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            )

            else -> RewardSelectionContent(
                uiState = uiState,
                onRewardSlotClick = onRewardSlotClick,
                onRewardCardClick = onRewardCardClick,
                onDeckClick = onDeckClick,
                onSkipClick = onSkipClick,
                onHelpClick = { isHelpDialogOpen = true },
                onCaptureCameraClick = {
                    if (
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        runCatching { createCameraImageUri(context) }
                            .onSuccess { imageUri ->
                                pendingCameraImageUri = imageUri
                                cameraLauncher.launch(imageUri)
                            }
                            .onFailure {
                                Toast.makeText(context, "카메라를 실행하는 중 문제가 발생했습니다.", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (uiState.pickerSlotIndex != null) {
            CardPickerDialog(
                cards = uiState.cards,
                onCardPicked = onCardPicked,
                onDismissRequest = onDismissCardPicker,
            )
        }

        if (uiState.isOcrResultDialogOpen) {
            OcrResultDialog(
                pendingCards = uiState.pendingRecognizedCards,
                ocrRawText = uiState.ocrRawText,
                onSlotClick = onOcrResultSlotClick,
                onConfirm = onConfirmOcrResult,
                onCancel = onCancelOcrResult,
            )
        }

        if (uiState.ocrPickerSlotIndex != null) {
            CardPickerDialog(
                cards = uiState.cards,
                onCardPicked = onOcrResultCardPicked,
                onDismissRequest = onDismissOcrResultCardPicker,
            )
        }

        if (uiState.isDeckDialogOpen) {
            DeckDialog(
                deckCards = uiState.currentDeck,
                onDismissRequest = onDismissDeck,
                onRemoveDeckCard = onRemoveDeckCard,
                onResetDeck = onResetDeck,
            )
        }

        if (isHelpDialogOpen) {
            HelpDialog(
                onDismissRequest = { isHelpDialogOpen = false },
            )
        }
    }
}

@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(color = Gold)
        Text(
            text = "카드 데이터를 불러오는 중...",
            style = MaterialTheme.typography.bodyMedium,
            color = BoneText,
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = PanelDark,
        border = BorderStroke(1.dp, Color(0xFFFFB4AB)),
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message.ifBlank { "카드 데이터를 불러오지 못했습니다." },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFDAD6),
                textAlign = TextAlign.Center,
            )
            OutlinedButton(
                onClick = onRetryClick,
                border = BorderStroke(1.dp, Gold),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xCC0B1015),
                    contentColor = GoldLight,
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(text = "재시도")
            }
        }
    }
}

@Composable
private fun RewardSelectionContent(
    uiState: MainUiState,
    onRewardSlotClick: (Int) -> Unit,
    onRewardCardClick: (SilentCard) -> Unit,
    onDeckClick: () -> Unit,
    onSkipClick: () -> Unit,
    onHelpClick: () -> Unit,
    onCaptureCameraClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recommendedCardId = uiState.recommendationResult
        ?.takeIf { it.action == RecommendationAction.PICK_CARD }
        ?.recommendedCard
        ?.id

    Box(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        HelpButton(
            onClick = onHelpClick,
            modifier = Modifier.align(Alignment.TopStart),
        )

        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DeckCountBadge(
                deckCardCount = uiState.deckCardCount,
                onClick = onDeckClick,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = if (uiState.recommendationResult == null) 76.dp else 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = "3장의 보상 카드를 선택하거나 카메라로 인식해 추천을 받아보세요.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = BoneText,
                textAlign = TextAlign.Center,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                uiState.selectedRewardCards.forEachIndexed { index, card ->
                    RewardCardSlot(
                        card = card,
                        isRecommended = card != null && card.id == recommendedCardId,
                        onEmptyClick = { onRewardSlotClick(index) },
                        onCardClick = onRewardCardClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            uiState.recommendationResult?.let { result ->
                RecommendationPanel(
                    result = result,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        BottomActionButtons(
            onSkipClick = onSkipClick,
            onCaptureCameraClick = onCaptureCameraClick,
            isRecognizing = uiState.isRecognizing,
            isSkipRecommended = uiState.recommendationResult?.action == RecommendationAction.SKIP,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun BottomActionButtons(
    onSkipClick: () -> Unit,
    onCaptureCameraClick: () -> Unit,
    isRecognizing: Boolean,
    isSkipRecommended: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RecognitionButton(
            text = if (isRecognizing) "인식 중..." else "카메라로 카드 인식",
            enabled = !isRecognizing,
            onClick = onCaptureCameraClick,
            modifier = Modifier.fillMaxWidth(),
        )

        SkipButton(
            isSkipRecommended = isSkipRecommended,
            onClick = onSkipClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SkipButton(
    isSkipRecommended: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skip-button-pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSkipRecommended) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skip-button-scale",
    )
    val borderColor = if (isSkipRecommended) Color(0xFFFFE68A) else Gold
    val containerColor = if (isSkipRecommended) Color(0x665B4521) else PanelDark
    val contentColor = if (isSkipRecommended) Color(0xFFFFF0B8) else GoldLight

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = pulseScale
            scaleY = pulseScale
        },
        border = BorderStroke(if (isSkipRecommended) 2.dp else 1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            text = if (isSkipRecommended) "✨ 스킵 추천" else "스킵",
            modifier = Modifier.padding(vertical = 5.dp),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RecognitionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        border = BorderStroke(1.dp, Color(0xFF4D6B82)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xCC0B1015),
            contentColor = Color(0xFFD9ECFF),
            disabledContainerColor = Color(0x880B1015),
            disabledContentColor = Color(0xFF7F919F),
        ),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 5.dp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DeckCountBadge(
    deckCardCount: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = PanelDark,
        border = BorderStroke(1.dp, Gold),
        tonalElevation = 4.dp,
    ) {
        Text(
            text = "🃏 $deckCardCount",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            color = GoldLight,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun HelpButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = PanelDark,
        border = BorderStroke(1.dp, Gold),
        tonalElevation = 4.dp,
    ) {
        Text(
            text = "?",
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            color = GoldLight,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun RewardCardSlot(
    card: SilentCard?,
    isRecommended: Boolean,
    onEmptyClick: () -> Unit,
    onCardClick: (SilentCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (card == null) {
            EmptyRewardSlot(
                onClick = onEmptyClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f),
            )
            Text(
                text = "카드를 선택하세요",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MutedText,
                maxLines = 2,
            )
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCardClick(card) },
                border = BorderStroke(
                    width = if (isRecommended) 3.dp else 1.dp,
                    color = if (isRecommended) RecommendGlow else Gold,
                ),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
            ) {
                Column(
                    modifier = Modifier.padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    AssetCardImage(
                        path = card.image,
                        contentDescription = card.displayName(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.68f),
                    )
                    Text(
                        text = if (isRecommended) "추천: ${card.displayName()}" else card.displayName(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = if (isRecommended) Color(0xFFD9ECFF) else BoneText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRewardSlot(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .background(CardDark, shape)
            .clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    color = Gold,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f)),
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                        x = 8.dp.toPx(),
                        y = 8.dp.toPx(),
                    ),
                )
            }
            .border(
                border = BorderStroke(1.dp, Color(0xFF51452F)),
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+",
            style = MaterialTheme.typography.displaySmall,
            color = GoldLight,
            fontWeight = FontWeight.Light,
        )
    }
}

@Composable
private fun RecommendationPanel(
    result: RecommendationResult,
    modifier: Modifier = Modifier,
) {
    val isSkipRecommended = result.action == RecommendationAction.SKIP
    val recommendedCardId = result.recommendedCard?.id
    var isScoreDetailVisible by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xCC0B1015),
        border = BorderStroke(1.dp, Color(0xFF4D6B82)),
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 340.dp)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (isSkipRecommended) "스킵 추천" else "✨ 추천 결과 ✨",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD9ECFF),
            )
            if (isSkipRecommended) {
                Text(
                    text = "이번 보상은 넘기는 편이 좋습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Text(
                    text = "그래도 원하면 후보 카드를 선택해 덱에 추가할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                )
                Text(
                    text = "이번 보상은 카드를 추가하지 않는 선택이 더 안정적입니다. 아래 스킵 버튼을 눌러 넘길 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldLight,
                )
            } else {
                Text(
                    text = "추천 카드: ${result.recommendedCard?.displayName().orEmpty()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Text(
                    text = "점수: ${result.recommendedScore?.score ?: 0}점",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BoneText,
                )
            }

            DeckAnalysisSection(deckAnalysis = result.deckAnalysis)
            ReasonSection(
                title = if (isSkipRecommended) "스킵 이유:" else "추천 이유:",
                reasons = result.reasons,
            )
            CandidateScoreSection(
                candidateScores = result.candidateScores,
                recommendedCardId = if (isSkipRecommended) null else recommendedCardId,
            )

            result.recommendedScore?.let { recommendedScore ->
                TextButton(
                    onClick = { isScoreDetailVisible = !isScoreDetailVisible },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (isScoreDetailVisible) "점수 상세 접기" else "점수 상세 보기",
                        color = GoldLight,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (isScoreDetailVisible) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        recommendedScore.scoreBreakdown.forEach { breakdown ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = Color(0x6621262D),
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Color(0x334D6B82),
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = "${breakdown.label}: ${breakdown.score.signedScore()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BoneText,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = breakdown.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE6EEF5),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckAnalysisSection(deckAnalysis: DeckAnalysis) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0x6621262D),
                shape = RoundedCornerShape(8.dp),
            )
            .border(
                width = 1.dp,
                color = Color(0x334D6B82),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "현재 덱 분석",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        Text(
            text = "카드 수: ${deckAnalysis.deckSize}장",
            style = MaterialTheme.typography.bodySmall,
            color = BoneText,
        )
        Text(
            text = "주요 태그: ${deckAnalysis.topTagSummary()}",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFE6EEF5),
        )
        Text(
            text = "부족한 역할: ${deckAnalysis.missingRoles.joinToString(", ")}",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFE6EEF5),
        )
    }
}

@Composable
private fun ReasonSection(
    title: String,
    reasons: List<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        reasons.forEach { reason ->
            Text(
                text = "- $reason",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE6EEF5),
            )
        }
    }
}

@Composable
private fun CandidateScoreSection(
    candidateScores: List<CandidateScore>,
    recommendedCardId: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "후보 점수 비교",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        candidateScores.forEach { candidateScore ->
            CandidateScoreRow(
                candidateScore = candidateScore,
                isRecommended = candidateScore.card.id == recommendedCardId,
            )
        }
    }
}

@Composable
private fun CandidateScoreRow(
    candidateScore: CandidateScore,
    isRecommended: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isRecommended) Color(0x55385F8E) else Color(0x55171510),
                shape = RoundedCornerShape(8.dp),
            )
            .border(
                width = 1.dp,
                color = if (isRecommended) RecommendGlow else Color(0x3351452F),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = candidateScore.card.displayName(),
                style = MaterialTheme.typography.bodySmall,
                color = if (isRecommended) Color(0xFFD9ECFF) else BoneText,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "티어 ${candidateScore.card.beginnerTier.ifBlank { "-" }} · ${candidateScore.card.previewTags()}",
                style = MaterialTheme.typography.labelSmall,
                color = MutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "${candidateScore.score}점",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isRecommended) Color(0xFFD9ECFF) else Color(0xFFE6EEF5),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CardPickerDialog(
    cards: List<SilentCard>,
    onCardPicked: (SilentCard) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CardPickerFilter.All) }
    var sortMode by remember { mutableStateOf(CardSortMode.Tier) }
    val visibleCards = remember(cards, searchQuery, selectedFilter, sortMode) {
        cards
            .asSequence()
            .filter { selectedFilter.matches(it) }
            .filter { it.matchesCardSearch(searchQuery) }
            .sortedWith(sortMode.comparator)
            .toList()
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(12.dp),
            color = PanelDark,
            border = BorderStroke(2.dp, Gold),
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "카드 선택",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Gold,
                    )
                    TextButton(onClick = onDismissRequest) {
                        Text(
                            text = "X",
                            color = GoldLight,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("카드 이름 또는 초성 검색") },
                    singleLine = true,
                )

                CardPickerFilterTabs(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
                )

                CardSortTabs(
                    selectedSortMode = sortMode,
                    onSortSelected = { sortMode = it },
                )

                if (visibleCards.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "검색 결과가 없습니다",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedText,
                        )
                    }
                } else {
                    CardGrid(
                        cards = visibleCards,
                        onCardClick = onCardPicked,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        minCellSize = 118.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CardPickerFilterTabs(
    selectedFilter: CardPickerFilter,
    onFilterSelected: (CardPickerFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CardPickerFilter.entries.forEach { filter ->
            SelectablePickerChip(
                text = filter.label,
                isSelected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
            )
        }
    }
}

@Composable
private fun CardSortTabs(
    selectedSortMode: CardSortMode,
    onSortSelected: (CardSortMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CardSortMode.entries.forEach { sortMode ->
            SelectablePickerChip(
                text = sortMode.label,
                isSelected = sortMode == selectedSortMode,
                onClick = { onSortSelected(sortMode) },
            )
        }
    }
}

@Composable
private fun SelectablePickerChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        modifier = Modifier
            .background(
                color = if (isSelected) Color(0xFF5B4521) else Color(0xFF292319),
                shape = RoundedCornerShape(8.dp),
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Gold else Color(0xFF51452F),
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        color = if (isSelected) GoldLight else MutedText,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
    )
}

@Composable
private fun DeckDialog(
    deckCards: List<DeckCard>,
    onDismissRequest: () -> Unit,
    onRemoveDeckCard: (String) -> Unit,
    onResetDeck: () -> Unit,
) {
    var selectedFilter by remember { mutableStateOf(DeckFilter.All) }
    var isResetConfirmOpen by remember { mutableStateOf(false) }
    val totalCardCount = deckCards.sumOf { it.count }
    val filteredDeckCards = deckCards.filter { selectedFilter.matches(it.card) }
    val selectedTabColor = Color(0xFF5B4521)
    val unselectedTabColor = Color(0xFF292319)

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(12.dp),
            color = PanelDark,
            border = BorderStroke(2.dp, Gold),
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "내 카드 풀",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Gold,
                        )
                        Text(
                            text = "${totalCardCount}장",
                            style = MaterialTheme.typography.titleMedium,
                            color = BoneText,
                        )
                    }
                    TextButton(onClick = onDismissRequest) {
                        Text(
                            text = "X",
                            color = GoldLight,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DeckFilter.entries.forEach { filter ->
                        val isSelected = filter == selectedFilter
                        Text(
                            text = filter.label,
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) selectedTabColor else unselectedTabColor,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Gold else Color(0xFF51452F),
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            color = if (isSelected) GoldLight else MutedText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }

                if (filteredDeckCards.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (deckCards.isEmpty()) {
                                "현재 덱이 비어 있습니다."
                            } else {
                                "표시할 카드가 없습니다."
                            },
                            color = MutedText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filteredDeckCards, key = { it.card.id }) { deckCard ->
                            DeckPoolCardItem(
                                deckCard = deckCard,
                                onRemoveClick = { onRemoveDeckCard(deckCard.card.id) },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { isResetConfirmOpen = true },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color(0xFFB36A4A)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xAA1D1110),
                            contentColor = Color(0xFFFFC0A8),
                        ),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(text = "초기화")
                    }
                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(text = "닫기")
                    }
                }
            }
        }
    }

    if (isResetConfirmOpen) {
        AlertDialog(
            onDismissRequest = { isResetConfirmOpen = false },
            containerColor = PanelDark,
            titleContentColor = Gold,
            textContentColor = Color(0xFFE6EEF5),
            title = {
                Text(
                    text = "덱 초기화",
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Text(text = "현재 덱을 사일런트 시작 덱으로 되돌릴까요?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetDeck()
                        isResetConfirmOpen = false
                    },
                ) {
                    Text(
                        text = "초기화",
                        color = Color(0xFFFFC0A8),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { isResetConfirmOpen = false }) {
                    Text(text = "취소", color = MutedText)
                }
            },
        )
    }
}

@Composable
private fun DeckPoolCardItem(
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
            AssetCardImage(
                path = deckCard.card.image,
                contentDescription = deckCard.card.displayName(),
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(3.dp)
                    .background(
                        color = Color(0xDD05070B),
                        shape = RoundedCornerShape(999.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xAAFFFFFF),
                        shape = RoundedCornerShape(999.dp),
                    )
                    .clickable(onClick = onRemoveClick)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "×",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (deckCard.count > 1) {
                Text(
                    text = "x${deckCard.count}",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .background(
                            color = Color(0xDD21180D),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .border(
                            width = 1.dp,
                            color = Gold,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    color = GoldLight,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            text = deckCard.card.displayName(),
            color = BoneText,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private enum class DeckFilter(val label: String) {
    All("전체"),
    Attack("공격"),
    Block("방어"),
    Skill("스킬"),
    Power("파워"),
    Status("상태");

    fun matches(card: SilentCard): Boolean =
        when (this) {
            All -> true
            Attack -> card.type == "Attack"
            Block -> "block" in card.allTags
            Skill -> card.type == "Skill"
            Power -> card.type == "Power"
            Status -> card.type == "Status" || card.typeKo == "상태"
        }
}

private enum class CardPickerFilter(val label: String) {
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

private enum class CardSortMode(
    val label: String,
    val comparator: Comparator<SilentCard>,
) {
    Tier(
        label = "티어순",
        comparator = compareBy<SilentCard> { it.beginnerTier.tierRank() }
            .thenBy { it.displayName() },
    ),
    Name(
        label = "이름순",
        comparator = compareBy { it.displayName() },
    ),
    Cost(
        label = "비용순",
        comparator = compareBy<SilentCard> { it.cost ?: Int.MAX_VALUE }
            .thenBy { it.beginnerTier.tierRank() }
            .thenBy { it.displayName() },
    );
}

@Composable
private fun CardGrid(
    cards: List<SilentCard>,
    onCardClick: (SilentCard) -> Unit,
    modifier: Modifier = Modifier,
    minCellSize: androidx.compose.ui.unit.Dp = 156.dp,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minCellSize),
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(cards, key = { it.id }) { card ->
            SilentCardItem(
                card = card,
                onClick = { onCardClick(card) },
            )
        }
    }
}

@Composable
private fun SilentCardItem(
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
        colors = CardDefaults.cardColors(
            containerColor = CardDark,
        ),
    ) {
        Column(
            modifier = Modifier.padding(7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box {
                AssetCardImage(
                    path = card.image,
                    contentDescription = card.displayName(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.72f),
                )
                Text(
                    text = card.beginnerTier.ifBlank { "?" },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(
                            color = Color(0xDD21180D),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .border(
                            width = 1.dp,
                            color = Gold,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    color = GoldLight,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = card.displayName(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = BoneText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "비용 ${card.cost?.toString() ?: "-"} · ${card.typeLabel()}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE6EEF5),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "티어 ${card.beginnerTier.ifBlank { "-" }} · ${card.rarityKo.ifBlank { card.rarity.ifBlank { "-" } }}",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AssetCardImage(
    path: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    CardAssetImage(
        path = path,
        contentDescription = contentDescription,
        fallbackText = contentDescription,
        modifier = modifier,
    )
}

private fun SilentCard.displayName(): String = nameKo.ifBlank { id }

private fun SilentCard.matchesCardSearch(query: String): Boolean {
    val normalizedQuery = KoreanInitialUtils.normalizeSearchText(query)
    if (normalizedQuery.isBlank()) return true

    val searchableValues = listOf(
        nameKo,
        normalizedName,
        KoreanInitialUtils.getKoreanInitials(nameKo),
    ).map(KoreanInitialUtils::normalizeSearchText)

    return searchableValues.any { it.contains(normalizedQuery) }
}

private fun SilentCard.typeLabel(): String = typeKo.ifBlank {
    when (type) {
        "Attack" -> "공격"
        "Skill" -> "스킬"
        "Power" -> "파워"
        "Status" -> "상태"
        else -> type.ifBlank { "-" }
    }
}

private fun SilentCard.previewTags(): String =
    allTags.distinct()
        .take(3)
        .joinToString(", ")
        .ifBlank { "-" }

private fun String.tierRank(): Int =
    when (uppercase()) {
        "S" -> 0
        "A" -> 1
        "B" -> 2
        "C" -> 3
        "D" -> 4
        else -> 5
    }

private fun DeckAnalysis.topTagSummary(): String =
    topTags.joinToString(", ") { (tag, count) -> "$tag $count" }
        .ifBlank { "-" }

private fun Int.signedScore(): String = if (this > 0) "+$this" else toString()

private fun createCameraImageUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply {
        mkdirs()
    }
    val imageFile = File(imagesDir, "camera_capture.jpg").apply {
        if (exists()) delete()
        createNewFile()
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile,
    )
}
