package com.krish.board

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.google.mlkit.vision.digitalink.Ink
import kotlin.math.hypot

class BoardView @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : View(c, a) {

    private class S(val path: Path, val color: Int, val pts: MutableList<Triple<Float, Float, Long>>)

    private val strokes = mutableListOf<S>()
    private val redoList = mutableListOf<S>()
    private var cur: S? = null

    var eraser = false
    var penColor = Color.BLACK
    var onTouchStart: (() -> Unit)? = null
    var onTouchEnd: (() -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        for (s in strokes) {
            paint.color = s.color
            canvas.drawPath(s.path, paint)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = e.x
        val y = e.y
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                onTouchStart?.invoke()
                if (eraser) {
                    erase(x, y)
                } else {
                    val p = Path()
                    p.moveTo(x, y)
                    p.lineTo(x + 0.01f, y)
                    val s = S(p, penColor, mutableListOf(Triple(x, y, e.eventTime)))
                    strokes.add(s)
                    cur = s
                    redoList.clear()
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (eraser) {
                    erase(x, y)
                } else {
                    cur?.let {
                        it.path.lineTo(x, y)
                        it.pts.add(Triple(x, y, e.eventTime))
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                cur = null
                onTouchEnd?.invoke()
            }
        }
        return true
    }

    private fun erase(x: Float, y: Float) {
        strokes.removeAll { s -> s.pts.any { hypot(it.first - x, it.second - y) < 40f } }
    }

    fun undo(): Boolean {
        if (strokes.isEmpty()) return false
        redoList.add(strokes.removeAt(strokes.size - 1))
        invalidate()
        return true
    }

    fun redo() {
        if (redoList.isEmpty()) return
        strokes.add(redoList.removeAt(redoList.size - 1))
        invalidate()
    }

    fun clearAll() {
        strokes.clear()
        redoList.clear()
        invalidate()
    }

    fun buildInk(): Ink? {
        if (strokes.isEmpty()) return null
        val b = Ink.builder()
        for (s in strokes) {
            val sb = Ink.Stroke.builder()
            for (p in s.pts) sb.addPoint(Ink.Point.create(p.first, p.second, p.third))
            b.addStroke(sb.build())
        }
        return b.build()
    }
}
