package com.bit.bithub.data

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
     * Маппинг технических имен категорий из БД в читаемые названия и иконки.
     */
    fun getCategoryDisplayInfo(category: String): Pair<String, String> {
        return when (category.lowercase()) {
            "health_fitness" -> "🍏" to "Здоровье"
            "tools" -> "🛠️" to "Инструменты"
            "communication" -> "📱" to "Связь"
            "entertainment" -> "🚀" to "Развлечения"
            "photography" -> "📷" to "Фото"
            "music" -> "🎧" to "Музыка"
            "finance" -> "💰" to "Финансы"
            "games", "Игры" -> "🎮" to "Игры"
            "action" -> "💥" to "Экшен"
            "rpg" -> "🗡️" to "RPG"
            "puzzle" -> "🧠" to "Головоломки"
            "racing" -> "🏎️" to "Гонки"
            "sports" -> "💪" to "Спорт"
            "simulation" -> "💻" to "Симуляторы"
            else -> "📦" to category.replaceFirstChar { it.uppercase() }
        }
    }
}
