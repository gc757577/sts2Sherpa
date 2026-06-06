package com.gchan.sts2_sherpa.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OcrCardRecognizer(
    private val context: Context,
) {
    private val recognizer = TextRecognition.getClient(
        KoreanTextRecognizerOptions.Builder().build(),
    )

    suspend fun recognizeText(uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        return recognizer.process(image).await().text
    }

    suspend fun recognizeText(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        return recognizer.process(image).await().text
    }

    private suspend fun Task<Text>.await(): Text =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                continuation.resume(result)
            }
            addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
            addOnCanceledListener {
                continuation.cancel()
            }
        }
}
