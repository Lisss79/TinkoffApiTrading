package ru.lisss79.tinkofftradingrobot.activities

import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import ru.lisss79.tinkofftradingrobot.R
import ru.lisss79.tinkofftradingrobot.RobotTradesLog
import ru.lisss79.tinkofftradingrobot.data_classes.Deal
import ru.lisss79.tinkofftradingrobot.data_classes.FinancialResult
import java.io.File
import java.time.LocalDate
import java.time.Period
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToInt

class StatisticsActivity : AppCompatActivity() {
    private val SCROLL_DELAY_MS = 100L
    private lateinit var robotTrades: File                  // лог-файл с результатами выставления заявок
    private lateinit var tableStatistics: TableLayout
    private lateinit var scrollViewStats: ScrollView
    private lateinit var optionsMenu: Menu
    private val currencySymbol = Currency.getInstance(Locale.getDefault()).symbol

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        tableStatistics = findViewById(R.id.tableLayoutStatistics)
        scrollViewStats = findViewById(R.id.scrollViewStatistics)
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
                    invalidateOptionsMenu()
                }
                R.id.listResults -> {
                    showResultsTable(robotTrades, tableStatistics)
                    menuInflater.inflate(R.menu.statistics_options_menu, optionsMenu)
                }
            }
            scrollViewStats
                .postDelayed({ scrollViewStats.fullScroll(View.FOCUS_DOWN) }, SCROLL_DELAY_MS)
            true
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        optionsMenu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        tableStatistics.removeAllViews()
        when (item.itemId) {
            R.id.daily -> {
                showResultsTable(robotTrades, tableStatistics)
            }
            R.id.monthly -> {
                showResultsTableMonthlyYearly(robotTrades, tableStatistics, true)
            }
            R.id.yearly -> {
                showResultsTableMonthlyYearly(robotTrades, tableStatistics, false)
            }
            android.R.id.home -> onBackPressed()
        }
        scrollViewStats.postDelayed(
            { scrollViewStats.fullScroll(View.FOCUS_DOWN) },
            SCROLL_DELAY_MS
        )
        return true
    }

    /**
     * Вывод на экран таблицы с финансовым результатом всех сделок
     */
    private fun showResultsTable(file: File, table: TableLayout) {
        fun getRow(result: FinancialResult): TableRow {
            val tableRow = TableRow(this)

            val date1 = LocalDate.from(result.dateTime1.atZone(ZoneId.systemDefault()))
            val date2 = LocalDate.from(result.dateTime2.atZone(ZoneId.systemDefault()))
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yy")
            TextView(this).apply {
                val displayText = date2.format(formatter)
                text = displayText
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT, 4f
                    )
                )
            }

            val period = Period.between(date1, date2)
            TextView(this).apply {
                val displayText = "${period.days}дн"
                text = displayText
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER_HORIZONTAL
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT, 3f
                    )
                )
            }


            val displayFinRes = String.format("%+,d%s", result.result.roundToInt(), currencySymbol)
            TextView(this).apply {
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                text = displayFinRes
                gravity = Gravity.END
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT, 4f
                    )
                )
            }

            TextView(this).apply {
                text = result.figi
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Small)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.START
                gravity = Gravity.END
                val params = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT, 5f
                )
                val px = TypedValue
                    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, resources.displayMetrics)
                params.marginStart = px.toInt()
                tableRow.addView(this, params)
            }

            return tableRow
        }

        val log = RobotTradesLog.fromFile(file)
        val deals = log?.getDeals() ?: listOf()

        getResults(deals).forEach {
            table.addView(getRow(it))
        }

    }

    /**
     * Вывод на экран таблицы с финансовым результатом по месяцам или годам
     */
    private fun showResultsTableMonthlyYearly(file: File, table: TableLayout, monthly: Boolean) {
        fun getRow(yearMonth: YearMonth, result: Float): TableRow {
            val tableRow = TableRow(this)

            val formatter = if (monthly) DateTimeFormatter.ofPattern("MMM yyyy")
            else DateTimeFormatter.ofPattern("yyyy")
            TextView(this).apply {
                text = yearMonth.format(formatter)
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT, 3f
                    )
                )
            }

            TextView(this).apply {
                text = String.format("%,.2f", result)
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                gravity = Gravity.CENTER_HORIZONTAL
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT, 3f
                    )
                )
            }

            return tableRow
        }

        val log = RobotTradesLog.fromFile(file)
        val deals = log?.getDeals() ?: listOf()
        val monthlyResults = mutableMapOf<YearMonth, Float>()

        getResults(deals).forEach {
            val currYearMonth =
                if (monthly) YearMonth.from(it.dateTime2.atZone(ZoneId.systemDefault()))
                else YearMonth.from(it.dateTime2.atZone(ZoneId.systemDefault())).withMonth(1)
            val currResult = monthlyResults[currYearMonth]
            if (currResult != null) monthlyResults[currYearMonth] = currResult + it.result
            else monthlyResults[currYearMonth] = it.result
        }

        monthlyResults.forEach { (yearMonth, result) ->
            table.addView(getRow(yearMonth, result))
        }

    }

    /**
     * Получает список финансовых результатов
     * @param deals список сделок
     */
    private fun getResults(deals: List<Deal>): List<FinancialResult> {
        val results = mutableListOf<FinancialResult>()
        if (deals.size > 1) {
            var index = 0
            while (index < deals.size - 1) {
                val deal1 = deals[index]
                val deal2 = deals[index + 1]
                if ((deal1.figi == deal2.figi) && (deal1.quantity == deal2.quantity)
                    && (deal1.result * deal2.result < 0)
                ) {
                    results.add(FinancialResult.fromDeals(deal1, deal2))
                    index++
                }
                index++

            }
        }
        return results
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
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT, 1f
                    )
                )
            }

            TextView(this).apply {
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                text = if (it.value.result > 0) "Продажа" else "Покупка"
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT, 3f
                    )
                )
            }

            TextView(this).apply {
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                text = String.format("%+,d%s", it.value.result.roundToInt(), currencySymbol)
                gravity = Gravity.END
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT, 4f
                    )
                )
            }

            val dateTime = LocalDate.from(it.value.dateTime.atZone(ZoneId.systemDefault()))
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yy")
            TextView(this).apply {
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                text = dateTime.format(formatter)
                gravity = Gravity.END
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT, 3f
                    )
                )
            }

            table.addView(tableRow)
        }
    }

}