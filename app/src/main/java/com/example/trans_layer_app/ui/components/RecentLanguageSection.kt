package com.example.trans_layer_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.trans_layer_app.domain.models.LanguageOption
import com.example.trans_layer_app.ui.theme.ChipShape
import com.example.trans_layer_app.ui.theme.PreviewBackgroundHex
import com.example.trans_layer_app.ui.theme.TranslayerappTheme

@Composable
fun RecentLanguageSection(
    text: String,
    options: List<LanguageOption>,
    selected: LanguageOption,
    onSelect: (LanguageOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ShortcutLabel(text = text)
        Spacer(modifier = Modifier.height(8.dp))
        RecentLanguageRow(
            options = options,
            selected = selected,
            onSelect = onSelect
        )
    }
}

@Composable
private fun ShortcutLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun RecentLanguageRow(
    options: List<LanguageOption>,
    selected: LanguageOption,
    onSelect: (LanguageOption) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            RecentLanguageChip(
                option = option,
                selected = option == selected,
                onClick = { onSelect(option) }
            )
        }
    }
}

@Composable
fun RecentLanguageChip(
    option: LanguageOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = ChipShape
    val background = if (selected) {
        MaterialTheme.colorScheme.surface
    } else {
        Color.Transparent
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    Column(
        modifier = modifier
            .wrapContentWidth()
            .widthIn(min = 72.dp)
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = option.label,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            style = if (selected) MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium) else MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, backgroundColor = PreviewBackgroundHex)
@Composable
fun RecentLanguageSectionPreview() {
    TranslayerappTheme {
        RecentLanguageSection(
            text = "タイトル",
            options = LanguageOption.PREVIEWS,
            selected = LanguageOption.PREVIEWS[0],
            onSelect = {}
        )
    }
}
