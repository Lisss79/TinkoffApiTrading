package ru.lisss79.tinkoffapitrading

import java.time.Instant

// Финансовый результат покупки или продажи
data class FinancialResult(
    val ticker: String = "",
    val dateTime: Instant = Instant.now(),
    val result: Float = 0f,
    val price: Float = 0f
)
