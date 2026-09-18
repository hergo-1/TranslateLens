package com.translatelens.data.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.translatelens.data.model.OcrResult
import com.translatelens.data.model.TextBlock
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class Renderer {
    fun render(
        background: Bitmap,
        ocr: OcrResult,
        translations: List<String>,
        targetLanguage: String
    ): Bitmap {
        require(!background.isRecycled) { "الصورة الأساسية غير صالحة" }
        require(ocr.textBlocks.isNotEmpty()) { "لم يتم العثور على نص في الصورة" }
        require(translations.size == ocr.textBlocks.size) { "عدد الترجمات لا يطابق عدد الكتل" }
        val output = background.copy(Bitmap.Config.ARGB_8888, true)
            ?: throw IllegalStateException("فشل نسخ الصورة")
        try {
            val canvas = Canvas(output)
            val rtl = isRtl(targetLanguage)
            ocr.textBlocks.forEachIndexed { index, block ->
                drawBlock(canvas, output, block, translations[index], rtl)
            }
            return output
        } catch (t: Throwable) {
            if (t is InterruptedException || t is kotlinx.coroutines.CancellationException) {
                output.recycle()
                throw t
            }
            if (t is IllegalArgumentException && t.message?.contains("لم يتم") == true) {
                output.recycle()
                throw t
            }
            try {
                output.recycle()
            } catch (_: Exception) {
            }
            throw t
        }
    }

    private fun drawBlock(
        canvas: Canvas,
        output: Bitmap,
        block: TextBlock,
        translated: String,
        rtl: Boolean
    ) {
        val text = translated.trim()
        if (text.isEmpty()) return
        val corners = normalizedCorners(block)
        if (corners.size < 4) return
        val fill = sampleRingColor(output, regionBounds(corners))
        fillPath(output, corners, fill)
        drawFittedText(canvas, corners, text, rtl, fill)
    }

    private fun normalizedCorners(block: TextBlock): List<PointF> {
        val pts = block.cornerPoints.take(4)
        if (pts.size == 4) return pts
        val b = block.boundingBox
        if (b.isEmpty) return emptyList()
        return listOf(
            PointF(b.left, b.top),
            PointF(b.right, b.top),
            PointF(b.right, b.bottom),
            PointF(b.left, b.bottom)
        )
    }

    private fun regionBounds(corners: List<PointF>): RectF {
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE
        for (p in corners) {
            left = min(left, p.x)
            top = min(top, p.y)
            right = max(right, p.x)
            bottom = max(bottom, p.y)
        }
        return RectF(left, top, right, bottom)
    }

    private fun sampleRingColor(bitmap: Bitmap, bounds: RectF): Int {
        val pad = 8f
        val left = (bounds.left - pad).toInt().coerceIn(0, bitmap.width - 1)
        val top = (bounds.top - pad).toInt().coerceIn(0, bitmap.height - 1)
        val right = (bounds.right + pad).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = (bounds.bottom + pad).toInt().coerceIn(top + 1, bitmap.height)
        var r = 0L
        var g = 0L
        var b = 0L
        var n = 0L
        val step = 4
        var x = left
        while (x < right) {
            if (x < bounds.left || x > bounds.right) {
                n += 2
                val c1 = bitmap.getPixel(x, top)
                val c2 = bitmap.getPixel(x, bottom - 1)
                r += Color.red(c1) + Color.red(c2)
                g += Color.green(c1) + Color.green(c2)
                b += Color.blue(c1) + Color.blue(c2)
            }
            x += step
        }
        var y = top
        while (y < bottom) {
            if (y < bounds.top || y > bounds.bottom) {
                n += 2
                val c1 = bitmap.getPixel(left, y)
                val c2 = bitmap.getPixel(right - 1, y)
                r += Color.red(c1) + Color.red(c2)
                g += Color.green(c1) + Color.green(c2)
                b += Color.blue(c1) + Color.blue(c2)
            }
            y += step
        }
        if (n == 0L) {
            val cx = bounds.centerX().toInt().coerceIn(0, bitmap.width - 1)
            val cy = bounds.centerY().toInt().coerceIn(0, bitmap.height - 1)
            return bitmap.getPixel(cx, cy)
        }
        return Color.rgb((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
    }

    private fun fillPath(bitmap: Bitmap, corners: List<PointF>, color: Int) {
        val path = Path().apply {
            moveTo(corners[0].x, corners[0].y)
            lineTo(corners[1].x, corners[1].y)
            lineTo(corners[2].x, corners[2].y)
            lineTo(corners[3].x, corners[3].y)
            close()
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        Canvas(bitmap).drawPath(path, paint)
    }

    private fun drawFittedText(
        canvas: Canvas,
        corners: List<PointF>,
        text: String,
        rtl: Boolean,
        bg: Int
    ) {
        val topW = hypot(corners[1].x - corners[0].x, corners[1].y - corners[0].y)
        val bottomW = hypot(corners[2].x - corners[3].x, corners[2].y - corners[3].y)
        val leftH = hypot(corners[3].x - corners[0].x, corners[3].y - corners[0].y)
        val rightH = hypot(corners[2].x - corners[1].x, corners[2].y - corners[1].y)
        val regionW = max(8f, (topW + bottomW) / 2f - 8f)
        val regionH = max(8f, (leftH + rightH) / 2f - 6f)
        if (regionW < 8f || regionH < 8f) return

        val fg = contrastingColor(bg)
        var bestSize = 10f
        var lo = 8f
        var hi = min(regionH * 0.9f, 220f).coerceAtLeast(9f)
        repeat(12) {
            val mid = (lo + hi) / 2f
            if (fits(text, mid, regionW, regionH, rtl)) {
                bestSize = mid
                lo = mid
            } else {
                hi = mid
            }
            if (hi - lo < 0.75f) return@repeat
        }
        val paint = buildPaint(bestSize, fg)
        val layout = buildLayout(text, paint, regionW.toInt().coerceAtLeast(8), rtl)

        val src = floatArrayOf(0f, 0f, regionW, 0f, regionW, regionH, 0f, regionH)
        val dst = floatArrayOf(
            corners[0].x, corners[0].y,
            corners[1].x, corners[1].y,
            corners[2].x, corners[2].y,
            corners[3].x, corners[3].y
        )
        val matrix = Matrix()
        if (!matrix.setPolyToPoly(src, 0, dst, 0, 4)) {
            val b = regionBounds(corners)
            canvas.save()
            canvas.translate(b.left + 4f, b.top + (b.height() - layout.height) / 2f)
            layout.draw(canvas)
            canvas.restore()
            return
        }
        canvas.save()
        canvas.concat(matrix)
        canvas.translate(0f, ((regionH - layout.height) / 2f).coerceAtLeast(0f))
        layout.draw(canvas)
        canvas.restore()
    }

    private fun buildPaint(size: Float, color: Int): TextPaint {
        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            isFakeBoldText = true
        }
    }

    private fun buildLayout(text: String, paint: TextPaint, width: Int, rtl: Boolean): StaticLayout {
        val align = Layout.Alignment.ALIGN_CENTER
        return if (Build.VERSION.SDK_INT >= 23) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(align)
                .setLineSpacing(0f, 1.0f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, paint, width, align, 1.0f, 0f, false)
        }
    }

    private fun fits(text: String, size: Float, w: Float, h: Float, rtl: Boolean): Boolean {
        if (size < 6f) return true
        val paint = buildPaint(size, Color.BLACK)
        val layout = buildLayout(text, paint, w.toInt().coerceAtLeast(8), rtl)
        if (layout.height > h) return false
        for (i in 0 until layout.lineCount) {
            if (layout.getLineWidth(i) > w) return false
        }
        return true
    }

    private fun contrastingColor(bg: Int): Int {
        val lum = (0.299 * Color.red(bg) + 0.587 * Color.green(bg) + 0.114 * Color.blue(bg)) / 255.0
        return if (lum > 0.6) Color.BLACK else Color.WHITE
    }

    private fun isRtl(code: String): Boolean {
        val c = code.lowercase().substringBefore('-').substringBefore('_')
        return c == "ar" || c == "fa" || c == "ur" || c == "he" || c == "iw" || c == "ps"
    }
}
