package com.betterklarna.app.data.local

import androidx.room.TypeConverter
import com.betterklarna.app.data.model.CardBarcodeFormat

class Converters {
    @TypeConverter
    fun fromBarcodeFormat(value: CardBarcodeFormat?): String {
        return value?.name ?: CardBarcodeFormat.CODE_128.name
    }

    @TypeConverter
    fun toBarcodeFormat(value: String?): CardBarcodeFormat {
        return try {
            if (value != null) CardBarcodeFormat.valueOf(value) else CardBarcodeFormat.CODE_128
        } catch (_: Exception) {
            CardBarcodeFormat.CODE_128
        }
    }
}
