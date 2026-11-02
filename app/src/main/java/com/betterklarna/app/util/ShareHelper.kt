package com.betterklarna.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import com.betterklarna.app.data.model.LoyaltyCard
import org.json.JSONObject
import java.nio.charset.StandardCharsets

object ShareHelper {
    // Hosted web viewer URL (can be customized or pointed to GitHub Pages / Cloudflare Pages)
    const val DEFAULT_WEB_HOST = "https://itajun.github.io/better-cards"

    fun generateShareUrl(card: LoyaltyCard, baseHost: String = DEFAULT_WEB_HOST): String {
        val json = JSONObject().apply {
            put("n", card.name)
            put("c", card.cardNumber.ifBlank { card.barcodeValue })
            put("b", card.effectiveBarcodePayload)
            put("f", card.barcodeFormat.name)
            put("col", card.colorHex)
            if (card.notes.isNotBlank()) {
                put("nt", card.notes)
            }
            if (!card.memberId.isNullOrBlank()) {
                put("m", card.memberId)
            }
        }
        val encoded = Base64.encodeToString(
            json.toString().toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        return "$baseHost/#c=$encoded"
    }

    fun parseShareUrl(url: String): LoyaltyCard? {
        return runCatching {
            val uri = Uri.parse(url)
            val fragment = uri.fragment ?: ""
            val payload = if (fragment.startsWith("c=")) {
                fragment.removePrefix("c=")
            } else {
                uri.getQueryParameter("c") ?: return null
            }

            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE)
            val json = JSONObject(String(decodedBytes, StandardCharsets.UTF_8))

            val formatStr = json.optString("f", "CODE_128")
            val format = com.betterklarna.app.data.model.CardBarcodeFormat.fromString(formatStr)
            val rawNum = json.getString("c")
            val barcodeVal = json.optString("b", rawNum)

            LoyaltyCard(
                name = json.getString("n"),
                barcodeValue = barcodeVal,
                cardNumber = rawNum,
                barcodeFormat = format,
                colorHex = json.optString("col", "#1E88E5"),
                notes = json.optString("nt", ""),
                memberId = if (json.has("m")) json.optString("m") else null
            )
        }.getOrNull()
    }

    fun shareViaSystem(context: Context, card: LoyaltyCard) {
        val shareUrl = generateShareUrl(card)
        val text = "Here is my ${card.name} card:\n$shareUrl\n\nOpen to scan the barcode or add it to your wallet."

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "${card.name} Loyalty Card")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share ${card.name} Card")
        context.startActivity(shareIntent)
    }

    fun shareViaWhatsApp(context: Context, card: LoyaltyCard) {
        val shareUrl = generateShareUrl(card)
        val text = "Here is my ${card.name} card:\n$shareUrl\n\nOpen to scan the barcode or add it to your wallet."

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(text))
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            // Fallback to regular system share if WhatsApp is not installed
            shareViaSystem(context, card)
        }
    }

    fun shareViaSms(context: Context, card: LoyaltyCard) {
        val shareUrl = generateShareUrl(card)
        val text = "Here is my ${card.name} card:\n$shareUrl"

        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:")
            putExtra("sms_body", text)
        }
        runCatching {
            context.startActivity(smsIntent)
        }.onFailure {
            shareViaSystem(context, card)
        }
    }
}
