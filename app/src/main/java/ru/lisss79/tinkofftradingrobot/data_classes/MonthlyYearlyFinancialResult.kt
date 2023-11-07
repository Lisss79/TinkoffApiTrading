package ru.lisss79.tinkofftradingrobot.data_classes

/**
 * Финансовый результат за месяц/год
 */
data class MonthlyYearlyFinancialResult(
    val financialResult: Float,
    val summaryDeals: Int,
    val successfullyDeals: Int
) {
    companion object {

        fun from(result: FinancialResult) =
            MonthlyYearlyFinancialResult(
                result.result,
                1,
                convertToCounter(result.result)
            )

        fun convertToCounter(value: Float) = if (value > 0) 1 else 0
    }

    operator fun plus(otherResult: MonthlyYearlyFinancialResult) =
        MonthlyYearlyFinancialResult(
            financialResult + otherResult.financialResult,
            summaryDeals + otherResult.summaryDeals,
            successfullyDeals + otherResult.successfullyDeals
        )

    operator fun plus(dailyResult: FinancialResult) =
        MonthlyYearlyFinancialResult(
            financialResult + dailyResult.result,
            summaryDeals + 1,
            successfullyDeals + convertToCounter(dailyResult.result)
        )

}
