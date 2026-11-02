package com.betterklarna.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.betterklarna.app.util.LogoLoader

@Composable
fun BrandLogo(
    logoUrl: String?,
    cardName: String,
    fallbackTextColor: Color,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 44.dp
) {
    val context = LocalContext.current

    var bitmap by remember(logoUrl, cardName) {
        mutableStateOf(LogoLoader.getCachedOrAssetSync(context, logoUrl, cardName))
    }

    LaunchedEffect(logoUrl, cardName) {
        if (bitmap == null && cardName.isNotBlank()) {
            bitmap = LogoLoader.loadLogo(context, logoUrl, cardName)
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap: Bitmap? = bitmap
        if (currentBitmap != null) {
            val ratio = currentBitmap.width.toFloat() / currentBitmap.height.toFloat().coerceAtLeast(1f)
            val isSquareIcon = ratio in 0.75f..1.35f &&
                    (logoUrl == null || (!logoUrl.contains("klarnacdn.net") && !logoUrl.startsWith("asset://")))

            if (isSquareIcon) {
                // Square brand icon (e.g. from domain lookup or custom photo) + bold store name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = cardName,
                        modifier = Modifier
                            .size(maxHeight. coerceAtMost(38.dp))
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.High
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = cardName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = fallbackTextColor,
                            letterSpacing = (-0.3).sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                // Wide wordmark logo (e.g. Klarna transparent PNG wordmark)
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = cardName,
                    modifier = Modifier
                        .heightIn(max = maxHeight)
                        .fillMaxWidth(0.82f),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.High
                )
            }
        } else {
            // Clean typographic wordmark when no image logo exists
            Text(
                text = cardName.ifBlank { "Card Preview" },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = fallbackTextColor,
                    letterSpacing = (-0.5).sp
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
