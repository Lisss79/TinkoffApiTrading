package ru.lisss79.tinkofftradingrobot.activities

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import ru.lisss79.tinkofftradingrobot.*
import ru.lisss79.tinkofftradingrobot.data_classes.InfoForWidget
import ru.lisss79.tinkofftradingrobot.data_classes.TradingConfig
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.ACCOUNTS
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.ACCOUNTS_NAMES
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.CONFIG
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.LAST_PURCHASE_PRICE
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.ORDER
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.SCHEDULE_NEXT
import ru.lisss79.tinkofftradingrobot.queries_and_responses.PostOrder
import java.io.File
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.time.*
import java.util.*
import kotlin.system.exitProcess


@SuppressLint("UnspecifiedImmutableFlag")
class MainActivity : AppCompatActivity() {
    private lateinit var api: TinkoffOpenApi
    private lateinit var intentRobot: Intent
    private lateinit var alarmManager: AlarmManager
    private lateinit var tvInfo: TextView
    private lateinit var tvOrder: TextView
    private lateinit var tvAlarmInfo: TextView
    private lateinit var tvError: TextView
    private lateinit var pbLoading: ProgressBar
    private lateinit var intentSettings: Intent
    private lateinit var receiver: ResultReceiver
    private lateinit var prefs: SharedPreferences
    private var workId = UUID.nameUUIDFromBytes(NIGHTLY_WORKER_ID.encodeToByteArray())

