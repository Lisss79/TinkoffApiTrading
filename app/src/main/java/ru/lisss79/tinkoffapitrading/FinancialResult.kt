package ru.lisss79.tinkoffapitrading

import java.time.Instant

// Финансовый результат покупки или продажи
data class FinancialResult(
    val dateTime: Instant? = null,
    val result: Float = 0f,
    val price: Float? = 0f
)
