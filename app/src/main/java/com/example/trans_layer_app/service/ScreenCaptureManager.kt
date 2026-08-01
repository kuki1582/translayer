package com.example.trans_layer_app.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.example.trans_layer_app.utils.WindowUtils
import kotlinx.coroutines.delay

class ScreenCaptureManager(private val context: Context) {
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var captureWidth = 0
    private var captureHeight = 0

    fun setMediaProjection(projection: MediaProjection) {
        this.mediaProjection = projection
        setupImageReader()
    }

    fun stop() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    fun setupImageReader(width: Int? = null, height: Int? = null) {
        val projection = mediaProjection ?: return

        val (finalWidth, finalHeight) = if (width != null && height != null) {
            width to height
        } else {
            WindowUtils.getScreenSize(context)
        }

        // サイズが変わっていない場合は何もしない
        if (captureWidth == finalWidth && captureHeight == finalHeight && imageReader != null) {
            return
        }

        captureWidth = finalWidth
        captureHeight = finalHeight
        val densityDpi = context.resources.displayMetrics.densityDpi

        // 新しいImageReaderを作成
        val newImageReader = ImageReader.newInstance(
            finalWidth,
            finalHeight,
            PixelFormat.RGBA_8888,
            2
        )

        val currentVirtualDisplay = virtualDisplay
        if (currentVirtualDisplay != null) {
            currentVirtualDisplay.resize(finalWidth, finalHeight, densityDpi)
            currentVirtualDisplay.surface = newImageReader.surface
            imageReader?.close()
        } else {
            virtualDisplay = projection.createVirtualDisplay(
                "ScreenCapture",
                finalWidth,
                finalHeight,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                newImageReader.surface,
                null,
                null
            )
        }
        imageReader = newImageReader
    }

    suspend fun captureScreen(): Bitmap? {
        delay(400)
        val image = imageReader?.acquireLatestImage() ?: return null
        return try {
            processImage(image)
        } catch (e: Exception) {
            Log.e("ScreenCaptureManager", "画像変換中にエラー: ${e.message}")
            null
        } finally {
            image.close()
        }
    }

    private fun processImage(image: Image): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = createBitmap(image.width + rowPadding / pixelStride, image.height)
        bitmap.copyPixelsFromBuffer(buffer)

        val cleanBitmap = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
        bitmap.recycle()

        val maxDimension = maxOf(cleanBitmap.width, cleanBitmap.height)
        val scale = 2048f / maxOf(maxDimension, 1)
        return if (scale < 1f) {
            cleanBitmap.scale(
                (cleanBitmap.width * scale).toInt(),
                (cleanBitmap.height * scale).toInt()
            ).also { cleanBitmap.recycle() }
        } else {
            cleanBitmap
        }
    }

    fun getCaptureSize(): Pair<Int, Int> = captureWidth to captureHeight
}
