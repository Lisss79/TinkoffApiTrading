package ru.lisss79.tinkofftradingrobot

enum class MarketOrders(val rus_name: String) {

    // Никогда
    NEVER("Никогда"),

    // Только при повышенном спросе
    INCREASED_BID("При повышенном спросе"),

    // Всегда
    ALWAYS("Всегда");

    companion object {
        val defaultValue = NEVER
        fun getEntries() =
            MarketOrders.values().toList().map { it.name }.toTypedArray()

        fun getRusNames() =
            MarketOrders.values().toList().map { it.rus_name }.toTypedArray()
    }

}