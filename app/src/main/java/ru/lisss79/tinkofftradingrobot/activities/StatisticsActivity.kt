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
import ru.lisss79.tinkofftradingrobot.data_classes.MonthlyYearlyFinancialResult
import ru.lisss79.tinkofftradingrobot.queries_and_responses.Direction
import java.io.File
import java.time.LocalDate
import java.time.Period
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
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
                    if (optionsMenu.size() == 0)
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
                tooltipText = "Дата продажи"
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
                tooltipText = "Интервал между покупкой и продажей"
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
                tooltipText = "Финансовый результат купли-продажи"
                gravity = Gravity.END
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT, 4f
                    )
                )
            }

            val displayFinResPercent = String.format("%.2f%s", result.resultPercent, "%")
            TextView(this).apply {
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                text = displayFinResPercent
                tooltipText = "Финансовый результат в процентах"
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
                tooltipText = "FIGI инструмента"
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Small)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.START
                gravity = Gravity.END
                val params = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT, 3f
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
        fun getRow(yearMonth: YearMonth, result: MonthlyYearlyFinancialResult): TableRow {
            val tableRow = TableRow(this)

            val formatter = if (monthly) DateTimeFormatter.ofPattern("MMM yyyy")
            else DateTimeFormatter.ofPattern("yyyy")
            TextView(this).apply {
                text = yearMonth.format(formatter)
                tooltipText = "Интервал времени"
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT, 5f
                    )
                )
            }

            TextView(this).apply {
                text = String.format("%,.2f%s", result.financialResult, currencySymbol)
                tooltipText = "Финансовый результат"
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                gravity = Gravity.CENTER_HORIZONTAL
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT, 5f
                    )
                )
            }

            TextView(this).apply {
                text = String.format("%,.2f%s", result.financialResultPercent, "%")
                tooltipText = "Финансовый результат в процентах"
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                gravity = Gravity.CENTER_HORIZONTAL
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT, 3f
                    )
                )
            }

            TextView(this).apply {
                text = String.format("%d", result.successfullyDeals)
                tooltipText = "Успешных сделок"
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                gravity = Gravity.CENTER_HORIZONTAL
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT, 2f
                    )
                )
            }

            TextView(this).apply {
                text = String.format("%d", result.summaryDeals)
                tooltipText = "Всего сделок"
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                gravity = Gravity.CENTER_HORIZONTAL
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT, 2f
                    )
                )
            }

            return tableRow
        }

        val log = RobotTradesLog.fromFile(file)
        val deals = log?.getDeals() ?: listOf()

        // Создаем карту. Ключ - месяц и год (или только год, а месяц - январь)
        // Значение - пара финансовый результат и число сделок в указанный месяц/год
        val monthlyResults = mutableMapOf<YearMonth, MonthlyYearlyFinancialResult>()

        getResults(deals).forEach {
            val currYearMonth =
                if (monthly) YearMonth.from(it.dateTime2.atZone(ZoneId.systemDefault()))
                else YearMonth.from(it.dateTime2.atZone(ZoneId.systemDefault())).withMonth(1)
            val currResult = monthlyResults[currYearMonth]
            if (currResult != null) monthlyResults[currYearMonth] = currResult + it
            else monthlyResults[currYearMonth] =
                MonthlyYearlyFinancialResult.from(it)
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
        fun firstPurchaseIndex() =
            try {
                val entry = deals.first { it.direction == Direction.ORDER_DIRECTION_BUY }
                deals.indexOf(entry)
            } catch (e: NoSuchElementException) {
                e.printStackTrace()
                -1
            }

        fun dealsInOneDirection(index: Int): List<Deal> {
            val results = mutableListOf<Deal>()
            val direction = deals[index].direction
            val figi = deals[index].figi
            var tempIndex = index
            while (tempIndex < deals.size && deals[tempIndex].direction == direction
                && deals[tempIndex].figi == figi
            ) {
                results.add(deals[tempIndex])
                tempIndex++
            }
            return results
        }

        val results = mutableListOf<FinancialResult>()
        var index = firstPurchaseIndex()
        while (index < deals.size) {
            val deals1 = dealsInOneDirection(index)
            index += deals1.size
            if (index < deals.size) {
                val deals2 = dealsInOneDirection(index)
                index += deals2.size
                results.add(FinancialResult.fromDeals(deals1, deals2))
            } else break
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
                tooltipText = "Порядковый номер"
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT, 1f
                    )
                )
            }

            TextView(this).apply {
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                text = if (it.value.direction == Direction.ORDER_DIRECTION_SELL) "Продажа"
                else "Покупка"
                tooltipText = "Направление сделки"
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT, 2f
                    )
                )
            }

            TextView(this).apply {
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                text = String.format("%+,d%s", it.value.result.roundToInt(), currencySymbol)
                tooltipText = "Сумма сделки"
                gravity = Gravity.END
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT, 2f
                    )
                )
            }

            val dateTime = LocalDate.from(it.value.dateTime.atZone(ZoneId.systemDefault()))
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yy")
            TextView(this).apply {
                setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium)
                text = dateTime.format(formatter)
                tooltipText = "Дата сделки"
                gravity = Gravity.END
                tableRow.addView(
                    this, TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT, 2f
                    )
                )
            }

            table.addView(tableRow)
        }
    }

}