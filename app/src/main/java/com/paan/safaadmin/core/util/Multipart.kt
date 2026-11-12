package com.paan.safaadmin.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/** Builds a multipart form: text fields and an optional photo. */
class Form {
    private val parts = mutableListOf<MultipartBody.Part>()
    fun text(name: String, value: String) = apply { parts += MultipartBody.Part.createFormData(name, value) }
    fun flag(name: String, value: Boolean) = text(name, if (value) "1" else "0")
    fun image(bytes: ByteArray?) = apply {
        if (bytes != null) {
            parts += MultipartBody.Part.createFormData("image", "photo.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
        }
    }
    fun list(): List<MultipartBody.Part> = parts.toList()
}

/** Shrinks a picked photo (max 1600 px, JPEG) so it stays under the server's 2 MB limit. */
suspend fun compressImage(context: Context, uri: Uri, maxSide: Int = 1600): ByteArray? = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide * 2) sample *= 2
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return@runCatching null
        val scale = min(1f, maxSide.toFloat() / max(bitmap.width, bitmap.height))
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else bitmap
        var quality = 85
        var out: ByteArray
        do {
            val stream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            out = stream.toByteArray()
            quality -= 10
        } while (out.size > 1_800_000 && quality > 30)
        out
    }.getOrNull()
}