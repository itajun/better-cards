package com.betterklarna.app.data.model

import com.google.zxing.BarcodeFormat

enum class CardBarcodeFormat(val displayName: String, val zxingFormat: BarcodeFormat) {
    CODE_128("Code 128", BarcodeFormat.CODE_128),
    EAN_13("EAN 13", BarcodeFormat.EAN_13),
    EAN_8("EAN 8", BarcodeFormat.EAN_8),
    UPC_A("UPC A", BarcodeFormat.UPC_A),
    QR_CODE("QR Code", BarcodeFormat.QR_CODE),
    AZTEC("Aztec", BarcodeFormat.AZTEC),
    PDF_417("PDF 417", BarcodeFormat.PDF_417),
    CODE_39("Code 39", BarcodeFormat.CODE_39),
    DATA_MATRIX("Data Matrix", BarcodeFormat.DATA_MATRIX);

    val is2D: Boolean
        get() = this == QR_CODE || this == AZTEC || this == DATA_MATRIX

    companion object {
        fun fromString(formatStr: String): CardBarcodeFormat {
            val normalized = formatStr.uppercase().trim()
            return when (normalized) {
                "CODE_128", "GS1_128" -> CODE_128
                "EAN_13", "EAN13" -> EAN_13
                "EAN_8", "EAN8" -> EAN_8
                "UPC_A", "UPC" -> UPC_A
                "QR_CODE", "QR", "QRCODE" -> QR_CODE
                "AZTEC" -> AZTEC
                "PDF_417", "PDF417" -> PDF_417
                "CODE_39", "CODE39" -> CODE_39
                "DATA_MATRIX", "DATAMATRIX" -> DATA_MATRIX
                else -> CODE_128
            }
        }

        fun fromZxing(format: BarcodeFormat): CardBarcodeFormat {
            return entries.find { it.zxingFormat == format } ?: CODE_128
        }

        fun fromMlKit(mlKitFormat: Int): CardBarcodeFormat {
            return when (mlKitFormat) {
                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_128 -> CODE_128
                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_13 -> EAN_13
                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_8 -> EAN_8
                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_A -> UPC_A
                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE -> QR_CODE
                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_AZTEC -> AZTEC
                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_PDF417 -> PDF_417
                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_39 -> CODE_39
                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_DATA_MATRIX -> DATA_MATRIX
                else -> CODE_128
            }
        }
    }
}
