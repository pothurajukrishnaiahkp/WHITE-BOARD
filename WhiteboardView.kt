package com.krish.board

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

class WhiteboardView(context: Context) : View(context) {
    private class Stroke(val path: Path, val paint: Paint)

    private val strokes = ArrayList<Stroke>()
    private val redoStack = ArrayList<Stroke>()
    private var current: Stroke? = null
    private var lastX = 0f
    private var lastY = 0f

    var color = Color.BLACK
    var brushSize = 8f
    var eraser = false

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        for (s in strokes) canvas.drawPath(s.path, s.paint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = e.x
        val y = e.y
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val p = Path().apply { moveTo(x, y); lineTo(x + 0.01f, y) }
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    strokeWidth = if (eraser) brushSize * 3 else brushSize
                    this.color = if (eraser) Color.WHITE else this@WhiteboardView.color
                }
                current = Stroke(p, paint).also { strokes.add(it) }
                redoStack.clear()
                lastX = x; lastY = y
            }
            MotionEvent.ACTION_MOVE -> current?.let {
                it.path.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2)
                lastX = x; lastY = y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                current?.path?.lineTo(x, y)
                current = null
            }
        }
        invalidate()
        return true
    }

    fun undo() { if (strokes.isNotEmpty()) { redoStack.add(strokes.removeAt(strokes.lastIndex)); invalidate() } }
    fun redo() { if (redoStack.isNotEmpty()) { strokes.add(redoStack.removeAt(redoStack.lastIndex)); invalidate() } }
    fun clear() { strokes.clear(); redoStack.clear(); invalidate() }

    fun toBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        draw(Canvas(bmp))
        return bmp
    }
}
