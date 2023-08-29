package ru.lisss79.tinkoffapitrading.activities

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
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
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import ru.lisss79.tinkoffapitrading.*
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.ACCOUNTS
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.CONFIG
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.ORDER
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.SCHEDULE_NEXT
import ru.lisss79.tinkoffapitrading.queries_and_responses.PostOrder
import java.text.SimpleDateFormat
import java.util.*


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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        requestBatteryPermission()
        val settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs = getSharedPreferences(packageName, MODE_PRIVATE)
        TOKEN = settingsPrefs.getString("token", "") ?: ""
        api = TinkoffOpenApi(TOKEN)
        intentSettings = Intent(this, SettingsActivity::class.java)
        val accounts = api.getAccounts().get()
        if (accounts != null) {
            val length = accounts.accounts.size
            val values = Array(length) { accounts.accounts[it].id }
            intentSettings.putExtra(ACCOUNTS, values)
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

        buttonConnect.setOnClickListener {
            pbLoading.visibility = View.VISIBLE
            intentRobot.putExtra(SCHEDULE_NEXT, true)
            println("Starting Broadcast...")
            sendBroadcast(intentRobot)
        }

        buttonInfo.setOnClickListener {
            pbLoading.visibility = View.VISIBLE
            intentRobot.putExtra(SCHEDULE_NEXT, false)
            println("Starting Broadcast...")
            sendBroadcast(intentRobot)
        }

        buttonStop.setOnClickListener {
            val pi = PendingIntent.getBroadcast(
                this, 0, intentRobot,
                PendingIntent.FLAG_NO_CREATE
            )
            pi?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
            checkForAlarmPlanning()
        }
    }

    private fun checkForAlarmPlanning() {
        val nextTime = prefs.getLong(PLAN_TIME, 0L)
        val calNextTime = Calendar.getInstance()
        calNextTime.timeInMillis = nextTime
        val today = Calendar.getInstance()
        val date = if(calNextTime.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH))
            "СЕГОДНЯ"
        else if (calNextTime.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH) + 1)
            "ЗАВТРА"
        else
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(nextTime))
        val displayDate = String.format("%s в %tT", date, nextTime)

        val alarmUp = (PendingIntent.getBroadcast(this, 0, intentRobot,
            PendingIntent.FLAG_NO_CREATE) != null)
        if (alarmUp) {
            if (nextTime > 0L) {
                tvAlarmInfo.text = displayDate
            }
            else tvAlarmInfo.text = "ЗАПЛАНИРОВАН"
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
            }
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> {
                startActivity(intentSettings)
            }
            R.id.statistics -> {
                val intentStatistics = Intent(this, StatisticsActivity::class.java)
                startActivity(intentStatistics)
            }
            R.id.log -> {
                val intentLog = Intent(this, LogActivity::class.java)
                startActivity(intentLog)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}