package com.krish.board

import android.app.Activity
import android.content.ContentValues
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private lateinit var board: WhiteboardView
    private lateinit var eraserBtn: Button

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        board = WhiteboardView(this)

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        val colors = listOf("#000000", "#E53935", "#1E88E5", "#43A047", "#FB8C00", "#8E24AA")
        for (c in colors) {
            val dot = View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(c))
                    setStroke(dp(2), Color.LTGRAY)
                }
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { setMargins(dp(4), 0, dp(4), 0) }
                setOnClickListener {
                    board.color = Color.parseColor(c)
                    board.eraser = false
                    eraserBtn.text = "Eraser"
                }
            }
            bar.addView(dot)
        }
        fun btn(label: String, action: (Button) -> Unit) = Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action(this) }
        }.also { bar.addView(it) }

        eraserBtn = btn("Eraser") {
            board.eraser = !board.eraser
            it.text = if (board.eraser) "Pen" else "Eraser"
        }
        btn("Undo") { board.undo() }
        btn("Redo") { board.redo() }
        btn("Clear") { board.clear() }
        btn("Save") { save() }

        val seek = SeekBar(this).apply {
            max = 40
            progress = 8
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) { board.brushSize = maxOf(2, p).toFloat() }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
            setPadding(dp(16), dp(4), dp(16), dp(8))
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            addView(HorizontalScrollView(this@MainActivity).apply { addView(bar) })
            addView(seek)
            addView(board, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        setContentView(root)
    }

    private fun save() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "KrishBoard_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KrishBoard")
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) { Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show(); return }
        contentResolver.openOutputStream(uri)?.use { board.toBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        Toast.makeText(this, "Saved to Pictures/KrishBoard", Toast.LENGTH_SHORT).show()
    }
}
