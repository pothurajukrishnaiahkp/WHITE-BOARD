package com.krish.board

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions

class MainActivity : AppCompatActivity() {

    private lateinit var board: BoardView
    private lateinit var output: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val chunks = mutableListOf<String>()
    private var recognizer: DigitalInkRecognizer? = null
    private var ready = false
    private var teluguMode = true
    private var textSizeSp = 24f

    private val penColors = intArrayOf(Color.BLACK, Color.RED, Color.BLUE, Color.rgb(0, 128, 0))
    private val textColors = intArrayOf(Color.BLACK, Color.RED, Color.BLUE, Color.rgb(0, 128, 0), Color.MAGENTA)
    private var penIdx = 0
    private var textIdx = 0

    private val convert = Runnable { recognize() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        board = findViewById(R.id.board)
        output = findViewById(R.id.output)

        board.onTouchStart = { handler.removeCallbacks(convert) }
        board.onTouchEnd = { handler.postDelayed(convert, 1500) }

        click(R.id.undo) {
            if (!board.undo() && chunks.isNotEmpty()) {
                chunks.removeAt(chunks.size - 1)
                render()
            }
        }
        click(R.id.redo) { board.redo() }
        click(R.id.eraser) { b ->
            board.eraser = !board.eraser
            b.text = if (board.eraser) "Eraser: ON" else "Eraser: OFF"
        }
        click(R.id.pen) {
            penIdx = (penIdx + 1) % penColors.size
            board.penColor = penColors[penIdx]
        }
        click(R.id.textcolor) {
            textIdx = (textIdx + 1) % textColors.size
            output.setTextColor(textColors[textIdx])
        }
        click(R.id.bigger) { textSizeSp += 4f; output.textSize = textSizeSp }
        click(R.id.smaller) { if (textSizeSp > 12f) textSizeSp -= 4f; output.textSize = textSizeSp }
        click(R.id.lang) { b ->
            teluguMode = !teluguMode
            b.text = if (teluguMode) "Language: Telugu" else "Language: English"
            loadLanguage()
        }
        click(R.id.copy) {
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("text", output.text))
            toast("Copied")
        }
        click(R.id.clear) {
            chunks.clear()
            render()
            board.clearAll()
        }
        loadLanguage()
    }

    private fun click(id: Int, action: (Button) -> Unit) {
        val b = findViewById<Button>(id)
        b.setOnClickListener { action(b) }
    }

    private fun render() {
        output.text = chunks.joinToString(" ")
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()

    private fun loadLanguage() {
        ready = false
        val tag = if (teluguMode) "te" else "en-US"
        val id = DigitalInkRecognitionModelIdentifier.fromLanguageTag(tag)
        if (id == null) {
            toast("Language not supported")
            return
        }
        val model = DigitalInkRecognitionModel.builder(id).build()
        recognizer?.close()
        recognizer = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
        toast("Downloading language model (needs internet first time)...")
        RemoteModelManager.getInstance()
            .download(model, DownloadConditions.Builder().build())
            .addOnSuccessListener { ready = true; toast("Ready. Start writing.") }
            .addOnFailureListener { toast("Download failed. Turn on internet and switch language again.") }
    }

    private fun recognize() {
        val rec = recognizer ?: return
        if (!ready) { toast("Language model not ready yet"); return }
        val ink = board.buildInk() ?: return
        rec.recognize(ink)
            .addOnSuccessListener { result ->
                val t = result.candidates.firstOrNull()?.text ?: ""
                if (t.isNotEmpty()) {
                    chunks.add(t)
                    render()
                }
                board.clearAll()
            }
            .addOnFailureListener { toast("Could not read: " + it.message) }
    }
}
