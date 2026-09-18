package com.translatelens.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream

class ImageFiles(
    private val context: Context,
    private val maxDimension: Int = MAX_DIMENSION,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun decode(path: String): Bitmap = withContext(ioDispatcher) {
        when {
            path.startsWith(CONTENT_SCHEME) -> decodeUri(Uri.parse(path))
            path.startsWith(FILE_SCHEME) -> {
                val p = Uri.parse(path).path ?: throw IOException("مسار الصورة غير صالح")
                decodeFile(p)
            }
            else -> decodeFile(path)
        }
    }

    suspend fun decode(uri: Uri): Bitmap = withContext(ioDispatcher) {
        decodeUri(uri)
    }

    suspend fun write(bitmap: Bitmap, prefix: String): String = withContext(ioDispatcher) {
        val dir = File(context.filesDir, OUTPUT_DIR).apply { mkdirs() }
        val safe = prefix.filter { it.isLetterOrDigit() }.take(20).ifEmpty { "img" }
        val file = File.createTempFile(safe + "_", IMAGE_SUFFIX, dir)
        file.outputStream().use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                throw IOException("فشل كتابة الصورة")
            }
            out.flush()
        }
        file.absolutePath
    }

    private fun decodeFile(path: String): Bitmap {
        val file = File(path)
        if (!file.exists()) throw IOException("ملف الصورة غير موجود")
        val rotation = readExifRotation(file.absolutePath)
        return decodeStream({ file.inputStream() }, rotation)
    }

    private fun decodeUri(uri: Uri): Bitmap {
        val rotation = readExifRotation(uri)
        return decodeStream(
            { context.contentResolver.openInputStream(uri) ?: throw IOException("تعذر فتح الصورة") },
            rotation
        )
    }

    private fun decodeStream(openStream: () -> InputStream, rotationDegrees: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream().use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("صورة غير صالحة أو غير مدعومة")
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val sampled = openStream().use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("فشل فك ترميز الصورة")
        val rotated = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            try {
                val result = Bitmap.createBitmap(sampled, 0, 0, sampled.width, sampled.height, matrix, true)
                if (result != sampled) sampled.recycle()
                result
            } catch (e: Exception) {
                sampled
            }
        } else {
            sampled
        }
        return downscaleToBounds(rotated)
    }

    private fun downscaleToBounds(bitmap: Bitmap): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / largest
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val result = Bitmap.createScaledBitmap(bitmap, width, height, true)
        if (result != bitmap) bitmap.recycle()
        return result
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        val largest = maxOf(width, height)
        while (largest / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun exifToDegrees(orientation: Int): Int = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }

    private fun readExifRotation(path: String): Int = try {
        exifToDegrees(ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL))
    } catch (_: Exception) {
        0
    }

    private fun readExifRotation(uri: Uri): Int = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            exifToDegrees(ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL))
        } ?: 0
    } catch (_: Exception) {
        0
    }

    companion object {
        const val MAX_DIMENSION = 2048
        private const val OUTPUT_DIR = "images"
        private const val CONTENT_SCHEME = "content://"
        private const val FILE_SCHEME = "file://"
        private const val IMAGE_SUFFIX = ".png"
    }
}
