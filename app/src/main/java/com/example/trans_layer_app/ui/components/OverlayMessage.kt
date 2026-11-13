package com.example.trans_layer_app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.trans_layer_app.ui.theme.MessageBorderColor
import com.example.trans_layer_app.ui.theme.MessageContainerColor
import com.example.trans_layer_app.ui.theme.MessageTextColor
import com.example.trans_layer_app.ui.theme.PreviewBackgroundHex
import com.example.trans_layer_app.ui.theme.TranslayerappTheme

/**
 * 他のアプリの上に重ねて表示する、短いメッセージ用のカード。
 *
 * Service から出す Toast は、通知が許可されていないと表示されないため、
 * エラーなどはこのカードをオーバーレイとして表示する。
 */
@Composable
fun OverlayMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            // 影がウィンドウの端で切れないよう、上下にも余白をとる
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .widthIn(max = 320.dp),
        shape = MaterialTheme.shapes.large,
        // テーマの surface は半透明 (ガラス風) で、下の画像が透けて読みにくいため使わない
        colors = CardDefaults.cardColors(containerColor = MessageContainerColor),
        border = BorderStroke(1.dp, MessageBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color = MessageTextColor,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, backgroundColor = PreviewBackgroundHex)
@Composable
fun OverlayMessagePreview() {
    TranslayerappTheme {
        OverlayMessage(message = "現在、翻訳サービスを利用できません。時間をおいてお試しください")
    }
}
