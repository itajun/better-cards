package com.betterklarna.app

import com.betterklarna.app.data.model.CardBarcodeFormat
import com.betterklarna.app.util.KlarnaJsonHelper
import org.junit.Assert.*
import org.junit.Test

class KlarnaJsonHelperTest {

    private val sampleJson = """
        [
          {
            "name": "amcal",
            "barcode_value": "900000000001",
            "barcode_format": "CODE_128",
            "card_number": "900000000001",
            "color": "#018fcf"
          },
          {
            "name": "flybuys",
            "barcode_value": "2790000000001",
            "barcode_format": "EAN_13",
            "card_number": "2790000000001",
            "member_id": "6008000000000001",
            "color": "#1a7dce"
          },
          {
            "name": "TerryWhite Chemmart",
            "barcode_value": "R20000001",
            "barcode_format": "GS1_128",
            "card_number": "R20000001",
            "color": "#13a352"
          },
          {
            "name": "IKEA Family",
            "barcode_value": "6275000000000000001",
            "barcode_format": "QR_CODE",
            "card_number": "6275000000000000001",
            "color": "#0057a4"
          },
          {
            "name": "Nespresso",
            "barcode_value": "2100001",
            "barcode_format": "PDF_417",
            "card_number": "2100001",
            "color": "#000000"
          }
        ]
    """.trimIndent()

    @Test
    fun testParseAndNormalizeKlarnaJson() {
        val cards = KlarnaJsonHelper.parseKlarnaJson(sampleJson)
        assertEquals(5, cards.size)

        val amcal = cards.find { it.name == "Amcal" }
        assertNotNull(amcal)
        assertEquals("900000000001", amcal!!.barcodeValue)
        assertEquals(CardBarcodeFormat.CODE_128, amcal.barcodeFormat)

        val flybuys = cards.find { it.name == "Flybuys" }
        assertNotNull(flybuys)
        assertEquals("2790000000001", flybuys!!.cardNumber)
        assertEquals("6008000000000001", flybuys.memberId)
        assertEquals(CardBarcodeFormat.EAN_13, flybuys.barcodeFormat)

        val terryWhite = cards.find { it.name == "TerryWhite Chemmart" }
        assertNotNull(terryWhite)
        assertEquals(CardBarcodeFormat.CODE_128, terryWhite!!.barcodeFormat)

        // Test Export round-trip
        val exportedJson = KlarnaJsonHelper.exportToJson(cards)
        val reimportedCards = KlarnaJsonHelper.parseKlarnaJson(exportedJson)
        assertEquals(5, reimportedCards.size)
    }
}
