package com.betterklarna.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.betterklarna.app.data.model.LoyaltyCard
import com.betterklarna.app.ui.components.BarcodeImage
import com.betterklarna.app.ui.components.BrandLogo
import com.betterklarna.app.ui.theme.*
import com.betterklarna.app.ui.viewmodel.CardViewModel
import com.betterklarna.app.util.KeepScreenBright
import com.betterklarna.app.util.ShareHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    cardId: Long,
    viewModel: CardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var card by remember { mutableStateOf<LoyaltyCard?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(cardId) {
        card = viewModel.getCardById(cardId)
        viewModel.markUsed(cardId)
    }

    // Force 100% screen brightness when viewing barcode
    KeepScreenBright()

    if (card == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentCard = card!!
    val brandColor = parseHexColor(currentCard.colorHex)
    val headerContentColor = cardContentColor(brandColor)
    val isLight = isLightCardColor(brandColor)
    val isVeryDark = isVeryDarkCardColor(brandColor)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(currentCard.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val updated = currentCard.copy(isFavorite = !currentCard.isFavorite)
                        card = updated
                        viewModel.toggleFavorite(currentCard)
                    }) {
                        Icon(
                            imageVector = if (currentCard.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Starred",
                            tint = if (currentCard.isFavorite) Color(0xFFFFB300) else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { onNavigateToEdit(currentCard.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Card")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Card")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Wallet Pass Container
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = when {
                    isLight -> BorderStroke(1.dp, Color.Black.copy(alpha = 0.14f))
                    isVeryDark -> BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    else -> null
                },
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Brand Header with Logo
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(brandColor)
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            BrandLogo(
                                logoUrl = currentCard.logoUrl,
                                cardName = currentCard.name,
                                fallbackTextColor = headerContentColor,
                                maxHeight = 54.dp,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = currentCard.name.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = headerContentColor.copy(alpha = 0.78f),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                            )
                        }
                    }

                    // Subtle divider if brand header is white
                    if (isLight) {
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.08f))
                    }

                    // High-contrast White Scan Zone
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BarcodeImage(
                            content = currentCard.effectiveBarcodePayload,
                            format = currentCard.barcodeFormat,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tap to copy Card Number pill
                        Surface(
                            color = Color(0xFFF3F4F6),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Card Number", currentCard.cardNumber)
                                    clipboard.setPrimaryClip(clip)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Copied card number")
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentCard.displayCardNumber,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111827),
                                        letterSpacing = 1.5.sp
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Card Number",
                                    tint = Color(0xFF4B5563),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Optional Member ID (e.g. Flybuys, Myer One, Qantas)
                        if (!currentCard.memberId.isNullOrBlank() && currentCard.memberId != currentCard.cardNumber) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Member ID", currentCard.memberId)
                                        clipboard.setPrimaryClip(clip)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Copied Member ID")
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Member ID: ${currentCard.memberIdFormatted ?: currentCard.memberId}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF4B5563),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Member ID",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        if (currentCard.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = currentCard.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF4B5563),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Share Card Section
            Text(
                text = "Send Card Link or Backup",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { ShareHelper.shareViaWhatsApp(context, currentCard) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp", color = Color.White, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { ShareHelper.shareViaSms(context, currentCard) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.ChatBubble, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SMS", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { ShareHelper.shareViaSystem(context, currentCard) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Link")
                }

                OutlinedButton(
                    onClick = { viewModel.exportCardsAndShare(context, listOf(currentCard)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export JSON")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Card") },
            text = { Text("Are you sure you want to delete '${currentCard.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCard(currentCard)
                        showDeleteDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