    private lateinit var activityLauncher: ActivityResultLauncher<Intent>
    private var waitingDialog: AlertDialog? = null

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_options_menu, menu)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        title = String.format(getString(R.string.title_activity_main), BuildConfig.VERSION_NAME)

        requestBatteryPermission()
        val settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs = getSharedPreferences(packageName, MODE_PRIVATE)
        val token = settingsPrefs.getString(getString(R.string.TOKEN), "") ?: ""
        api = TinkoffOpenApi(token)
        intentSettings = Intent(this, SettingsActivity::class.java)

        val loadingDialog = createWaitingDialog()
        loadingDialog.show()

        // Асинхронно читаем данные об аккаунте
        val mainHandler = Handler(mainLooper)
        val future = api.getAccounts()
        future.thenAcceptAsync { response ->
            loadingDialog.dismiss()
            val accounts = response.first
            val responseCode = response.second

            // Если данные получены, передать их в intent в настройки
            if (accounts != null) {
                val length = accounts.accounts.size
                val accountsIds = Array(length) { accounts.accounts[it].id }
                val accountsNames = Array(length) { accounts.accounts[it].name }
                intentSettings.putExtra(ACCOUNTS, accountsIds)
                intentSettings.putExtra(ACCOUNTS_NAMES, accountsNames)
            }

            // Иначе показать сообщение об ошибке
            else {
                val readingDialog = AlertDialog.Builder(this).apply {
                    this.setTitle("Ошибка")
                        .setIcon(R.drawable.ic_error)
                        .setPositiveButton("OK", null)
                        .setNegativeButton("Выход") { _, _ ->
                            exitProcess(0)
                        }
                        .setNeutralButton("Повтор") { _, _ ->
                            recreate()
                        }
                }
                val message = when (responseCode) {
                    HttpURLConnection.HTTP_UNAUTHORIZED ->
                        "Невозможно соединиться с сервером. Проверьте токен."
                    HttpURLConnection.HTTP_INTERNAL_ERROR ->
                        "Внутренняя ошибка сервера."
                    else ->
                        "Невозможно подключиться к серверу. Отсутствует соединение."
                }
                mainHandler.post {
                    readingDialog.setMessage(message).show()
                }
            }
        }

        receiver = setReceiver()
        pbLoading = findViewById(R.id.progress_bar_loading)
        tvAlarmInfo = findViewById(R.id.text_view_alarm_info)
        tvInfo = findViewById(R.id.text_view_info)
        tvOrder = findViewById(R.id.text_view_order)
        tvError = findViewById(R.id.text_view_error)
        val buttonConnect = findViewById<Button>(R.id.button_connect)
        val buttonStop = findViewById<Button>(R.id.button_stop)
        val buttonInfo = findViewById<Button>(R.id.button_info)
        intentRobot = Intent(this, RobotReceiver::class.java)
        intentRobot.putExtra(RECEIVER, receiver)
        checkForAlarmPlanning()

        // Обработка нажатия кнопки Start
        buttonConnect.setOnClickListener {
            checkNightlyWorker(true)
            sendBroadcastToRobot(true)
        }

        // Обработка нажатия кнопки Info
        buttonInfo.setOnClickListener {
            sendBroadcastToRobot(false)
        }

        // Обработка нажатия кнопки Stop
        buttonStop.setOnClickListener {
            val pi = PendingIntent.getBroadcast(
                this, 0, intentRobot,
                PendingIntent.FLAG_NO_CREATE
            )
            pi?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
            WorkManager.getInstance(this).apply {
                cancelWorkById(workId)
                pruneWork()
            }
            sendBroadcastToRobot(false)
        }

        activityLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            waitingDialog?.dismiss()
        }

    }

    /**
     * Отображение индикатора процесса и запуск рассылки для получения роботом.
     * (Фактически запуск робота)
     * @param scheduleNext должен ли робот планировать следующий запуск
     */
    private fun sendBroadcastToRobot(scheduleNext: Boolean) {
        pbLoading.visibility = View.VISIBLE
        intentRobot.putExtra(SCHEDULE_NEXT, scheduleNext)
        println("Starting Broadcast...")
        sendBroadcast(intentRobot)
    }

    private fun checkForAlarmPlanning() {
        val nextTime = prefs.getLong(PLAN_TIME, 0L)
        val today = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()
        val tomorrow = today.plusDays(1)
        val nextTimeLocalDate =
            Instant.ofEpochMilli(nextTime).atZone(ZoneId.systemDefault()).toLocalDate()

        val date = if (today.equals(nextTimeLocalDate)) "СЕГОДНЯ"
        else if (tomorrow.equals(nextTimeLocalDate)) "ЗАВТРА"
        else SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(nextTime))

        val displayDate = String.format("%s в %tT", date, nextTime)

        val alarmUp = (PendingIntent.getBroadcast(
            this, 0, intentRobot,
            PendingIntent.FLAG_NO_CREATE
        ) != null)
        if (alarmUp) {
            if (nextTime > 0L) {
                tvAlarmInfo.text = displayDate
            } else tvAlarmInfo.text = "ЗАПЛАНИРОВАН"
            tvAlarmInfo.setTextColor(Color.GREEN)
        } else {
            tvAlarmInfo.text = "НЕ ЗАПЛАНИРОВАН"
            tvAlarmInfo.setTextColor(Color.RED)
        }
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryPermission() {
        val intentBattery = Intent()
        val packageName = packageName
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            intentBattery.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intentBattery.data = Uri.parse("package:$packageName")
            startActivity(intentBattery)
        }
    }

    private fun setReceiver() =
        object : ResultReceiver(Handler(Looper.getMainLooper())) {
            @Suppress("DEPRECATION")
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                super.onReceiveResult(resultCode, resultData)
                pbLoading.visibility = View.INVISIBLE
                checkForAlarmPlanning()

                val config = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    resultData?.getSerializable(CONFIG, TradingConfig::class.java)
                } else {
                    resultData?.getSerializable(CONFIG) as TradingConfig
                } ?: TradingConfig()
                if (config.accountId.isNotEmpty()) tvInfo.text = config.toString()
                else tvInfo.text = "Ошибка получения данных"

                if (config.error.isNotEmpty()) {
                    val errorText = "\nПроизошла ошибка! ${config.error}"
                    tvError.text = errorText
                } else tvError.text = ""

                val order = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    resultData?.getSerializable(ORDER, PostOrder::class.java)
                } else {
                    resultData?.getSerializable(ORDER) as PostOrder
                } ?: PostOrder()
                if (order.accountId.isNotEmpty())
                    tvOrder.text = String.format("Выставлена новая заявка:\n%s", order.toString())
                else tvOrder.text = "Новая заявка не выставлена"
                updateWidget(InfoForWidget.createFromConfig(config, order))

            }
        }

    private fun checkNightlyWorker(schedule: Boolean): Boolean {
        val scheduledDate = LocalDate.now().plusDays(1)
        val scheduledTime = LocalTime.parse(SCHEDULED_TIME)
        val scheduled = scheduledTime.atDate(scheduledDate).atZone(ZoneId.systemDefault())
        val delay = Duration.between(Instant.now(), scheduled)
        val duration = Duration.ofHours(24)
        return if (schedule) {
            val request = PeriodicWorkRequest.Builder(NightlyWorker::class.java, duration)
                .setInitialDelay(delay).setId(workId).build()
            val operation = WorkManager.getInstance(this).enqueue(request)
            true
        }
        else {
            val workInfo = WorkManager.getInstance(this).getWorkInfoById(workId).get()
            workInfo != null
        }
    }

    /**
     * Обновляет содержимое виджета
     */
    private fun updateWidget(info: InfoForWidget) {
        val manager = AppWidgetManager.getInstance(applicationContext)
        val component = ComponentName(applicationContext, RobotWidget::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isNotEmpty()) {
            val widgetIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            widgetIntent.putExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, info)
            sendBroadcast(widgetIntent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> {
                startActivity(intentSettings)
            }
            R.id.statistics -> {
                waitingDialog = createWaitingDialog()
                waitingDialog?.show()
                val intentStatistics = Intent(this, StatisticsActivity::class.java)
                activityLauncher.launch(intentStatistics)
            }
            R.id.log -> {
                val intentLog = Intent(this, LogActivity::class.java)
                startActivity(intentLog)
            }
            R.id.about -> {
                val text = "Торговый робот Абрам ${BuildConfig.VERSION_NAME}\n" +
                        "Build ${BuildConfig.VERSION_CODE}\n(c)2023 Lisss79 Studio"
                val about = AlertDialog.Builder(this)
                    .setTitle("О программе")
                    .setMessage(text)
                    .setIcon(R.drawable.ic_info)
                    .setPositiveButton("OK", null)
                about.show()
            }
            R.id.lastPriceError -> {
                checkForLastPriceError()
            }
            R.id.lastPriceClear -> {
                clearLastPrice()
            }
            R.id.orderError -> {
                checkForOrderListErrors()
            }
            R.id.isRunningError -> {
                checkForIsRunningErrors()
            }
        }
        return super.onOptionsItemSelected(item)
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

    private fun clearLastPrice() {
        prefs.edit().apply() {
            putFloat(LAST_PURCHASE_PRICE, 0f)
            apply()
        }
        showResult(true)
    }

    private fun checkForIsRunningErrors() {
        val isRunning = prefs.getBoolean(ROBOT_IS_RUNNING, false)
        val scheduled = checkNightlyWorker(false)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Внимание")

        if (!isRunning && scheduled) {
            dialog.setMessage(
                "Ошибок в состоянии запуска робота не найдено. " +
                        "Флаг isRunning не установлен, запуск очистителя флага запланирован"
            )
                .setPositiveButton("OK", null)
                .setIcon(R.drawable.ic_info)
                .show()
        } else if (isRunning && scheduled) {
            dialog.setMessage("Найдена ошибка в состоянии запуска робота. " +
                    "Флаг isRunning установлен. Исправить?")
                .setNegativeButton("Отмена", null)
                .setIcon(R.drawable.ic_error)
                .setPositiveButton("OK") { _, _ ->
                    prefs.edit().apply {
                        putBoolean(ROBOT_IS_RUNNING, false)
                        apply()
                    }
                    showResult(true)
                }
                .show()
        } else if (!isRunning && !scheduled) {
            dialog.setMessage("Найдена ошибка в состоянии запуска робота. " +
                    "Не запланирован запуск очистителя флага. Исправить?")
                .setNegativeButton("Отмена", null)
                .setIcon(R.drawable.ic_error)
                .setPositiveButton("OK") { _, _ ->
                    checkNightlyWorker(true)
                    showResult(true)
                }
                .show()
        }
    }

    private fun checkForLastPriceError() {
        val tradesLog = RobotTradesLog
            .fromFile(File(getExternalFilesDir(null), getString(R.string.robotfile_name)))
        val correct = tradesLog?.checkForLastPurchasePrice(prefs)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Внимание")

        if (correct == null || correct.second) {
            dialog.setMessage("Ошибок в установке последней цены не найдено")
                .setPositiveButton("OK", null)
                .setIcon(R.drawable.ic_info)
                .show()
        } else {
            dialog.setMessage("Найдена ошибка в установке последней цены. Исправить?")
                .setNegativeButton("Отмена", null)
                .setIcon(R.drawable.ic_error)
                .setPositiveButton("OK") { _, _ ->
                    tradesLog.correctLastPurchasePrice(prefs, correct)
                    showResult(true)
                }
                .show()
        }
    }

    private fun checkForOrderListErrors() {
        val file = File(getExternalFilesDir(null), getString(R.string.robotfile_name))
        val tradesLog = RobotTradesLog
            .fromFile(file)
        val errors = tradesLog?.checkForCorrectOrdersList()

        val dialog = AlertDialog.Builder(this)
            .setTitle("Внимание")

        if (errors == null || errors.isEmpty()) {
            dialog.setMessage("Ошибок в списке заявок не найдено")
                .setPositiveButton("OK", null)
                .setIcon(R.drawable.ic_info)
                .show()
        } else {
            dialog.setMessage("Найдены ошибок в списке заявок: ${errors.size}шт.  Исправить?")
                .setNegativeButton("Отмена", null)
                .setIcon(R.drawable.ic_error)
                .setPositiveButton("OK") { _, _ ->
                    val settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this)
                    val newOrders = tradesLog.correctOrderList(api, settingsPrefs, errors)
                    val result = tradesLog
                        .toFile(file, newOrders)
                    showResult(result)
                }
                .show()
        }
    }

    private fun showResult(result: Boolean) {
        AlertDialog.Builder(this)
            .setTitle("Результат")
            .setPositiveButton("OK", null)
            .apply {
                if (result) {
                    this.setMessage("Операция успешно выполнена")
                        .setIcon(R.drawable.ic_info)
                } else {
                    this.setMessage("Не удалось выполнить операцию")
                        .setIcon(R.drawable.ic_error)
                }
            }.show()

    }
}