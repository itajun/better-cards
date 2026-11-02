package com.betterklarna.app.data.local

import com.betterklarna.app.data.model.LoyaltyCard
import com.betterklarna.app.util.NameNormalizer
import kotlinx.coroutines.flow.Flow

class CardRepository(private val cardDao: CardDao) {
    val allCards: Flow<List<LoyaltyCard>> = cardDao.getAllCards()
    val favoriteCards: Flow<List<LoyaltyCard>> = cardDao.getFavoriteCards()

    fun searchCards(query: String): Flow<List<LoyaltyCard>> = cardDao.searchCards(query)

    suspend fun getCardById(id: Long): LoyaltyCard? = cardDao.getCardById(id)

    suspend fun insertCard(card: LoyaltyCard): Long =
        cardDao.insertCard(card.copy(name = NameNormalizer.normalize(card.name)))

    suspend fun insertCards(cards: List<LoyaltyCard>): List<Long> =
        upsertImportedCards(cards)

    suspend fun upsertImportedCards(importedCards: List<LoyaltyCard>): List<Long> {
        val existing = cardDao.getAllCardsOnce()
        val existingByKey = existing.associateBy {
            "${NameNormalizer.normalize(it.name).lowercase()}|${it.effectiveBarcodePayload}"
        }.toMutableMap()

        val cardsToSave = mutableListOf<LoyaltyCard>()
        for (card in importedCards) {
            val normalizedName = NameNormalizer.normalize(card.name)
            val key = "${normalizedName.lowercase()}|${card.effectiveBarcodePayload}"
            val match = existingByKey[key]
            if (match != null) {
                // Preserve existing ID and favorite state, update metadata/logo/color
                cardsToSave.add(
                    card.copy(
                        id = match.id,
                        name = normalizedName,
                        isFavorite = match.isFavorite || card.isFavorite,
                        notes = card.notes.ifBlank { match.notes }
                    )
                )
            } else {
                cardsToSave.add(card.copy(name = normalizedName))
            }
        }
        return cardDao.insertCards(cardsToSave)
    }

    suspend fun normalizeAndDeduplicateExisting() {
        val existing = cardDao.getAllCardsOnce()
        if (existing.isEmpty()) return

        val seenKeys = mutableMapOf<String, LoyaltyCard>()
        val duplicateIdsToDelete = mutableListOf<Long>()
        val cardsToUpdate = mutableListOf<LoyaltyCard>()

        for (card in existing) {
            val normalizedName = NameNormalizer.normalize(card.name)
            val key = "${normalizedName.lowercase()}|${card.effectiveBarcodePayload}"

            val previous = seenKeys[key]
            if (previous != null) {
                // Duplicate found; keep previous (and preserve favorite if either was starred)
                duplicateIdsToDelete.add(card.id)
                if (card.isFavorite && !previous.isFavorite) {
                    val upgraded = previous.copy(isFavorite = true)
                    seenKeys[key] = upgraded
                    cardsToUpdate.removeAll { it.id == previous.id }
                    cardsToUpdate.add(upgraded)
                }
            } else {
                val updated = if (card.name != normalizedName) {
                    card.copy(name = normalizedName)
                } else {
                    card
                }
                seenKeys[key] = updated
                if (updated != card) {
                    cardsToUpdate.add(updated)
                }
            }
        }

        if (duplicateIdsToDelete.isNotEmpty()) {
            cardDao.deleteCardsByIds(duplicateIdsToDelete)
        }
        for (card in cardsToUpdate) {
            cardDao.updateCard(card)
        }
    }

    suspend fun updateCard(card: LoyaltyCard) =
        cardDao.updateCard(card.copy(name = NameNormalizer.normalize(card.name)))

    suspend fun deleteCard(card: LoyaltyCard) = cardDao.deleteCard(card)

    suspend fun markUsed(id: Long) = cardDao.updateLastUsed(id)

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = cardDao.toggleFavorite(id, isFavorite)
}
