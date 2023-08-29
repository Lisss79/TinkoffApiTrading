package ru.lisss79.tinkoffapitrading.activities

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import ru.lisss79.tinkoffapitrading.Deal
import ru.lisss79.tinkoffapitrading.R
import ru.lisss79.tinkoffapitrading.queries_and_responses.Direction
import ru.lisss79.tinkoffapitrading.queries_and_responses.ExecutionReportStatus
import ru.lisss79.tinkoffapitrading.queries_and_responses.OrderState
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class StatisticsActivity : AppCompatActivity() {
    private var text: String = ""
    private lateinit var robotTrades: File                  // лог-файл с результатами выставления заявок

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        val textViewStatistic = findViewById<TextView>(R.id.textViewStatistics)
        robotTrades = File(getExternalFilesDir(null), "robot.txt")
        text = getDealsText(robotTrades)
        textViewStatistic.text = text

        val bottomNavigationView =
            findViewById<BottomNavigationView>(R.id.bottomNavigationViewStatistics)
        bottomNavigationView.setOnItemSelectedListener {
            text = when (it.itemId) {
                R.id.listDeals -> {
                    getDealsText(robotTrades)
                }
                R.id.listResults -> {
                    getResultsText(robotTrades)
                }
                else -> ""
            }
            textViewStatistic.text = text
            true
        }

    }

    private fun getFinancialResult(orders: List<OrderState?>, order: OrderState?): Float {
        val state = order?.executionReportStatus ?: return 0f
        if (state != ExecutionReportStatus.EXECUTION_REPORT_STATUS_FILL) return 0f
        val ordersWithId = orders.filter { it?.orderId == order.orderId }
        if (ordersWithId.size != 2) return 0f
        return if (ordersWithId[0]?.executionReportStatus
            != ExecutionReportStatus.EXECUTION_REPORT_STATUS_NEW
        ) 0f
        else when (order.direction) {
            Direction.ORDER_DIRECTION_BUY -> -order.initialOrderPrice.value
            Direction.ORDER_DIRECTION_SELL -> order.initialOrderPrice.value
            else -> 0f
        }
    }

    private fun getResultsText(file: File): String {
        val builder = StringBuilder()

        fun getLine(deal1: Deal, deal2: Deal): String {
            val date = Date.from(deal1.dateTime)
            val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val displayDate = format.format(date)
            val displayFinRes = String.format("%.2f", deal1.result + deal2.result)
            return "$displayDate: $displayFinRes, ${deal1.figi}"
        }

        val deals = getDeals(file)
        if (deals.size > 1) {
            var index = 0
            while (index < deals.size - 1) {
                val deal1 = deals[index]
                val deal2 = deals[index + 1]
                if ((deal1.figi == deal2.figi) && (deal1.quantity == deal2.quantity)
                    && (deal1.result * deal2.result < 0)
                ) {
                    builder.append(getLine(deal1, deal2)).append("\n")
                    index++
                }
                index++

            }
        } else builder.append("Нет данных")
        return builder.toString()

    }

    private fun getDealsText(file: File): String {
        val builder = StringBuilder()

        fun getLine(result: Deal): String {
            val date = Date.from(result.dateTime)
            val format = SimpleDateFormat("HH:mm:ss dd.MM.yyyy", Locale.getDefault())
            val displayDate = format.format(date)
            return if (result.result > 0f) "Продажа +${result.result} (${result.price}) в $displayDate"
            else "Покупка ${result.result} (${result.price}) в $displayDate"
        }

        val deals = getDeals(file)
        if (deals.isNotEmpty()) deals.withIndex().forEach {
            builder.append(it.index).append(". ").append(getLine(it.value)).append("\n")
        }
        else builder.append("Нет данных")
        return builder.toString()
    }

    private fun getDeals(file: File) =
        try {
            val orderLines = file.readLines()
            val orders = orderLines.map { OrderState.parse(it) }
            orders.map {
                it?.run {
                    val quantity = if (initialSecurityPrice.value != 0f)
                        (initialOrderPrice.value / initialSecurityPrice.value).roundToInt()
                    else 0
                    Deal(
                        figi,
                        orderDate,
                        getFinancialResult(orders, it),
                        initialSecurityPrice.value,
                        quantity
                    )
                } ?: Deal()
            }.filter { it.result != 0f }
        } catch (e: IOException) {
            listOf()
        }

}