package ru.lisss79.tinkofftradingrobot

/**
 * Перечисление, определяющее, должна ли быть цена продажи
 * обязательно выше цены покупки
 */
enum class SellingPriceHigher(val rus_name: String) {

    // Необязательно
    NO("Нет"),

    // Всегда выше на один шаг
    EXACTLY_ONE_STEP("Выше ровно на 1 шаг"),

    // Выше как минимум на один шаг или больше, если позволяет стакан
    ONE_STEP_OR_MORE("Выше минимум на 1 шаг");

    companion object {
        val defaultValue = NO
        fun getEntries() =
            values().toList().map { it.name }.toTypedArray()

        fun getRusNames() =
            values().toList().map { it.rus_name }.toTypedArray()
    }

}