package com.bit.bithub.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class NewsItem(
    val title: String,
    val description: String,
    val imageUrl: String,
    val onClick: () -> Unit = {}
)

object MockData {
    val newsList = listOf(
        NewsItem(
            title = "Летняя распродажа игр",
            description = "Скидки до 90% на хиты этого сезона",
            imageUrl = "https://picsum.photos/id/10/600/400"
        ),
        NewsItem(
            title = "Обновление bit Stream",
            description = "Теперь с поддержкой 4K и HDR",
            imageUrl = "https://picsum.photos/id/20/600/400"
        )
    )

    /**
     * Маппинг технических имен категорий из БД в читаемые названия и иконки ImageVector.
     */
    fun getCategoryDisplayInfo(category: String): Pair<ImageVector, String> {
        return when (category.lowercase()) {
            "health_fitness" -> Icons.Default.HealthAndSafety to "Здоровье"
            "tools" -> Icons.Default.Build to "Инструменты"
            "communication" -> Icons.AutoMirrored.Filled.Chat to "Связь"
            "entertainment" -> Icons.Default.Movie to "Развлечения"
            "photography" -> Icons.Default.PhotoCamera to "Фото"
            "music" -> Icons.Default.MusicNote to "Музыка"
            "finance" -> Icons.Default.AccountBalanceWallet to "Финансы"
            "games", "Игры" -> Icons.Default.Games to "Игры"
            "action" -> Icons.Default.Whatshot to "Экшен"
            "rpg" -> Icons.Default.Shield to "RPG"
            "puzzle" -> Icons.Default.Extension to "Головоломки"
            "racing" -> Icons.Default.DirectionsCar to "Гонки"
            "sports" -> Icons.Default.SportsBasketball to "Спорт"
            "simulation" -> Icons.Default.Computer to "Симуляторы"
            else -> Icons.Default.Inventory2 to category.replaceFirstChar { it.uppercase() }
        }
    }
}
