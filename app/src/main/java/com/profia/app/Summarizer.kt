package com.profia.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object Summarizer {
    private const val CHUNK = 5            // pages envoyées par appel
    private const val WORDS_PER_PAGE = 350
    private const val RATIO = 0.45f        // 10 pages -> ~4-5 pages

    suspend fun run(
        ctx: Context,
        uris: List<Uri>,
        apiKey: String,
        model: String,
        subject: String,
        onStatus: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val n = uris.size
        val targetPages = maxOf(1, Math.round(n * RATIO))
        val finalWords = targetPages * WORDS_PER_PAGE
        val chunks = uris.chunked(CHUNK)

        if (chunks.size == 1) {
            onStatus("Lecture de $n page(s)…")
            val parts = chunks[0].map { GeminiClient.imagePart(toBase64Jpeg(ctx, it)) }
            return@withContext GeminiClient.generate(
                apiKey, model, Prompts.SYSTEM,
                parts + GeminiClient.textPart(Prompts.finalPrompt(n, targetPages, finalWords, subject))
            )
        }

        val partials = mutableListOf<String>()
        var start = 1
        chunks.forEachIndexed { i, chunk ->
            val end = start + chunk.size - 1
            onStatus("Résumé des pages $start à $end (${i + 1}/${chunks.size})…")
            val words = maxOf(120, (chunk.size * WORDS_PER_PAGE * 0.55f).toInt())
            val parts = chunk.map { GeminiClient.imagePart(toBase64Jpeg(ctx, it)) }
            partials += GeminiClient.generate(
                apiKey, model, Prompts.SYSTEM,
                parts + GeminiClient.textPart(Prompts.chunkPrompt(start, end, n, words, subject))
            )
            start = end + 1
        }

        onStatus("Fusion en un résumé final…")
        GeminiClient.generate(
            apiKey, model, Prompts.SYSTEM,
            listOf(GeminiClient.textPart(Prompts.mergePrompt(partials, targetPages, finalWords, subject)))
        )
    }

    private fun toBase64Jpeg(ctx: Context, uri: Uri): String {
        val resolver = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }

        var sample = 1
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        while (maxSide / sample > 2000) sample *= 2

        val decoded = resolver.openInputStream(uri)!!.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw RuntimeException("Image illisible")

        val orientation = try {
            resolver.openInputStream(uri)!!.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val bmp: Bitmap = if (degrees != 0f) {
            Bitmap.createBitmap(
                decoded, 0, 0, decoded.width, decoded.height,
                Matrix().apply { postRotate(degrees) }, true
            )
        } else decoded

        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
