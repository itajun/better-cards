package com.betterklarna.app.util

import android.graphics.Bitmap
import android.graphics.Color
import com.betterklarna.app.data.model.CardBarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.EnumMap

object BarcodeGenerator {
    private val writer = MultiFormatWriter()

    suspend fun generateBarcodeBitmap(
        content: String,
        format: CardBarcodeFormat,
        width: Int = 800,
        height: Int = 300
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        runCatching {
            if (content.isBlank()) {
                throw IllegalArgumentException("Barcode content cannot be blank")
            }

            val actualWidth: Int
            val actualHeight: Int
            if (format.is2D) {
                // Square dimensions for QR / Aztec / Data Matrix
                val size = minOf(width, height).coerceAtLeast(400)
                actualWidth = size
                actualHeight = size
            } else {
                actualWidth = width.coerceAtLeast(600)
                actualHeight = height.coerceAtLeast(200)
            }

            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 2)
            }

            val bitMatrix = writer.encode(
                content,
                format.zxingFormat,
                actualWidth,
                actualHeight,
                hints
            )

            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height
            val pixels = IntArray(matrixWidth * matrixHeight)

            for (y in 0 until matrixHeight) {
                val offset = y * matrixWidth
                for (x in 0 until matrixWidth) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }

            val bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, matrixWidth, 0, 0, matrixWidth, matrixHeight)
            bitmap
        }
    }
}
