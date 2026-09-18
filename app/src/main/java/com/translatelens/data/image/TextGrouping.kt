package com.translatelens.data.image

import android.graphics.PointF
import android.graphics.RectF
import com.translatelens.data.model.TextBlock
import kotlin.math.max
import kotlin.math.min

data class Paragraph(
    val text: String,
    val lines: List<TextBlock>,
    val boundingBox: RectF,
    val cornerPoints: List<PointF>
)

object TextGrouping {
    fun groupIntoParagraphs(blocks: List<TextBlock>): List<Paragraph> {
        if (blocks.isEmpty()) return emptyList()
        val byBlock = blocks.groupBy { it.blockIndex }.toSortedMap()
        val paragraphs = mutableListOf<Paragraph>()
        for ((_, lines) in byBlock) {
            val ordered = lines.sortedWith(compareBy({ it.centerY }, { it.centerX }))
            val runs = splitIntoRuns(ordered)
            for (run in runs) {
                paragraphs.add(buildParagraph(run))
            }
        }
        return paragraphs.sortedWith(compareBy({ it.boundingBox.centerY() }, { it.boundingBox.centerX() }))
    }

    private fun splitIntoRuns(lines: List<TextBlock>): List<List<TextBlock>> {
        if (lines.size <= 1) return listOf(lines)
        val heights = lines.map { it.height.coerceAtLeast(1f) }.sorted()
        val medianH = heights[heights.size / 2]
        val runs = mutableListOf<MutableList<TextBlock>>()
        var current = mutableListOf(lines[0])
        for (i in 1 until lines.size) {
            val prev = lines[i - 1]
            val cur = lines[i]
            val gap = cur.boundingBox.top - prev.boundingBox.bottom
            val sameColumn = horizontalOverlap(prev.boundingBox, cur.boundingBox) > 0.25f
            if (gap <= medianH * 1.1f && (sameColumn || gap < 0)) {
                current.add(cur)
            } else {
                runs.add(current)
                current = mutableListOf(cur)
            }
        }
        runs.add(current)
        return runs
    }

    private fun horizontalOverlap(a: RectF, b: RectF): Float {
        val left = max(a.left, b.left)
        val right = min(a.right, b.right)
        if (right <= left) return 0f
        val minW = min(a.width(), b.width()).coerceAtLeast(1f)
        return (right - left) / minW
    }

    private fun buildParagraph(lines: List<TextBlock>): Paragraph {
        val text = joinLines(lines.map { it.text })
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE
        for (l in lines) {
            left = min(left, l.boundingBox.left)
            top = min(top, l.boundingBox.top)
            right = max(right, l.boundingBox.right)
            bottom = max(bottom, l.boundingBox.bottom)
        }
        val bounds = RectF(left, top, right, bottom)
        val corners = if (lines.size == 1 && lines[0].cornerPoints.size == 4) {
            lines[0].cornerPoints
        } else {
            listOf(
                PointF(left, top),
                PointF(right, top),
                PointF(right, bottom),
                PointF(left, bottom)
            )
        }
        return Paragraph(text, lines, bounds, corners)
    }

    private fun joinLines(lines: List<String>): String {
        if (lines.isEmpty()) return ""
        val out = StringBuilder(lines[0].trim())
        for (i in 1 until lines.size) {
            val prev = lines[i - 1].trimEnd()
            val cur = lines[i].trim()
            if (prev.endsWith("-") && cur.isNotEmpty() && cur[0].isLowerCase() && cur[0] in 'a'..'z') {
                out.setLength(out.length - 1)
                out.append(cur)
            } else {
                out.append(' ').append(cur)
            }
        }
        return out.toString().replace(Regex("\\s+"), " ").trim()
    }
}
