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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScreenCaptureManager(private val context: Context) {
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var captureWidth = 0
    private var captureHeight = 0

    /**
     * 画面のキャプチャが有効かどうか。
     * 画面共有の許可が得られなかった場合や、ユーザーがシステムの UI から共有を止めた場合は false。
     */
    val isCapturing: Boolean
        get() = mediaProjection != null

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
        val image = imageReader?.acquireLatestImage() ?: return null
        return try {
            // ピクセルのコピーと縮小は重い処理なので、Main スレッドで行わない
            withContext(Dispatchers.Default) { toScaledBitmap(image) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert image to bitmap", e)
            null
        } finally {
            image.close()
        }
    }

    /**
     * [Image] のピクセルを [Bitmap] にコピーし、長い辺が [MAX_BITMAP_DIMENSION_PX] 以下になるよう縮小する。
     *
     * [Image] は ImageReader に返却 (close) する必要があり、そのままでは
     * ML Kit やオーバーレイの表示に渡せないため、ここで Bitmap にコピーしておく。
     */
    private fun toScaledBitmap(image: Image): Bitmap {
        val plane = image.planes[0]
        val pixelStride = plane.pixelStride
        // 1 行のバイト数はハードウェアの都合で揃えられることがあり、その差分が rowPadding になる
        val rowPadding = plane.rowStride - pixelStride * image.width

        // rowPadding を含めた幅でコピーしないと、各行がずれて画像が崩れる
        val paddedBitmap = createBitmap(image.width + rowPadding / pixelStride, image.height)
        paddedBitmap.copyPixelsFromBuffer(plane.buffer)

        // 余白がない端末では切り出し (= もう一度コピー) は不要
        val cleanBitmap = if (rowPadding == 0) {
            paddedBitmap
        } else {
            Bitmap.createBitmap(paddedBitmap, 0, 0, image.width, image.height)
                .also { paddedBitmap.recycle() }
        }

        val maxDimension = maxOf(cleanBitmap.width, cleanBitmap.height)
        val scale = MAX_BITMAP_DIMENSION_PX.toFloat() / maxOf(maxDimension, 1)
        return if (scale < 1f) {
            cleanBitmap.scale(
                (cleanBitmap.width * scale).toInt(),
                (cleanBitmap.height * scale).toInt()
            ).also { cleanBitmap.recycle() }
        } else {
            cleanBitmap
        }
    }

    private companion object {
        const val TAG = "ScreenCaptureManager"
        // OCR と AI への送信に十分で、メモリを使いすぎない最大辺
        const val MAX_BITMAP_DIMENSION_PX = 2048
    }
}
