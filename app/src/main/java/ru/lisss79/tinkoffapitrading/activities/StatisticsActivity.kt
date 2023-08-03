package ru.lisss79.tinkoffapitrading.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import ru.lisss79.tinkoffapitrading.R
import ru.lisss79.tinkoffapitrading.queries_and_responses.Direction
import ru.lisss79.tinkoffapitrading.queries_and_responses.ExecutionReportStatus
import ru.lisss79.tinkoffapitrading.queries_and_responses.OrderState
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

class StatisticsActivity : AppCompatActivity() {
    private lateinit var robotTrades: File                  // лог-файл с результатами выставления заявок

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        robotTrades = File(getExternalFilesDir(null), "robot.txt")
        val orderLines = robotTrades.readLines()
        val orders = orderLines.map { OrderState.parse(it) }
        val results = orders.map { getFinancialResult(orders, it) to it?.orderDate }
            .filter { it.first != 0f }

        val textViewStatistic = findViewById<TextView>(R.id.textViewStatistics)
        results.forEach {
            textViewStatistic.append(getText(it) + "\n")
        }

    }

    private fun getFinancialResult(orders: List<OrderState?>, order: OrderState?): Float {
        val state = order?.executionReportStatus ?: return 0f
        if (state != ExecutionReportStatus.EXECUTION_REPORT_STATUS_FILL) return 0f
        val ordersWithId = orders.filter { it?.orderId == order.orderId }
        if (ordersWithId.size != 2) return 0f
        return if (ordersWithId[0]?.executionReportStatus
            != ExecutionReportStatus.EXECUTION_REPORT_STATUS_NEW) 0f
        else when (order.direction) {
            Direction.ORDER_DIRECTION_BUY -> -order.initialOrderPrice.value
            Direction.ORDER_DIRECTION_SELL -> order.initialOrderPrice.value
            else -> 0f
        }
    }

    private fun getText(result: Pair<Float, Instant?>): String {
        val date = if (result.second != null) Date.from(result.second) else Date(0L)
        val format = SimpleDateFormat("HH:mm:ss dd.MM.yyyy", Locale.getDefault())
        val displayDate = format.format(date)
        return if (result.first > 0f) "Продажа +${result.first} в $displayDate"
        else "Покупка ${result.first} в $displayDate"
    }
}