package ru.lisss79.tinkofftradingrobot

import java.time.Instant

// Успешно выполненная сделка покупки или продажи
data class Deal(
    val figi: String = "",
    val dateTime: Instant = Instant.now(),
    val result: Float = 0f,
    val price: Float = 0f,
    val quantity: Int = 0
)
