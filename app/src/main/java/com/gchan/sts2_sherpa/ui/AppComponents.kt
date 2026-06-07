package com.gchan.sts2_sherpa.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gchan.sts2_sherpa.ui.theme.DangerAccent
import com.gchan.sts2_sherpa.ui.theme.GoldAccent
import com.gchan.sts2_sherpa.ui.theme.GoldAccentLight
import com.gchan.sts2_sherpa.ui.theme.SurfaceDark
import com.gchan.sts2_sherpa.ui.theme.TextPrimary
import com.gchan.sts2_sherpa.ui.theme.TextSecondary

private val PanelShape = RoundedCornerShape(12.dp)

@Composable
fun AppPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = GoldAccent.copy(alpha = 0.72f),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = PanelShape,
        color = SurfaceDark,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 6.dp,
        content = content,
    )
}

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GoldAccent,
            contentColor = Color(0xFF17140F),
            disabledContainerColor = GoldAccent.copy(alpha = 0.28f),
            disabledContentColor = TextSecondary,
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
    ) {
        ActionButtonContent(text = text, icon = icon)
    }
}

@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.75f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xAA1D180E),
            contentColor = GoldAccentLight,
            disabledContainerColor = Color(0x661D180E),
            disabledContentColor = TextSecondary,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    ) {
        ActionButtonContent(text = text, icon = icon)
    }
}

@Composable
fun DangerActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, DangerAccent),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xAA1D1110),
            contentColor = DangerAccent,
            disabledContainerColor = Color(0x661D1110),
            disabledContentColor = TextSecondary,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    ) {
        ActionButtonContent(text = text, icon = icon)
    }
}

@Composable
fun IconCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = GoldAccentLight,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .background(Color(0xCC17140F), RoundedCornerShape(999.dp))
            .border(1.dp, GoldAccent.copy(alpha = 0.7f), RoundedCornerShape(999.dp)),
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
    }
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = GoldAccentLight,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    AppPanel(modifier = modifier) {
        Box(modifier = Modifier.padding(18.dp)) {
            androidx.compose.foundation.layout.Column {
                Icon(imageVector = icon, contentDescription = null, tint = GoldAccent, modifier = Modifier.padding(bottom = 8.dp))
                Text(title, color = GoldAccentLight, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun RowScope.ActionButtonContent(
    text: String,
    icon: ImageVector?,
) {
    if (icon != null) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
    }
    Text(text = text, maxLines = 1, fontWeight = FontWeight.SemiBold)
}

fun Modifier.pressableCard(onClick: () -> Unit): Modifier =
    clickable(
        interactionSource = MutableInteractionSource(),
        indication = null,
        onClick = onClick,
    )

