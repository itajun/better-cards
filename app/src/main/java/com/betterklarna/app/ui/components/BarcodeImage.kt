package com.betterklarna.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.betterklarna.app.data.model.CardBarcodeFormat
import com.betterklarna.app.util.BarcodeGenerator

@Composable
fun BarcodeImage(
    content: String,
    format: CardBarcodeFormat,
    modifier: Modifier = Modifier,
    width: Int = 800,
    height: Int = 320
) {
    var bitmap by remember(content, format) { mutableStateOf<Bitmap?>(null) }
    var error by remember(content, format) { mutableStateOf<String?>(null) }
    var isLoading by remember(content, format) { mutableStateOf(true) }

    LaunchedEffect(content, format) {
        isLoading = true
        error = null
        val result = BarcodeGenerator.generateBarcodeBitmap(
            content = content,
            format = format,
            width = width,
            height = height
        )
        result.onSuccess {
            bitmap = it
            isLoading = false
        }.onFailure {
            error = it.localizedMessage ?: "Failed to generate barcode"
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            error != null -> {
                Text(
                    text = "Barcode Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Barcode for $content",
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (format.is2D) Modifier.aspectRatio(1f)
                            else Modifier.heightIn(min = 120.dp, max = 220.dp)
                        ),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
