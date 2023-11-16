package ru.lisss79.tinkofftradingrobot.data_classes

import java.time.Instant

/**
 * Финансовый результат покупки-продажи
 */
data class FinancialResult(
    val figi: String = "",
    val dateTime1: Instant = Instant.now(),
    val dateTime2: Instant = Instant.now(),
    val result: Float = 0f
) {

    companion object {
        fun fromDeals(deal1: Deal, deal2: Deal) =
            FinancialResult(
                deal1.figi,
                deal1.dateTime,
                deal2.dateTime,
                deal1.result + deal2.result
            )

        fun fromDeals(deals1: List<Deal>, deals2: List<Deal>): FinancialResult {
            val result1 = deals1.sumOf { it.result.toDouble() }
            val result2 = deals2.sumOf { it.result.toDouble() }
            return FinancialResult(
                deals1.first().figi,
                deals1.first().dateTime,
                deals2.last().dateTime,
                (result1 + result2).toFloat()
            )
        }
    }

}
