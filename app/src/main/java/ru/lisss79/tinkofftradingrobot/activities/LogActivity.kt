package ru.lisss79.tinkofftradingrobot.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Html
import android.text.Html.FROM_HTML_MODE_COMPACT
import android.view.*
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import ru.lisss79.tinkofftradingrobot.DatePickerWithNeutralButton
import ru.lisss79.tinkofftradingrobot.R
import ru.lisss79.tinkofftradingrobot.RobotTradesLog
import ru.lisss79.tinkofftradingrobot.queries_and_responses.ExecutionReportStatus.EXECUTION_REPORT_STATUS_FILL
import ru.lisss79.tinkofftradingrobot.queries_and_responses.ExecutionReportStatus.EXECUTION_REPORT_STATUS_NEW
import ru.lisss79.tinkofftradingrobot.queries_and_responses.OperationsResponse
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class LogActivity : AppCompatActivity() {
    private val LOG_FILE = 1
    private val LOG_ROBOT = 2
    private val LOG_MONEY = 3
    private val SCROLL_DELAY_MS = 50L
    private var scrollPositionLog = -1
    private var scrollPositionRobot = -1
    private var scrollPositionMoney = -1
    private var selectedLog = LOG_FILE
    private var endDate = LocalDate.now()
    private var startDate = LocalDate.of(1970, 1, 1)
    private lateinit var logFile: File
    private lateinit var robotTrades: File
    private lateinit var moneyLog: File
    private lateinit var scrollViewLog: ScrollView
    private lateinit var textViewLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        logFile = File(getExternalFilesDir(null), getString(R.string.logfile_name))
        robotTrades = File(getExternalFilesDir(null), getString(R.string.robotfile_name))
        moneyLog = File(getExternalFilesDir(null), getString(R.string.moneyfile_name))
        scrollViewLog = findViewById(R.id.scrollViewLog)
        textViewLog = findViewById(R.id.textViewLog)
        registerForContextMenu(textViewLog)
        readAndShowLog()

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationViewLog)
        bottomNavigationView.setOnItemSelectedListener {
            textViewLog.text = ""
            when (it.itemId) {
                R.id.logFile -> {
                    selectedLog = LOG_FILE
                    readAndShowLog()
                }

                R.id.robotLog -> {
                    selectedLog = LOG_ROBOT
                    println("scrollPosition: $scrollPositionLog")
                    readAndShowRobot()
                }

                R.id.moneyLog -> {
                    selectedLog = LOG_MONEY
                    println("scrollPosition: $scrollPositionLog")
                    readAndShowMoney()
                }
            }
            true
        }

        scrollViewLog.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            // Отслеживаем изменение позиции скроллинга только если есть текст
            if (textViewLog.text.isNotEmpty()) {
                when (selectedLog) {
                    LOG_FILE -> scrollPositionLog = scrollY
                    LOG_ROBOT -> scrollPositionRobot = scrollY
                    LOG_MONEY -> scrollPositionMoney = scrollY
                }
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.log_options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.range -> {
                selectRange()
            }

            android.R.id.home -> onBackPressed()
        }
        return true
    }

    private fun selectRange() {
        val constraints = CalendarConstraints.Builder()
            .setEnd(Calendar.getInstance().timeInMillis)
            .build()
        val dateRangePickerBuilder = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Выберите даты:")
            .setCalendarConstraints(constraints)
            .setTheme(
                com.google.android.material
                    .R.style.ThemeOverlay_MaterialComponents_MaterialCalendar
            )
        if (!(startDate.isEqual(LocalDate.of(1970, 1, 1)) &&
                    endDate.isEqual(LocalDate.now()))
        ) {
            val selection = androidx.core.util.Pair(
                startDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
                endDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            )
            dateRangePickerBuilder.setSelection(selection)
        }
        val dateRangePicker = dateRangePickerBuilder.build()
        dateRangePicker.addOnPositiveButtonClickListener {
            startDate = LocalDate.from(
                Instant
                    .ofEpochMilli(it.first).atZone(ZoneId.of("UTC"))
            )
            endDate = LocalDate.from(
                Instant
                    .ofEpochMilli(it.second).atZone(ZoneId.systemDefault())
            )
            when (selectedLog) {
                LOG_FILE -> {
                    scrollPositionLog = -1
                    readAndShowLog()
                }

                LOG_ROBOT -> {
                    scrollPositionRobot = -1
                    readAndShowRobot()
                }

                LOG_MONEY -> {
                    scrollPositionMoney = -1
                    readAndShowMoney()
                }
            }
        }

        val picker = DatePickerWithNeutralButton(dateRangePicker) {
            startDate = LocalDate.of(1970, 1, 1)
            endDate = LocalDate.now()
            when (selectedLog) {
                LOG_FILE -> {
                    scrollPositionLog = -1
                    readAndShowLog()
                }

                LOG_ROBOT -> {
                    scrollPositionRobot = -1
                    readAndShowRobot()
                }

                LOG_MONEY -> {
                    scrollPositionMoney = -1
                    readAndShowMoney()
                }
            }
        }
        picker.show(supportFragmentManager, "range")
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.log_context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete_log -> {
                when (selectedLog) {
                    LOG_FILE -> {
                        try {
                            logFile.delete()
                            recreate()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }

                    LOG_ROBOT -> {
                        try {
                            robotTrades.delete()
                            recreate()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }

                    else -> {
                        try {
                            moneyLog.delete()
                            recreate()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
                true
            }

            else -> super.onContextItemSelected(item)
        }
    }

    private fun readAndShowMoney() {
        val dialog = createWaitingDialog()
        dialog.show()
        scrollViewLog.postDelayed({
            val log = OperationsResponse.fromFile(moneyLog)
            val entries = log?.operations ?: listOf()

            val text = entries.filter {
                val currDate = LocalDate
                    .from(it.date.atZone(ZoneId.systemDefault()))
                (currDate.isAfter(startDate) || currDate.isEqual(startDate))
                        && (currDate.isBefore(endDate) || currDate.isEqual(endDate))
            }.joinToString(separator = "\n\n") { it.toJsonLog() }
            textViewLog.text = text

            if (scrollPositionMoney == -1) {
                scrollViewLog
                    .postDelayed({ scrollViewLog.fullScroll(View.FOCUS_DOWN) }, SCROLL_DELAY_MS)
            } else {
                val k = scrollPositionMoney
                scrollViewLog
                    .postDelayed({ scrollViewLog.scrollTo(0, k) }, SCROLL_DELAY_MS)
            }
            dialog.dismiss()
        }, SCROLL_DELAY_MS)
    }

    private fun readAndShowLog() {
        fun getLogEntries(): List<Pair<LocalDate, String>> {
            val logEntries = mutableListOf<Pair<LocalDate, String>>()
            val lines = try {
                logFile.readLines()
            } catch (e: IOException) {
                listOf("Ошибка чтения данных")
            }
            var tempText = StringBuilder()
            var currDate = LocalDate.now()
            var index = 0
            while (index < lines.size) {
                val entryDate = lines[index].startsFromInstant()
                if (entryDate != null) {
                    logEntries.add(entryDate to tempText.toString())
                    tempText = StringBuilder(lines[index] + "\n")
                    currDate = entryDate
                } else {
                    tempText.append(lines[index] + "\n")
                }
                index++
            }
            logEntries.add(currDate to tempText.toString())
            return logEntries
        }

        val dialog = createWaitingDialog()
        dialog.show()
        scrollViewLog.postDelayed({
            val entries = getLogEntries()
            val text = entries.filter {
                (it.first.isAfter(startDate) || it.first.isEqual(startDate))
                        && (it.first.isBefore(endDate) || it.first.isEqual(endDate))
            }.joinToString(separator = "\n") { it.second }
            textViewLog.text = text

            if (scrollPositionLog == -1) {
                scrollViewLog
                    .postDelayed({ scrollViewLog.fullScroll(View.FOCUS_DOWN) }, SCROLL_DELAY_MS)
            } else {
                val k = scrollPositionLog
                scrollViewLog
                    .postDelayed({ scrollViewLog.scrollTo(0, k) }, SCROLL_DELAY_MS)
            }
            dialog.dismiss()
        }, SCROLL_DELAY_MS)
    }

    private fun readAndShowRobot() {

        val dialog = createWaitingDialog()
        dialog.show()
        scrollViewLog.postDelayed({
            val log = RobotTradesLog.fromFile(robotTrades)
            val entries = log?.orders ?: listOf()

            val text = entries.filter {
                if (it?.orderState != null) {
                    val currDate = LocalDate
                        .from(it.orderState.orderDate.atZone(ZoneId.systemDefault()))
                    (currDate.isAfter(startDate) || currDate.isEqual(startDate))
                            && (currDate.isBefore(endDate) || currDate.isEqual(endDate))
                } else false
            }.joinToString(separator = "<br><br>") {
                if (it != null) {
                    val color = when (it.orderState?.executionReportStatus) {
                        EXECUTION_REPORT_STATUS_FILL -> "GREEN"
                        EXECUTION_REPORT_STATUS_NEW -> "YELLOW"
                        else -> "RED"
                    }
                    "<font COLOR='$color'>${it.toJsonLog()}</font>"
                } else ""
            }
            textViewLog.text = Html.fromHtml(text, FROM_HTML_MODE_COMPACT)

            if (scrollPositionRobot == -1) {
                scrollViewLog
                    .postDelayed({ scrollViewLog.fullScroll(View.FOCUS_DOWN) }, SCROLL_DELAY_MS)
            } else {
                val k = scrollPositionRobot
                scrollViewLog
                    .postDelayed({ scrollViewLog.scrollTo(0, k) }, SCROLL_DELAY_MS)
            }
            dialog.dismiss()
        }, SCROLL_DELAY_MS)
    }

    /**
     * Создание диалогового окна на время ожидания
     */
    @SuppressLint("InflateParams")
    private fun createWaitingDialog(): AlertDialog {
        val groupView: View = layoutInflater.inflate(R.layout.loading_dialog, null)
        return AlertDialog.Builder(this)
            .setView(groupView)
            .setCancelable(false)
            .create()
    }

    private fun String.startsFromInstant() = try {
        val instant = Instant.parse(substringBefore('Z') + 'Z')
        LocalDate.from(instant.atZone(ZoneId.systemDefault()))
    } catch (e: Exception) {
        null
    }

}