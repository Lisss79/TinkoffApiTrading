package ru.lisss79.tinkoffapitrading

/**
 * Перечисление для выбора приоритета цены в зависимости от ситуации
 */
enum class PricePriority(val rus_name: String) {

    // Приоритет цены последнего часа
    PRIORITY_RECENT_PRICE("Последние цены"),

    // Приоритет цены из стакана
    PRIORITY_PRICE_ORDER_BOOK("Лучшие цены в стакане"),

    // Приоритет цены с макс. количеством из стакана
    PRIORITY_PRICE_ORDER_BOOK_WITH_QUANTITY("Цены в стакане с макс. объемом"),

    // Приоритет цены с допустимым от макс. количества из стакана
    PRIORITY_PRICE_ORDER_BOOK_WITH_QUANTITY_TOLERANCE("Лучшие цены с допуском объема"),

    // Приоритет цены закрытия
    PRIORITY_CLOSE_PRICE("Цена закрытия");

    companion object {
        val defaultValue = PRIORITY_CLOSE_PRICE
        fun getEntries() =
            PricePriority.values().toList().map { it.name }.toTypedArray()
        fun getRusNames() =
            PricePriority.values().toList().map { it.rus_name }.toTypedArray()
    }

}