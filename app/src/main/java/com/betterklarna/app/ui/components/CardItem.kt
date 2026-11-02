package com.betterklarna.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.betterklarna.app.data.model.LoyaltyCard
import com.betterklarna.app.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WalletGridCardItem(
    card: LoyaltyCard,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: (() -> Unit)? = null
) {
    val cardColor = parseHexColor(card.colorHex)
    val isLight = isLightCardColor(cardColor)
    val isVeryDark = isVeryDarkCardColor(cardColor)
    val contentColor = cardContentColor(cardColor)
    val pillBg = cardPillBackgroundColor(cardColor)

    val borderStroke = when {
        isSelected -> BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
        isLight -> BorderStroke(1.dp, Color.Black.copy(alpha = 0.14f))
        isVeryDark -> BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
        else -> BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.52f)
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLight) 2.dp else 4.dp),
        shape = RoundedCornerShape(18.dp),
        border = borderStroke,
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            cardColor,
                            if (isLight) cardColor else cardColor.copy(alpha = 0.90f)
                        )
                    )
                )
                .padding(12.dp)
        ) {
            // Top Row: Selection indicator or Favorite Star
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "Selected" else "Not selected",
                        tint = if (isSelected) Color(0xFF4CAF50) else contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.size(22.dp))
                }

                if (!isSelectionMode) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(pillBg)
                    ) {
                        Icon(
                            imageVector = if (card.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (card.isFavorite) "Unstar" else "Star",
                            tint = if (card.isFavorite) Color(0xFFFFB300) else contentColor.copy(alpha = 0.75f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Center: Brand Logo or Stylized Store Name
            BrandLogo(
                logoUrl = card.logoUrl,
                cardName = card.name,
                fallbackTextColor = contentColor,
                maxHeight = 42.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(vertical = 18.dp)
            )

            // Bottom Row: Normalized Store Name + Masked/Short Number
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor.copy(alpha = 0.92f),
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                val shortNum = card.cardNumber.takeLast(4)
                if (shortNum.isNotBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "••$shortNum",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = contentColor.copy(alpha = 0.65f),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardItem(
    card: LoyaltyCard,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: (() -> Unit)? = null
) {
    val cardColor = parseHexColor(card.colorHex)
    val isLight = isLightCardColor(cardColor)
    val isVeryDark = isVeryDarkCardColor(cardColor)
    val contentColor = cardContentColor(cardColor)
    val pillBg = cardPillBackgroundColor(cardColor)

    val borderStroke = when {
        isSelected -> BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
        isLight -> BorderStroke(1.dp, Color.Black.copy(alpha = 0.14f))
        isVeryDark -> BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
        else -> BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLight) 2.dp else 4.dp),
        shape = RoundedCornerShape(18.dp),
        border = borderStroke,
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFF4CAF50) else contentColor.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(26.dp)
                        .padding(end = 10.dp)
                )
            }

            // Left: Logo badge
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(44.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BrandLogo(
                    logoUrl = card.logoUrl,
                    cardName = card.name,
                    fallbackTextColor = contentColor,
                    maxHeight = 36.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Middle: Normalized Name & Formatted Number
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = card.displayCardNumber,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = contentColor.copy(alpha = 0.78f),
                        letterSpacing = 0.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right: Share & Star actions
            if (!isSelectionMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(pillBg)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Card",
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(pillBg)
                    ) {
                        Icon(
                            imageVector = if (card.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (card.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (card.isFavorite) Color(0xFFFFB300) else contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
