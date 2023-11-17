package ru.lisss79.tinkofftradingrobot.data_classes

/**
 * Финансовый результат за месяц/год
 */
data class MonthlyYearlyFinancialResult(
    val financialResult: Float,
    val financialResultPercent: Float,
    val summaryDeals: Int,
    val successfullyDeals: Int
) {
    companion object {

        fun from(result: FinancialResult) =
            MonthlyYearlyFinancialResult(
                result.result,
                result.resultPercent,
                1,
                convertToCounter(result.result)
            )

        fun convertToCounter(value: Float) = if (value > 0) 1 else 0
    }

    operator fun plus(otherResult: MonthlyYearlyFinancialResult) =
        MonthlyYearlyFinancialResult(
            financialResult + otherResult.financialResult,
            financialResultPercent + otherResult.financialResultPercent,
            summaryDeals + otherResult.summaryDeals,
            successfullyDeals + otherResult.successfullyDeals
        )

    operator fun plus(dailyResult: FinancialResult) =
        MonthlyYearlyFinancialResult(
            financialResult + dailyResult.result,
            financialResultPercent + dailyResult.resultPercent,
            summaryDeals + 1,
            successfullyDeals + convertToCounter(dailyResult.result)
        )

}
