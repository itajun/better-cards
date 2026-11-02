package com.betterklarna.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loyalty_cards")
data class LoyaltyCard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val barcodeValue: String = "",
    val barcodeFormat: CardBarcodeFormat = CardBarcodeFormat.CODE_128,
    val cardNumber: String = "",
    val cardNumberFormatted: String? = null,
    val memberId: String? = null,
    val memberIdFormatted: String? = null,
    val colorHex: String = "#1E88E5",
    val logoUrl: String? = null,
    val isCustomCard: Boolean = false,
    val addedVia: String? = "MANUAL",
    val klarnaCardId: String? = null,
    val klarnaProviderId: String? = null,
    val market: String? = "AU",
    val notes: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis()
) {
    // Returns the exact string to be encoded into the barcode graphic
    val effectiveBarcodePayload: String
        get() = barcodeValue.ifBlank { cardNumber }

    // Returns human readable card number
    val displayCardNumber: String
        get() = cardNumberFormatted?.ifBlank { cardNumber } ?: cardNumber.ifBlank { barcodeValue }
}
