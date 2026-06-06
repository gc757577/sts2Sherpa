package com.gchan.sts2_sherpa.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
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
import com.gchan.sts2_sherpa.logic.RecommendationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    onImageSelected: (Uri) -> Unit,
    onOcrMessageShown: () -> Unit,
    onOcrResultSlotClick: (Int) -> Unit,
    onOcrResultCardPicked: (SilentCard) -> Unit,
    onDismissOcrResultCardPicker: () -> Unit,
    onConfirmOcrResult: () -> Unit,
    onCancelOcrResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var pendingCameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(onImageSelected) }
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
            val imageUri = createCameraImageUri(context)
            pendingCameraImageUri = imageUri
            cameraLauncher.launch(imageUri)
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
            uiState.isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Gold,
            )

            uiState.errorMessage != null -> Text(
                text = uiState.errorMessage,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                color = Color(0xFFFFB4AB),
            )

            else -> RewardSelectionContent(
                uiState = uiState,
                onRewardSlotClick = onRewardSlotClick,
                onRewardCardClick = onRewardCardClick,
                onDeckClick = onDeckClick,
                onSkipClick = onSkipClick,
                onRecognizeImageClick = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onCaptureCameraClick = {
                    if (
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val imageUri = createCameraImageUri(context)
                        pendingCameraImageUri = imageUri
                        cameraLauncher.launch(imageUri)
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
            )
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
    onRecognizeImageClick: () -> Unit,
    onCaptureCameraClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recommendedCardId = uiState.recommendationResult?.recommendedCard?.id

    Box(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
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
            onRecognizeImageClick = onRecognizeImageClick,
            onCaptureCameraClick = onCaptureCameraClick,
            isRecognizing = uiState.isRecognizing,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun BottomActionButtons(
    onSkipClick: () -> Unit,
    onRecognizeImageClick: () -> Unit,
    onCaptureCameraClick: () -> Unit,
    isRecognizing: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RecognitionButton(
                text = if (isRecognizing) "인식 중..." else "사진에서 인식",
                enabled = !isRecognizing,
                onClick = onRecognizeImageClick,
                modifier = Modifier.weight(1f),
            )
            RecognitionButton(
                text = if (isRecognizing) "인식 중..." else "카메라로 촬영",
                enabled = !isRecognizing,
                onClick = onCaptureCameraClick,
                modifier = Modifier.weight(1f),
            )
        }

        OutlinedButton(
            onClick = onSkipClick,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Gold),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = PanelDark,
                contentColor = GoldLight,
            ),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(
                text = "스킵",
                modifier = Modifier.padding(vertical = 5.dp),
                fontWeight = FontWeight.SemiBold,
            )
        }
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xCC0B1015),
        border = BorderStroke(1.dp, Color(0xFF4D6B82)),
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "✨ 추천 결과 ✨",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD9ECFF),
            )
            Text(
                text = "추천 카드: ${result.recommendedCard.displayName()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                text = "점수: ${result.recommendedScore.score}점",
                style = MaterialTheme.typography.bodyMedium,
                color = BoneText,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "추천 이유:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                result.recommendedScore.reasons.forEach { reason ->
                    Text(
                        text = "- $reason",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE6EEF5),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "후보 점수:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                result.candidateScores.forEach { candidateScore ->
                    Text(
                        text = "- ${candidateScore.card.displayName()}: ${candidateScore.score}점",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE6EEF5),
                    )
                }
            }
        }
    }
}

@Composable
private fun CardPickerDialog(
    cards: List<SilentCard>,
    onCardPicked: (SilentCard) -> Unit,
    onDismissRequest: () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "카드 선택",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Button(onClick = onDismissRequest) {
                        Text(text = "닫기")
                    }
                }

                CardGrid(
                    cards = cards,
                    onCardClick = onCardPicked,
                    modifier = Modifier.fillMaxSize(),
                    minCellSize = 128.dp,
                )
            }
        }
    }
}

@Composable
private fun DeckDialog(
    deckCards: List<DeckCard>,
    onDismissRequest: () -> Unit,
) {
    var selectedFilter by remember { mutableStateOf(DeckFilter.All) }
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
                            text = "표시할 카드가 없습니다.",
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
                            DeckPoolCardItem(deckCard = deckCard)
                        }
                    }
                }

                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "닫기")
                }
            }
        }
    }
}

@Composable
private fun DeckPoolCardItem(
    deckCard: DeckCard,
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
            if (deckCard.count > 1) {
                Text(
                    text = "x${deckCard.count}",
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AssetCardImage(
                path = card.image,
                contentDescription = card.displayName(),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f),
            )
            Text(
                text = card.displayName(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Cost ${card.cost ?: "-"} / ${card.type.ifBlank { "Unknown" }}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Tier ${card.beginnerTier.ifBlank { "-" }} / ${card.rarity.ifBlank { "Unknown" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val context = LocalContext.current
    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(path).use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Text(
                text = "No image",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun SilentCard.displayName(): String = nameKo.ifBlank { id }

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
