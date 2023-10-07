package ru.lisss79.tinkofftradingrobot.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import ru.lisss79.tinkofftradingrobot.Deal
import ru.lisss79.tinkofftradingrobot.R
import ru.lisss79.tinkofftradingrobot.RobotTradesLog
import ru.lisss79.tinkofftradingrobot.queries_and_responses.Direction
import ru.lisss79.tinkofftradingrobot.queries_and_responses.ExecutionReportStatus
import ru.lisss79.tinkofftradingrobot.queries_and_responses.OrderState
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.math.roundToInt

class StatisticsActivity : AppCompatActivity() {
    private val SCROLL_DELAY_MS = 100L
    private lateinit var robotTrades: File                  // лог-файл с результатами выставления заявок

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        val tableStatistics = findViewById<TableLayout>(R.id.tableLayoutStatistics)
        val scrollViewStats = findViewById<ScrollView>(R.id.scrollViewStatistics)
        robotTrades = File(getExternalFilesDir(null), getString(R.string.robotfile_name))
        showDealsTable(robotTrades, tableStatistics)
        scrollViewStats
            .postDelayed({ scrollViewStats.fullScroll(View.FOCUS_DOWN) }, SCROLL_DELAY_MS)

        val bottomNavigationView =
            findViewById<BottomNavigationView>(R.id.bottomNavigationViewStatistics)
        bottomNavigationView.setOnItemSelectedListener {
            tableStatistics.removeAllViews()
            when (it.itemId) {
                R.id.listDeals -> {
                    showDealsTable(robotTrades, tableStatistics)
                }
                R.id.listResults -> {
                    showResultsTable(robotTrades, tableStatistics)
                }
            }
            scrollViewStats
                .postDelayed({ scrollViewStats.fullScroll(View.FOCUS_DOWN) }, SCROLL_DELAY_MS)
            true
        }

    }

    /**
     * Вывод на экран таблицы с финансовым результатом
     */
    private fun showResultsTable(file: File, table: TableLayout) {
        fun getRow(deal1: Deal, deal2: Deal): TableRow {
            val tableRow = TableRow(this)

            val date = Date.from(deal1.dateTime)
            TextView(this).apply {
                text = String.format("%te.%tm.%ty", date, date, date)
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT, 2f
                    )
                )
            }


            val displayFinRes = String.format("%+.2f", deal1.result + deal2.result)
            TextView(this).apply {
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                text = displayFinRes
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT, 2f
                    )
                )
            }

            TextView(this).apply {
                text = deal1.figi
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT, 3f
                    )
                )
            }

            return tableRow
        }

        val log = RobotTradesLog.fromFile(file)
        val deals = log?.getDeals() ?: listOf()

        if (deals.size > 1) {
            var index = 0
            while (index < deals.size - 1) {
                val deal1 = deals[index]
                val deal2 = deals[index + 1]
                if ((deal1.figi == deal2.figi) && (deal1.quantity == deal2.quantity)
                    && (deal1.result * deal2.result < 0)
                ) {
                    table.addView(getRow(deal1, deal2))
                    index++
                }
                index++

            }
        }
    }

    /**
     * Вывод на экран таблицы сделок
     */
    private fun showDealsTable(file: File, table: TableLayout) {
        val log = RobotTradesLog.fromFile(file)
        val deals = (log?.getDeals() ?: listOf()).withIndex()
        deals.forEach {
            val tableRow = TableRow(this)

            TextView(this).apply {
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                text = it.index.inc().toString()
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT, 1f
                    )
                )
            }

            TextView(this).apply {
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                text = if (it.value.result > 0) "Продажа" else "Покупка"
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT, 4f
                    )
                )
            }

            TextView(this).apply {
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                text = String.format("%+d", it.value.result.roundToInt())
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT, 4f
                    )
                )
            }

            val dateTime = Date.from(it.value.dateTime)
            TextView(this).apply {
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                text = String.format("%tR", dateTime)
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT, 3f
                    )
                )
            }

            TextView(this).apply {
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                text = String.format("%te.%tm.%ty", dateTime, dateTime, dateTime)
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT, 3f
                    )
                )
            }

            table.addView(tableRow)
        }
    }

}