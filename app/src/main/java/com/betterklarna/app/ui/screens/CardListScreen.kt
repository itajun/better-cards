package com.betterklarna.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.betterklarna.app.ui.components.CardItem
import com.betterklarna.app.ui.components.WalletGridCardItem
import com.betterklarna.app.ui.viewmodel.CardViewModel
import com.betterklarna.app.util.ShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardListScreen(
    viewModel: CardViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAddCard: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToBatchImport: () -> Unit
) {
    val context = LocalContext.current
    val cards by viewModel.cards.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val onlyFavorites by viewModel.onlyFavorites.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()

    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedCardIds by viewModel.selectedCardIds.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showAddFabMenu by remember { mutableStateOf(false) }

    val jsonFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importFromJsonUri(context, it) { result ->
                result.onSuccess { count ->
                    Toast.makeText(context, "Imported & normalized $count cards!", Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    Toast.makeText(context, "Import failed: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val (starredCards, otherCards) = remember(cards, searchQuery, onlyFavorites) {
        if (searchQuery.isBlank() && !onlyFavorites) {
            Pair(cards.filter { it.isFavorite }, cards.filter { !it.isFavorite })
        } else {
            Pair(emptyList(), cards)
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedCardIds.size} Selected",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Selection")
                        }
                    },
                    actions = {
                        TextButton(onClick = { viewModel.selectAll(cards) }) {
                            Text("Select All", color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = {
                                val selectedCards = cards.filter { selectedCardIds.contains(it.id) }
                                if (selectedCards.isNotEmpty()) {
                                    viewModel.exportCardsAndShare(context, selectedCards)
                                    viewModel.clearSelection()
                                }
                            },
                            enabled = selectedCardIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Export Selected")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = "Loyalty Wallet",
                            fontWeight = FontWeight.ExtraBold
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View"
                            )
                        }
                        IconButton(onClick = onNavigateToScanner) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Card")
                        }
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Import Klarna JSON File") },
                                leadingIcon = { Icon(Icons.Default.FileOpen, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    jsonFilePickerLauncher.launch(arrayOf("application/json", "*/*"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import from Screenshots") },
                                leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToBatchImport()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Export All Cards (JSON)") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    if (cards.isNotEmpty()) {
                                        viewModel.exportCardsAndShare(context, cards)
                                    } else {
                                        Toast.makeText(context, "No cards to export", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Select Cards to Export") },
                                leadingIcon = { Icon(Icons.Default.CheckCircleOutline, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.setSelectionMode(true)
                                }
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                Column(horizontalAlignment = Alignment.End) {
                    if (showAddFabMenu) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                showAddFabMenu = false
                                jsonFilePickerLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            icon = { Icon(Icons.Default.FileOpen, null) },
                            text = { Text("Import JSON File") },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        ExtendedFloatingActionButton(
                            onClick = {
                                showAddFabMenu = false
                                onNavigateToBatchImport()
                            },
                            icon = { Icon(Icons.Default.PhotoLibrary, null) },
                            text = { Text("Import Screenshots") },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        ExtendedFloatingActionButton(
                            onClick = {
                                showAddFabMenu = false
                                onNavigateToScanner()
                            },
                            icon = { Icon(Icons.Default.QrCodeScanner, null) },
                            text = { Text("Scan Card") },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        ExtendedFloatingActionButton(
                            onClick = {
                                showAddFabMenu = false
                                onNavigateToAddCard()
                            },
                            icon = { Icon(Icons.Default.Edit, null) },
                            text = { Text("Add Manually") },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    FloatingActionButton(
                        onClick = { showAddFabMenu = !showAddFabMenu }
                    ) {
                        Icon(
                            imageVector = if (showAddFabMenu) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Add Card"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Pill Search Bar
            TextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = { Text("Search store or card number...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            // Filter Chips & Select button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !onlyFavorites,
                        onClick = { if (onlyFavorites) viewModel.toggleFavoritesFilter() },
                        label = { Text("All (${cards.size})") },
                        shape = RoundedCornerShape(20.dp)
                    )
                    FilterChip(
                        selected = onlyFavorites,
                        onClick = { if (!onlyFavorites) viewModel.toggleFavoritesFilter() },
                        label = { Text("Starred (${cards.count { it.isFavorite }})") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                if (!isSelectionMode && cards.isNotEmpty()) {
                    TextButton(onClick = { viewModel.setSelectionMode(true) }) {
                        Text("Select", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (cards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching cards" else "Your wallet is empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank())
                                "Try searching with a different store name or number."
                            else
                                "Import your Klarna cards JSON file or screenshots to get all your cards with logos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { jsonFilePickerLauncher.launch(arrayOf("application/json", "*/*")) }) {
                            Icon(Icons.Default.FileOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import Klarna JSON File")
                        }
                    }
                }
            } else if (isGridView) {
                // 2-Column Wallet Grid View
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 92.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (starredCards.isNotEmpty()) {
                        item(key = "header_starred", span = { GridItemSpan(2) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "STARRED (${starredCards.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.1.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        items(starredCards, key = { "starred_${it.id}" }) { card ->
                            WalletGridCardItem(
                                card = card,
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedCardIds.contains(card.id),
                                onClick = {
                                    if (isSelectionMode) {
                                        viewModel.toggleCardSelection(card.id)
                                    } else {
                                        onNavigateToDetail(card.id)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        viewModel.setSelectionMode(true)
                                        viewModel.toggleCardSelection(card.id)
                                    }
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(card) }
                            )
                        }

                        if (otherCards.isNotEmpty()) {
                            item(key = "header_all", span = { GridItemSpan(2) }) {
                                Text(
                                    text = "ALL CARDS (${otherCards.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.1.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                )
                            }
                        }
                    }

                    items(otherCards, key = { it.id }) { card ->
                        WalletGridCardItem(
                            card = card,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedCardIds.contains(card.id),
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleCardSelection(card.id)
                                } else {
                                    onNavigateToDetail(card.id)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    viewModel.setSelectionMode(true)
                                    viewModel.toggleCardSelection(card.id)
                                }
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(card) }
                        )
                    }
                }
            } else {
                // 1-Column List View
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (starredCards.isNotEmpty()) {
                        item(key = "header_starred") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "STARRED (${starredCards.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.1.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        items(starredCards, key = { "starred_${it.id}" }) { card ->
                            CardItem(
                                card = card,
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedCardIds.contains(card.id),
                                onClick = {
                                    if (isSelectionMode) {
                                        viewModel.toggleCardSelection(card.id)
                                    } else {
                                        onNavigateToDetail(card.id)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        viewModel.setSelectionMode(true)
                                        viewModel.toggleCardSelection(card.id)
                                    }
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(card) },
                                onShare = { ShareHelper.shareViaSystem(context, card) }
                            )
                        }

                        if (otherCards.isNotEmpty()) {
                            item(key = "header_all") {
                                Text(
                                    text = "ALL CARDS (${otherCards.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.1.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                )
                            }
                        }
                    }

                    items(otherCards, key = { it.id }) { card ->
                        CardItem(
                            card = card,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedCardIds.contains(card.id),
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleCardSelection(card.id)
                                } else {
                                    onNavigateToDetail(card.id)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    viewModel.setSelectionMode(true)
                                    viewModel.toggleCardSelection(card.id)
                                }
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(card) },
                            onShare = { ShareHelper.shareViaSystem(context, card) }
                        )
                    }
                }
            }
        }
    }
}
