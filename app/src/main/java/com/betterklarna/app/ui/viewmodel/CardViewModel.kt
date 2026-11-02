package com.betterklarna.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.betterklarna.app.data.local.AppDatabase
import com.betterklarna.app.data.local.CardRepository
import com.betterklarna.app.data.model.LoyaltyCard
import com.betterklarna.app.util.ImportCandidate
import com.betterklarna.app.util.KlarnaImportHelper
import com.betterklarna.app.util.KlarnaJsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class CardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CardRepository

    init {
        val database = AppDatabase.getInstance(application)
        repository = CardRepository(database.cardDao())
        viewModelScope.launch(Dispatchers.IO) {
            repository.normalizeAndDeduplicateExisting()
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _onlyFavorites = MutableStateFlow(false)
    val onlyFavorites = _onlyFavorites.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView = _isGridView.asStateFlow()

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    // Multi-selection state for exporting
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode = _isSelectionMode.asStateFlow()

    private val _selectedCardIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedCardIds = _selectedCardIds.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val cards: StateFlow<List<LoyaltyCard>> = combine(
        _searchQuery,
        _onlyFavorites
    ) { query, favoritesOnly ->
        Pair(query, favoritesOnly)
    }.flatMapLatest { (query, favoritesOnly) ->
        if (query.isNotBlank()) {
            repository.searchCards(query)
        } else if (favoritesOnly) {
            repository.favoriteCards
        } else {
            repository.allCards
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavoritesFilter() {
        _onlyFavorites.value = !_onlyFavorites.value
    }

    fun toggleFavorite(card: LoyaltyCard) {
        viewModelScope.launch {
            repository.toggleFavorite(card.id, !card.isFavorite)
        }
    }

    fun markUsed(cardId: Long) {
        viewModelScope.launch {
            repository.markUsed(cardId)
        }
    }

    suspend fun getCardById(id: Long): LoyaltyCard? {
        return repository.getCardById(id)
    }

    fun saveCard(card: LoyaltyCard, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            if (card.id == 0L) {
                val newId = repository.insertCard(card)
                onSaved(newId)
            } else {
                repository.updateCard(card)
                onSaved(card.id)
            }
        }
    }

    fun deleteCard(card: LoyaltyCard) {
        viewModelScope.launch {
            repository.deleteCard(card)
        }
    }

    // --- Selection Mode for Exporting ---
    fun setSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
        if (!enabled) {
            _selectedCardIds.value = emptySet()
        }
    }

    fun toggleCardSelection(cardId: Long) {
        val current = _selectedCardIds.value.toMutableSet()
        if (current.contains(cardId)) {
            current.remove(cardId)
        } else {
            current.add(cardId)
        }
        _selectedCardIds.value = current
        if (current.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    fun selectAll(cardList: List<LoyaltyCard>) {
        _selectedCardIds.value = cardList.map { it.id }.toSet()
        _isSelectionMode.value = true
    }

    fun clearSelection() {
        _selectedCardIds.value = emptySet()
        _isSelectionMode.value = false
    }

    // --- Klarna JSON Import & Export ---
    fun importFromJsonUri(context: Context, uri: Uri, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Could not open file")
                val cards = KlarnaJsonHelper.parseFromInputStream(inputStream)
                if (cards.isNotEmpty()) {
                    repository.insertCards(cards)
                }
                withContext(Dispatchers.Main) {
                    onResult(Result.success(cards.size))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(e))
                }
            }
        }
    }

    fun exportCardsAndShare(context: Context, cardsToExport: List<LoyaltyCard>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileUri = KlarnaJsonHelper.createExportFile(context, cardsToExport)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Klarna Loyalty Cards Backup")
                    putExtra(Intent.EXTRA_TEXT, "Here are ${cardsToExport.size} loyalty cards from Better Cards.")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Share Cards (${cardsToExport.size} cards)").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (_: Exception) {
            }
        }
    }

    fun exportCardsToOutputStream(outputStream: OutputStream, cardsToExport: List<LoyaltyCard>) {
        viewModelScope.launch(Dispatchers.IO) {
            outputStream.use { stream ->
                val json = KlarnaJsonHelper.exportToJson(cardsToExport)
                stream.write(json.toByteArray(StandardCharsets.UTF_8))
            }
        }
    }

    // --- Batch Screenshot Import Logic ---
    private val _importCandidates = MutableStateFlow<List<ImportCandidate>>(emptyList())
    val importCandidates = _importCandidates.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    fun processScreenshots(uris: List<Uri>) {
        viewModelScope.launch {
            _isImporting.value = true
            val results = mutableListOf<ImportCandidate>()
            for (uri in uris) {
                val candidate = KlarnaImportHelper.processImageUri(getApplication(), uri)
                results.add(candidate)
            }
            _importCandidates.value = results
            _isImporting.value = false
        }
    }

    fun updateCandidateName(index: Int, newName: String) {
        val current = _importCandidates.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(name = newName)
            _importCandidates.value = current
        }
    }

    fun removeCandidate(index: Int) {
        val current = _importCandidates.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _importCandidates.value = current
        }
    }

    fun commitBatchImport(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val successful = _importCandidates.value.filter { it.isSuccess && it.cardNumber.isNotBlank() }
            val cardsToInsert = successful.map { KlarnaImportHelper.toLoyaltyCard(it) }
            if (cardsToInsert.isNotEmpty()) {
                repository.insertCards(cardsToInsert)
            }
            _importCandidates.value = emptyList()
            onComplete(cardsToInsert.size)
        }
    }

    fun clearBatchCandidates() {
        _importCandidates.value = emptyList()
    }
}
