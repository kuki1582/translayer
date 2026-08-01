package com.example.trans_layer_app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trans_layer_app.R
import com.example.trans_layer_app.ui.state.FloatingButtonState
import com.example.trans_layer_app.ui.theme.TranslayerTheme
import com.example.trans_layer_app.ui.theme.TranslayerappTheme

/**
 * フローティングボタンの本体サイズ（dp）
 */
const val FLOATING_BUTTON_CONTENT_SIZE_DP = 64

/**
 * アニメーション（バウンド等）がウィンドウ境界で切れないようにするためのパディング（dp）
 * これにより、graphicsLayer の translationY などでボタンが動いても表示が維持されます。
 */
const val FLOATING_BUTTON_WINDOW_PADDING_DP = 20

@Composable
fun FloatingButton(
    state: FloatingButtonState,
    onClick: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit,
) {
    val shape = CircleShape
    val bubbleGradient = Brush.radialGradient(colors = TranslayerTheme.gradients.bubbleButton)
    val glossyHighlight = Brush.radialGradient(colors = TranslayerTheme.gradients.glossyHighlight)
    val infiniteTransition = rememberInfiniteTransition(label = "floating_button_intro")
    val introOffsetY = infiniteTransition.animateFloat(
        initialValue = -18f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                18f at 0
                16f at 420
                6f at 760
                6f at 960
                0f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "floating_button_drop_offset"
    )
    val introScaleX = infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.94f at 0
                1f at 220
                1.12f at 420
                0.98f at 760
                1.03f at 960
                1f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "floating_button_drop_scale_x"
    )
    val introScaleY = infiniteTransition.animateFloat(
        initialValue = 1.08f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                1.08f at 0
                1f at 220
                0.9f at 420
                1.04f at 760
                0.98f at 960
                1f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "floating_button_drop_scale_y"
    )
    val translationY = if (state == FloatingButtonState.INTRO) introOffsetY.value else 0f
    val scaleX = if (state == FloatingButtonState.INTRO) introScaleX.value else 1f
    val scaleY = if (state == FloatingButtonState.INTRO) introScaleY.value else 1f
    val label = when (state) {
        FloatingButtonState.LEFT_COLLAPSED -> ">"
        FloatingButtonState.RIGHT_COLLAPSED -> "<"
        else -> stringResource(R.string.floating_button_translate)
    }
    val contentAlignment =
        when (state) {
            FloatingButtonState.LEFT_COLLAPSED -> Alignment.CenterEnd
            FloatingButtonState.RIGHT_COLLAPSED -> Alignment.CenterStart
            else -> Alignment.Center
        }
    val fontSize = when (state) {
        FloatingButtonState.LEFT_COLLAPSED,
        FloatingButtonState.RIGHT_COLLAPSED -> 20.sp

        else -> 12.sp
    }

    // ウィンドウ内でのクリッピングを防ぐための外側コンテナ
    Box(
        modifier = Modifier
            .size((FLOATING_BUTTON_CONTENT_SIZE_DP + FLOATING_BUTTON_WINDOW_PADDING_DP * 2).dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = shape,
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (state == FloatingButtonState.INTRO) 14.dp else 9.dp
            ),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .size(FLOATING_BUTTON_CONTENT_SIZE_DP.dp) // ここで本来のサイズを指定
                .graphicsLayer {
                    this.translationY = translationY
                    this.scaleX = scaleX
                    this.scaleY = scaleY
                }
                .clip(shape)
                .border(
                    width = 1.2.dp,
                    color = Color.White.copy(alpha = 0.55f),
                    shape = shape
                )
                .clickable { onClick() }
        ) {
            Box(
                contentAlignment = contentAlignment,
                modifier = Modifier
                    .fillMaxSize()
                    .background(bubbleGradient)
                    .padding(horizontal = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 8.dp, y = 7.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(glossyHighlight)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-10).dp, y = (-10).dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.28f))
                )
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.White.copy(alpha = 0.65f),
                            blurRadius = 16f
                        )
                    )
                )
            }
        }
    }
}

class FloatingButtonStateProvider : PreviewParameterProvider<FloatingButtonState> {
    override val values = sequenceOf(
        FloatingButtonState.READY,
        FloatingButtonState.RIGHT_COLLAPSED
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun FloatingButtonPreviews(
    @PreviewParameter(FloatingButtonStateProvider::class) state: FloatingButtonState
) {
    TranslayerappTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            FloatingButton(
                state = state,
                onClick = {},
                onDrag = { _, _ -> }
            )
        }
    }
}
