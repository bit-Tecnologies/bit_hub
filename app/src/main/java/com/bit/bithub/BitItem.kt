package com.bit.bithub

import androidx.compose.ui.graphics.Color

data class BitItem(
    val id: Int,
    val title: String,
    val developer: String,
    val rating: Double,
    val reviews: String,
    val size: String,
    val description: String,
    val iconUrl: String,
    val iconColor: Color,
    val isGame: Boolean = false,
)

val allBitsData = listOf(
    BitItem(1, "bit Stream", "Stream Inc.", 4.5, "12K", "45MB", "Лучшее приложение для стриминга вашего контента.", "https://picsum.photos/id/1/200/200", Color(0xFF673AB7)),
    BitItem(2, "bit Pixel Art", "Design Studio", 4.8, "5K", "12MB", "Рисуйте пиксель-арт с легкостью.", "https://picsum.photos/id/2/200/200", Color(0xFF009688)),
    BitItem(3, "bit Code Runner", "Dev Tools", 4.2, "8K", "30MB", "Компилируйте код прямо на смартфоне.", "https://picsum.photos/id/3/200/200", Color(0xFFE91E63)),
    BitItem(4, "bit Notes Plus", "Productivity", 4.7, "20K", "15MB", "Умные заметки с облаком.", "https://picsum.photos/id/4/200/200", Color(0xFF9C27B0)),
    BitItem(5, "bit Monster Hunter", "GameDev Hub", 4.9, "1M", "2GB", "Эпическая RPG игра.", "https://picsum.photos/id/5/300/200", Color(0xFFFF9800), isGame = true),
    BitItem(6, "bit Racing Pro", "Speed Games", 4.6, "500K", "1.2GB", "Быстрые гонки онлайн.", "https://picsum.photos/id/6/300/200", Color(0xFFF44336), isGame = true),
    BitItem(7, "bit Space Puzzle", "Logic Games", 4.4, "100K", "150MB", "Головоломки в космосе.", "https://picsum.photos/id/7/300/200", Color(0xFF2196F3), isGame = true)
)
