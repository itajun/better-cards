package com.betterklarna.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object LogoLoader {
    private val memoryCache = object : LruCache<String, Bitmap>(60) {}

    // Fallback map from normalized store name -> bundled asset filename in assets/logos/
    private val BRAND_NAME_TO_ASSET = mapOf(
        "amcal" to "1ced99b56507b330e4577f2471fff22d.png",
        "bonds" to "fd398ff46b3146970b3b900b84b12ca1.png",
        "cincotta discount chemist" to "ffe094bcf5fbedc5c782ba5f44ea2067.png",
        "cotton on" to "7b4d912374ef0b9c7815f85689a1dfdc.png",
        "country road & trenery" to "cf90a74ec373ebcb149c4254b3cbb847.png",
        "country road" to "cf90a74ec373ebcb149c4254b3cbb847.png",
        "dan murphy's" to "bcfbab9d76825f9cd26b731be6c85241.png",
        "david jones" to "9148de829f257719aef5c24b45bd85fc.png",
        "decathlon" to "c2a49b9b6900d4104422f686e6bfee46.png",
        "everyday rewards" to "7e7a5e4f555d56565cc9a8173f3026d8.png",
        "woolworths" to "7e7a5e4f555d56565cc9a8173f3026d8.png",
        "flybuys" to "8fef1ff7019e3e527dbbd7ca8ac98bb9.png",
        "grill'd" to "4ce98397593f24e704f1f1545b0c2273.png",
        "harris farm markets" to "c3f585be5d354c2d73ec8bb756e06c9e.png",
        "iga" to "0ca9b4629d6ef6163e3f163209b6166a.png",
        "ikea family" to "b6d02914395f9b0fbf2fc601acf93b14.png",
        "ikea" to "b6d02914395f9b0fbf2fc601acf93b14.png",
        "just jeans" to "0105e80ca7551dcc65af1a17e5ad1570.png",
        "myer one" to "aeb5455a6a2ede52f32a3217e93c13cd.png",
        "myer" to "aeb5455a6a2ede52f32a3217e93c13cd.png",
        "nespresso" to "fe08920a981f8f08c98f8e79f5801e7e.png",
        "priceline pharmacy" to "676a10585dd7f9fa3dbbad34e6fb0f89.png",
        "priceline" to "676a10585dd7f9fa3dbbad34e6fb0f89.png",
        "qantas" to "23b53f32780eee9b67cb7914e5a6c58e.png",
        "rebel" to "23874f062249320caad3b944723b94e0.png",
        "san churro" to "896fde7ffb048bc847f18392ecb6ce24.png",
        "terrywhite chemmart" to "e81d71df7954ab9e0043fed25a7871cb.png",
        "the source bulk foods" to "cd6f7ec50169ee1516e31ffa12c75fbb.png"
    )

    fun getCachedOrAssetSync(context: Context, logoUrl: String?, cardName: String): Bitmap? {
        val effectiveUrl = logoUrl?.takeIf { it.isNotBlank() }
            ?: BrandCatalog.findMatch(cardName)?.defaultLogoUrl
        val assetFileName = resolveAssetFileName(effectiveUrl, cardName)
        val cacheKey = assetFileName ?: effectiveUrl?.take(80) ?: cardName.lowercase()

        memoryCache.get(cacheKey)?.let { return it }

        // 1. Try bundled APK assets first (instant 0ms offline)
        if (assetFileName != null) {
            runCatching {
                context.assets.open("logos/$assetFileName").use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        memoryCache.put(cacheKey, bmp)
                        return bmp
                    }
                }
            }
        }

        // 2. Try inline Base64 data URI ("data:image/png;base64,...")
        if (effectiveUrl != null && effectiveUrl.startsWith("data:image")) {
            val base64Part = effectiveUrl.substringAfter("base64,", "")
            if (base64Part.isNotBlank()) {
                runCatching {
                    val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        memoryCache.put(cacheKey, bmp)
                        return bmp
                    }
                }
            }
        }

        // 3. Try custom local file ("local://<filename>")
        if (effectiveUrl != null && effectiveUrl.startsWith("local://")) {
            val fileName = effectiveUrl.removePrefix("local://")
            val localFile = File(File(context.filesDir, "logos"), fileName)
            if (localFile.exists()) {
                val bmp = BitmapFactory.decodeFile(localFile.absolutePath)
                if (bmp != null) {
                    memoryCache.put(cacheKey, bmp)
                    return bmp
                }
            }
        }

        // 4. Try local disk cache for remote URLs
        if (!effectiveUrl.isNullOrBlank() && effectiveUrl.startsWith("http")) {
            val hash = md5(effectiveUrl)
            val cachedFile = File(File(context.filesDir, "logos"), "$hash.png")
            if (cachedFile.exists()) {
                val bmp = BitmapFactory.decodeFile(cachedFile.absolutePath)
                if (bmp != null) {
                    memoryCache.put(cacheKey, bmp)
                    return bmp
                }
            }
        }

        return null
    }

    suspend fun loadLogo(context: Context, logoUrl: String?, cardName: String): Bitmap? {
        getCachedOrAssetSync(context, logoUrl, cardName)?.let { return it }

        val effectiveUrl = logoUrl?.takeIf { it.isNotBlank() }
            ?: BrandCatalog.inferLogoUrlForStoreName(cardName)
            ?: return null

        if (!effectiveUrl.startsWith("http")) return null

        return withContext(Dispatchers.IO) {
            runCatching {
                val hash = md5(effectiveUrl)
                val logosDir = File(context.filesDir, "logos").apply { mkdirs() }
                val cachedFile = File(logosDir, "$hash.png")

                val connection = (URL(effectiveUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    instanceFollowRedirects = true
                    doInput = true
                }
                connection.connect()
                if (connection.responseCode in 200..299) {
                    val bytes = connection.inputStream.use { it.readBytes() }
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null && bmp.width >= 32) {
                        cachedFile.writeBytes(bytes)
                        memoryCache.put(effectiveUrl.take(80), bmp)
                        return@withContext bmp
                    }
                }
                null
            }.getOrNull()
        }
    }

    /**
     * Produces a portable logo representation for JSON export:
     * - If it's a `local://` custom photo, encodes the image as `data:image/png;base64,...`
     *   so anyone receiving the JSON file gets the custom photo embedded directly!
     * - If it's an `asset://` preset or empty, resolves to a portable URL or Base64 data URI.
     * - If it's already an `https://` or `data:image/...` URL, preserves it.
     */
    fun getPortableExportLogoUrl(context: Context?, logoUrl: String?, cardName: String): String? {
        val effective = logoUrl?.takeIf { it.isNotBlank() }
            ?: BrandCatalog.inferLogoUrlForStoreName(cardName)
            ?: return null

        if (effective.startsWith("http") || effective.startsWith("data:image")) {
            return effective
        }

        if (context != null) {
            // Convert local:// or asset:// into portable data:image/png;base64,...
            val bitmap = getCachedOrAssetSync(context, effective, cardName)
            if (bitmap != null) {
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, baos)
                val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                return "data:image/png;base64,$b64"
            }
        }

        return effective
    }

    suspend fun saveCustomLogoFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }

            // Scale down to max 300px so it loads fast and embeds compactly in JSON exports
            val maxDim = 300
            val scale = minOf(maxDim.toFloat() / rawBitmap.width, maxDim.toFloat() / rawBitmap.height, 1f)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    rawBitmap,
                    (rawBitmap.width * scale).toInt().coerceAtLeast(1),
                    (rawBitmap.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                rawBitmap
            }

            val logosDir = File(context.filesDir, "logos").apply { mkdirs() }
            val fileName = "custom_${System.currentTimeMillis()}.png"
            val outFile = File(logosDir, fileName)
            FileOutputStream(outFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            val localUrl = "local://$fileName"
            memoryCache.put(localUrl, scaled)
            localUrl
        }.getOrNull()
    }

    private fun resolveAssetFileName(logoUrl: String?, cardName: String): String? {
        if (logoUrl != null && logoUrl.startsWith("asset://")) {
            return logoUrl.removePrefix("asset://")
        }
        BRAND_NAME_TO_ASSET[cardName.trim().lowercase()]?.let { return it }
        if (!logoUrl.isNullOrBlank() && logoUrl.contains("klarnacdn.net")) {
            return "${md5(logoUrl)}.png"
        }
        return null
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
