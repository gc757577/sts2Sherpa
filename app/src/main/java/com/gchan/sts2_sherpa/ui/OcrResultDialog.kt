package com.gchan.sts2_sherpa.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gchan.sts2_sherpa.data.SilentCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun OcrResultDialog(
    pendingCards: List<SilentCard?>,
    ocrRawText: String,
    onSlotClick: (Int) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    var isRawTextVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF17140F),
            border = BorderStroke(2.dp, Color(0xFFD6B15E)),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "인식 결과 확인",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFE0A0),
                )
                Text(
                    text = "인식된 카드가 맞는지 확인해주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE8D7A2),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    repeat(3) { index ->
                        OcrResultSlot(
                            card = pendingCards.getOrNull(index),
                            onClick = { onSlotClick(index) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (ocrRawText.isNotBlank()) {
                    TextButton(onClick = { isRawTextVisible = !isRawTextVisible }) {
                        Text(
                            text = if (isRawTextVisible) "OCR 원문 숨기기" else "OCR 원문 보기",
                            color = Color(0xFFD9ECFF),
                        )
                    }
                    if (isRawTextVisible) {
                        Text(
                            text = ocrRawText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(0xAA0B1015),
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE6EEF5),
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color(0xFFD6B15E)),
                    ) {
                        Text(
                            text = "취소",
                            color = Color(0xFFFFE0A0),
                        )
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = "적용")
                    }
                }
            }
        }
    }
}

@Composable
private fun OcrResultSlot(
    card: SilentCard?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (card == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
                    .background(
                        color = Color(0xFF19150F),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFFD6B15E),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color(0xFFFFE0A0),
                )
            }
            Text(
                text = "직접 선택",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFBEB29A),
                textAlign = TextAlign.Center,
            )
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFD6B15E)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF19150F)),
            ) {
                Column(
                    modifier = Modifier.padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    OcrAssetCardImage(
                        path = card.image,
                        contentDescription = card.displayName(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.68f),
                    )
                    Text(
                        text = card.displayName(),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE8D7A2),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun OcrAssetCardImage(
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
                color = Color(0xFFBEB29A),
            )
        }
    }
}

private fun SilentCard.displayName(): String = nameKo.ifBlank { id }
