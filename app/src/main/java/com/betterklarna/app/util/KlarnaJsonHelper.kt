package com.betterklarna.app.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.betterklarna.app.data.model.CardBarcodeFormat
import com.betterklarna.app.data.model.LoyaltyCard
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets

object KlarnaJsonHelper {

    fun parseKlarnaJson(jsonString: String): List<LoyaltyCard> {
        val list = mutableListOf<LoyaltyCard>()
        val array = JSONArray(jsonString)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val rawName = obj.optString("name", "Card")
            val name = NameNormalizer.normalize(rawName)
            val barcodeValue = obj.optString("barcode_value", "")
            val barcodeFormatStr = obj.optString("barcode_format", "CODE_128")
            val cardNumber = obj.optString("card_number", barcodeValue)
            val cardNumberFormatted = if (obj.has("card_number_formatted") && !obj.isNull("card_number_formatted")) obj.optString("card_number_formatted") else null
            val memberId = if (obj.has("member_id") && !obj.isNull("member_id")) obj.optString("member_id") else null
            val memberIdFormatted = if (obj.has("member_id_formatted") && !obj.isNull("member_id_formatted")) obj.optString("member_id_formatted") else null
            val color = obj.optString("color", "#1E88E5")
            val logoUrl = if (obj.has("logo_url") && !obj.isNull("logo_url")) obj.optString("logo_url") else null
            val isCustomCard = obj.optBoolean("is_custom_card", false)
            val addedVia = obj.optString("added_via", "JSON_IMPORT")
            val klarnaCardId = if (obj.has("klarna_card_id") && !obj.isNull("klarna_card_id")) obj.optString("klarna_card_id") else null
            val klarnaProviderId = if (obj.has("klarna_provider_id") && !obj.isNull("klarna_provider_id")) obj.optString("klarna_provider_id") else null
            val market = obj.optString("market", "AU")

            val format = CardBarcodeFormat.fromString(barcodeFormatStr)

            list.add(
                LoyaltyCard(
                    name = name,
                    barcodeValue = barcodeValue.ifBlank { cardNumber },
                    barcodeFormat = format,
                    cardNumber = cardNumber.ifBlank { barcodeValue },
                    cardNumberFormatted = cardNumberFormatted,
                    memberId = memberId,
                    memberIdFormatted = memberIdFormatted,
                    colorHex = color,
                    logoUrl = logoUrl,
                    isCustomCard = isCustomCard,
                    addedVia = addedVia,
                    klarnaCardId = klarnaCardId,
                    klarnaProviderId = klarnaProviderId,
                    market = market
                )
            )
        }
        return list
    }

    fun parseFromInputStream(inputStream: InputStream): List<LoyaltyCard> {
        val text = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        return parseKlarnaJson(text)
    }

    fun exportToJson(cards: List<LoyaltyCard>, context: Context? = null): String {
        val array = JSONArray()
        for (card in cards) {
            val exportedLogo = LogoLoader.getPortableExportLogoUrl(context, card.logoUrl, card.name)
            val obj = JSONObject().apply {
                put("name", card.name)
                put("barcode_value", card.effectiveBarcodePayload)
                put("barcode_format", card.barcodeFormat.name)
                put("card_number", card.cardNumber.ifBlank { card.barcodeValue })
                put("card_number_formatted", card.cardNumberFormatted ?: JSONObject.NULL)
                put("member_id", card.memberId ?: JSONObject.NULL)
                put("member_id_formatted", card.memberIdFormatted ?: JSONObject.NULL)
                put("color", card.colorHex)
                put("logo_url", exportedLogo ?: JSONObject.NULL)
                put("is_custom_card", card.isCustomCard)
                put("added_via", card.addedVia ?: "BETTER_CARDS")
                put("klarna_card_id", card.klarnaCardId ?: JSONObject.NULL)
                put("klarna_provider_id", card.klarnaProviderId ?: JSONObject.NULL)
                put("market", card.market ?: "AU")
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    fun createExportFile(context: Context, cards: List<LoyaltyCard>): Uri {
        val jsonContent = exportToJson(cards, context)
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "loyalty-cards-${System.currentTimeMillis()}.json"
        val file = File(exportDir, fileName)
        file.writeText(jsonContent, StandardCharsets.UTF_8)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
