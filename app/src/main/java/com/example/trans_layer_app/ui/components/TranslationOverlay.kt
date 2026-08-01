package com.example.trans_layer_app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.trans_layer_app.data.models.OcrItem
import com.example.trans_layer_app.ui.theme.OverlayShape
import com.example.trans_layer_app.ui.theme.PreviewBackgroundHex
import com.example.trans_layer_app.ui.theme.TranslayerTheme
import com.example.trans_layer_app.ui.theme.TranslayerappTheme
import kotlin.math.roundToInt

private const val MAX_VERTICAL_BLOCK_GAP = 24

@Composable
fun TranslationOverlay(
    results: List<OcrItem>,
    isLoading: Boolean,
    backgroundBitmap: Bitmap?,
    onClick: () -> Unit,
) {
    val labelColor = Color.Yellow.copy(alpha = 0.9f)
    val loadingGradient = Brush.radialGradient(colors = TranslayerTheme.gradients.bubbleButton)
    val displayResults = results.mergeNearbyOverlappingBlocks()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
    ) {
        val overlayWidth = constraints.maxWidth.toFloat()
        val overlayHeight = constraints.maxHeight.toFloat()

        // スナップショット画像を背景として表示
        backgroundBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }

        // ロード中のみ少し暗くするオーバーレイ
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim)
            )
        }
        displayResults.forEach { result ->
            val rawTop = (result.box2d[0] / 1000f) * overlayHeight
            val rawLeft = (result.box2d[1] / 1000f) * overlayWidth

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = rawLeft.roundToInt(),
                            y = rawTop.roundToInt()
                        )
                    }
                    .shadow(
                        elevation = 8.dp,
                        shape = OverlayShape,
                        clip = false
                    )
                    .clip(OverlayShape)
                    .background(labelColor)
                    .border(
                        width = 1.2.dp,
                        color = MaterialTheme.colorScheme.inverseSurface,
                        shape = OverlayShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = result.text,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodySmall.copy(
                        shadow = Shadow(
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            blurRadius = 8f
                        )
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(all = 2.dp)
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .shadow(14.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(loadingGradient)
                    .border(1.2.dp, MaterialTheme.colorScheme.inverseSurface, CircleShape)
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.inversePrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

/**
 * 上下に近接し、横方向に重なる連続した OCR block を一つの表示用アイテムへ結合する。
 *
 * `box2d` は `[top, left, bottom, right]` の順で 0〜1000 に正規化された矩形座標を持つ。
 * 上から順に block を比較し、直前の block と垂直方向の間隔が
 * [MAX_VERTICAL_BLOCK_GAP] 以内かつ横方向に重なる場合、テキストを改行で連結する。
 * 結合後のアイテムは、グループ先頭 block の位置を保持する。
 */
private fun List<OcrItem>.mergeNearbyOverlappingBlocks(): List<OcrItem> {
    val groups = mutableListOf<MutableList<OcrItem>>()

    sortedBy { result -> result.box2d[0] }.forEach { result ->
        val previousBlock = groups.lastOrNull()?.lastOrNull()
        if (previousBlock?.isNearbyAndOverlapping(result) == true) {
            groups.last() += result
        } else {
            groups += mutableListOf(result)
        }
    }

    return groups.map { group ->
        group.first().copy(
            text = group.joinToString(separator = "\n") { result -> result.text }
        )
    }
}

private fun OcrItem.isNearbyAndOverlapping(other: OcrItem): Boolean =
    other.box2d[0] <= box2d[2] + MAX_VERTICAL_BLOCK_GAP &&
            other.box2d[1] < box2d[3] &&
            other.box2d[3] > box2d[1]

@Preview(showBackground = true, backgroundColor = PreviewBackgroundHex)
@Composable
fun TranslationOverlayPreview() {
    val items = listOf(
        OcrItem(id = 1, text = "翻訳されたテキスト", box2d = listOf(200, 200, 250, 600)),
        OcrItem(id = 2, text = "もう一つのテキスト", box2d = listOf(400, 100, 450, 500))
    )
    TranslayerappTheme {
        TranslationOverlay(
            results = items,
            isLoading = false,
            backgroundBitmap = null,
            onClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = PreviewBackgroundHex)
@Composable
fun TranslationOverlayLoadingPreview() {
    TranslayerappTheme {
        TranslationOverlay(
            results = emptyList(),
            isLoading = true,
            backgroundBitmap = null,
            onClick = {}
        )
    }
}
