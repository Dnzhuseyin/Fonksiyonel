package com.example.fonksiyonel.model

import androidx.compose.ui.graphics.Color

data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val earnedDate: Long = 0,
    val color1: Color = Color.Gray,
    val color2: Color = Color.LightGray
)
