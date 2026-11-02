package com.betterklarna.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.betterklarna.app.data.model.CardBarcodeFormat
import com.betterklarna.app.data.model.LoyaltyCard
import com.betterklarna.app.ui.components.BarcodeImage
import com.betterklarna.app.ui.components.BrandLogo
import com.betterklarna.app.ui.components.ColorPickerRow
import com.betterklarna.app.ui.theme.*
import com.betterklarna.app.ui.viewmodel.CardViewModel
import com.betterklarna.app.util.BrandCatalog
import com.betterklarna.app.util.LogoLoader
import com.betterklarna.app.util.NameNormalizer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCardScreen(
    cardId: Long?,
    initialNumber: String?,
    initialFormat: String?,
    viewModel: CardViewModel,
    onNavigateBack: () -> Unit,
    onCardSaved: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var existingCard by remember { mutableStateOf<LoyaltyCard?>(null) }
    var name by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf(initialNumber ?: "") }
    var selectedFormat by remember {
        mutableStateOf(
            initialFormat?.let {
                runCatching { CardBarcodeFormat.valueOf(it) }.getOrNull()
            } ?: CardBarcodeFormat.CODE_128
        )
    }
    var selectedColor by remember { mutableStateOf("#1E88E5") }
    var logoUrl by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var userManuallyPickedColor by remember { mutableStateOf(false) }
    var isFormatDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(cardId) {
        if (cardId != null && cardId != 0L) {
            val loaded = viewModel.getCardById(cardId)
            loaded?.let {
                existingCard = it
                name = it.name
                cardNumber = it.cardNumber.ifBlank { it.barcodeValue }
                selectedFormat = it.barcodeFormat
                selectedColor = it.colorHex
                logoUrl = it.logoUrl
                notes = it.notes
                userManuallyPickedColor = true
            }
        }
    }

    // Custom Logo Photo Picker
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val savedLocalUrl = LogoLoader.saveCustomLogoFromUri(context, uri)
                if (savedLocalUrl != null) {
                    logoUrl = savedLocalUrl
                }
            }
        }
    }

    // Brand suggestions as user types
    val brandSuggestions = remember(name) {
        BrandCatalog.searchSuggestions(name)
    }

    val isEditing = cardId != null && cardId != 0L
    val canSave = name.isNotBlank() && cardNumber.isNotBlank()

    fun buildCardToSave(): LoyaltyCard {
        val normalizedName = NameNormalizer.normalize(name)
        val finalLogoUrl = logoUrl?.takeIf { it.isNotBlank() }
            ?: BrandCatalog.inferLogoUrlForStoreName(normalizedName)

        return (existingCard ?: LoyaltyCard(name = normalizedName)).copy(
            id = cardId ?: 0L,
            name = normalizedName,
            barcodeValue = existingCard?.barcodeValue?.takeIf {
                existingCard?.cardNumber == cardNumber.trim()
            } ?: cardNumber.trim(),
            cardNumber = cardNumber.trim(),
            barcodeFormat = selectedFormat,
            colorHex = selectedColor,
            logoUrl = finalLogoUrl,
            notes = notes.trim()
        )
    }

    val previewBgColor = parseHexColor(selectedColor)
    val previewContentColor = cardContentColor(previewBgColor)
    val previewIsLight = isLightCardColor(previewBgColor)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Card" else "Add Loyalty Card",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (canSave) {
                                viewModel.saveCard(buildCardToSave()) { savedId ->
                                    onCardSaved(savedId)
                                }
                            }
                        },
                        enabled = canSave
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save Card")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Card Header Preview (shows Logo + Color live as you type!)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp),
                shape = RoundedCornerShape(20.dp),
                border = if (previewIsLight) BorderStroke(1.dp, Color.Black.copy(alpha = 0.14f)) else null,
                colors = CardDefaults.cardColors(containerColor = previewBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BrandLogo(
                        logoUrl = logoUrl ?: BrandCatalog.inferLogoUrlForStoreName(name),
                        cardName = NameNormalizer.normalize(name.ifBlank { "Card Preview" }),
                        fallbackTextColor = previewContentColor,
                        maxHeight = 46.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Store / Card Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { typed ->
                    name = typed
                    // Automatically apply official brand color & logo if an exact brand matches
                    val exactPreset = BrandCatalog.PRESETS.find {
                        it.name.equals(typed.trim(), ignoreCase = true)
                    }
                    if (exactPreset != null) {
                        if (!userManuallyPickedColor) {
                            selectedColor = exactPreset.colorHex
                        }
                        if (logoUrl == null || !logoUrl!!.startsWith("local://")) {
                            logoUrl = exactPreset.defaultLogoUrl
                        }
                    }
                },
                label = { Text("Store or Brand Name *") },
                placeholder = { Text("e.g. Bunnings, Chemist Warehouse, Costco") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Instant Brand Autocomplete Chips
            if (brandSuggestions.isNotEmpty() && name.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    brandSuggestions.forEach { preset ->
                        AssistChip(
                            onClick = {
                                name = preset.name
                                selectedColor = preset.colorHex
                                logoUrl = preset.defaultLogoUrl
                            },
                            label = { Text(preset.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(preset.colorHex))
                                )
                            }
                        )
                    }
                }
            }

            // Custom Logo Picker Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        logoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (logoUrl?.startsWith("local://") == true) "Change Custom Logo" else "Pick Custom Logo Photo")
                }

                if (logoUrl != null) {
                    IconButton(onClick = { logoUrl = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Reset Logo")
                    }
                }
            }

            // Card Number
            OutlinedTextField(
                value = cardNumber,
                onValueChange = { cardNumber = it },
                label = { Text("Card Number / Barcode Payload *") },
                placeholder = { Text("e.g. 9312345678901") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Barcode Format Dropdown
            ExposedDropdownMenuBox(
                expanded = isFormatDropdownExpanded,
                onExpandedChange = { isFormatDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedFormat.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Barcode Format") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFormatDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )

                ExposedDropdownMenu(
                    expanded = isFormatDropdownExpanded,
                    onDismissRequest = { isFormatDropdownExpanded = false }
                ) {
                    CardBarcodeFormat.entries.forEach { format ->
                        DropdownMenuItem(
                            text = { Text(format.displayName) },
                            onClick = {
                                selectedFormat = format
                                isFormatDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Brand Color
            Text(
                text = "Card Color Theme",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            ColorPickerRow(
                selectedColorHex = selectedColor,
                onColorSelected = {
                    selectedColor = it
                    userManuallyPickedColor = true
                }
            )

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                placeholder = { Text("e.g. Linked phone number, member tier") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // Live Barcode Preview
            if (cardNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Live Barcode Preview",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                BarcodeImage(
                    content = cardNumber,
                    format = selectedFormat,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (canSave) {
                        viewModel.saveCard(buildCardToSave()) { savedId ->
                            onCardSaved(savedId)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave
            ) {
                Text(if (isEditing) "Update Card" else "Save Card to Wallet")
            }
        }
    }
}
