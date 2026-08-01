package com.example.trans_layer_app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// App specific custom shapes
val ButtonShape = RoundedCornerShape(24.dp)
val DialogShape = RoundedCornerShape(30.dp)
val ChipShape = RoundedCornerShape(18.dp)
val OverlayShape = RoundedCornerShape(8.dp)
