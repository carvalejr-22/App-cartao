package com.carlos.appcartao

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ReceiptOcrInstrumentedTest {
    @Test
    fun bundledLatinRecognizerInitializesAndProcessesBitmap() {
        val bitmap = Bitmap.createBitmap(1400, 700, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 86f
        }
        canvas.drawText("DROGARIA TESTE", 80f, 180f, paint)
        canvas.drawText("VALOR A PAGAR R$ 52,60", 80f, 350f, paint)
        canvas.drawText("08/09/2026", 80f, 520f, paint)

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val result = Tasks.await(
                recognizer.process(InputImage.fromBitmap(bitmap, 0)),
                30,
                TimeUnit.SECONDS
            )
            assertTrue("ML Kit iniciou, mas não retornou texto", result.text.isNotBlank())
        } finally {
            recognizer.close()
            bitmap.recycle()
        }
    }
}
