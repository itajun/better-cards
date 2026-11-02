package com.betterklarna.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.betterklarna.app.data.model.CardBarcodeFormat
import com.betterklarna.app.data.model.LoyaltyCard
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.text.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}

data class ImportCandidate(
    val uri: Uri? = null,
    val name: String,
    val cardNumber: String,
    val format: CardBarcodeFormat,
    val colorHex: String = "#1E88E5",
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

object KlarnaImportHelper {
    private val barcodeScanner = BarcodeScanning.getClient()
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val IGNORED_WORDS = setOf(
        "klarna", "loyalty", "card", "cards", "membership", "barcode", "scan",
        "details", "number", "add", "apple", "google", "wallet", "points", "balance"
    )

    private val BRAND_COLORS = listOf(
        "#E53935", // Red
        "#D81B60", // Pink
        "#8E24AA", // Purple
        "#3949AB", // Indigo
        "#1E88E5", // Blue
        "#00897B", // Teal
        "#43A047", // Green
        "#FB8C00", // Orange
        "#F4511E", // Deep Orange
        "#546E7A"  // Blue Grey
    )

    suspend fun processImageUri(context: Context, uri: Uri): ImportCandidate = withContext(Dispatchers.IO) {
        try {
            val bitmap = loadBitmapFromUri(context, uri)
            val image = InputImage.fromBitmap(bitmap, 0)

            // 1. Scan for Barcode
            val barcodes = barcodeScanner.process(image).await()
            val primaryBarcode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }

            if (primaryBarcode == null || primaryBarcode.rawValue.isNullOrBlank()) {
                return@withContext ImportCandidate(
                    uri = uri,
                    name = "Unknown Card",
                    cardNumber = "",
                    format = CardBarcodeFormat.CODE_128,
                    isSuccess = false,
                    errorMessage = "No barcode detected in screenshot"
                )
            }

            val rawCardNumber = primaryBarcode.rawValue!!.trim()
            val detectedFormat = CardBarcodeFormat.fromMlKit(primaryBarcode.format)

            // 2. OCR Store / Brand Name from screenshot text
            var detectedName = "Store Card"
            try {
                val visionText = textRecognizer.process(image).await()
                val textBlocks = visionText.textBlocks

                // Pick the most likely candidate block for store name (usually top of screenshot)
                var nameCandidate: String? = null
                for (block in textBlocks) {
                    for (line in block.text.lines()) {
                        val trimmed = line.trim()
                        if (trimmed.length in 2..30 &&
                            !trimmed.contains(rawCardNumber) &&
                            !IGNORED_WORDS.contains(trimmed.lowercase()) &&
                            !trimmed.all { it.isDigit() || it.isWhitespace() || it == '-' }
                        ) {
                            nameCandidate = trimmed
                            break
                        }
                    }
                    if (nameCandidate != null) break
                }

                if (!nameCandidate.isNullOrBlank()) {
                    detectedName = NameNormalizer.normalize(nameCandidate)
                }
            } catch (_: Exception) {
                // Non-critical, fallback name used
            }

            val preset = BrandCatalog.findMatch(detectedName)
            val colorHex = preset?.colorHex ?: run {
                val colorIndex = (detectedName.hashCode().let { if (it < 0) -it else it }) % BRAND_COLORS.size
                BRAND_COLORS[colorIndex]
            }

            ImportCandidate(
                uri = uri,
                name = preset?.name ?: detectedName,
                cardNumber = rawCardNumber,
                format = detectedFormat,
                colorHex = colorHex,
                isSuccess = true
            )
        } catch (e: Exception) {
            ImportCandidate(
                uri = uri,
                name = "Failed to import",
                cardNumber = "",
                format = CardBarcodeFormat.CODE_128,
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Processing error"
            )
        }
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }

    fun toLoyaltyCard(candidate: ImportCandidate): LoyaltyCard {
        val cleanName = NameNormalizer.normalize(candidate.name.ifBlank { "Loyalty Card" })
        return LoyaltyCard(
            name = cleanName,
            barcodeValue = candidate.cardNumber,
            cardNumber = candidate.cardNumber,
            barcodeFormat = candidate.format,
            colorHex = candidate.colorHex,
            logoUrl = BrandCatalog.inferLogoUrlForStoreName(cleanName)
        )
    }
}
