package ru.lisss79.tinkofftradingrobot

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.*
import android.widget.RemoteViews
import ru.lisss79.tinkofftradingrobot.queries_and_responses.Direction
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.SCHEDULE_NEXT
import java.util.*

class RobotWidget : AppWidgetProvider() {
    lateinit var views: RemoteViews
    lateinit var appWidgetManager: AppWidgetManager
    lateinit var appWidgetIds: IntArray
    private var inputIntent: Intent? = null

    // Цвета
    private val positiveColor = Color.GREEN
    private val negativeColor = Color.RED

    //private val neutralColor = Color.parseColor("#A0000000")
    private val neutralColor = Color.WHITE

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        println("Calling onReceive of Widget")
        inputIntent = intent
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = intent?.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
        onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        if (inputIntent == null || appWidgetIds == null) return
        val intent = inputIntent!!
        this.appWidgetManager = appWidgetManager!!
        this.appWidgetIds = appWidgetIds

        val intentRobot = Intent(context, RobotReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            0, intentRobot, PendingIntent.FLAG_NO_CREATE
        )

        var alarmText = "Запуск НЕ запланирован"
        var alarmTextColor = negativeColor
        if (pi != null) {
            val displayDate = context?.getSharedPreferences(
                context.packageName,
                Context.MODE_PRIVATE
            )?.let { prefs ->
                val nextTime = prefs.getLong(PLAN_TIME, 0L)
                val calNextTime = Calendar.getInstance()
                calNextTime.timeInMillis = nextTime
                String.format(" в %tT", nextTime)
            }
            alarmText = if (displayDate != null) "Запуск в $displayDate" else "Запуск запланирован"
            alarmTextColor = positiveColor
        }
        views = RemoteViews(context?.packageName, R.layout.widget_layout)
            .apply {
                setTextViewText(R.id.textViewAlarm, alarmText)
                setTextColor(R.id.textViewAlarm, alarmTextColor)
                appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, this)

                // Обработка нажатия на кнопку "Обновить"
                val updateIntent = Intent(context, RobotWidget::class.java)
                updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                val updatePI = PendingIntent.getBroadcast(
                    context, 0,
                    updateIntent, PendingIntent.FLAG_UPDATE_CURRENT
                )
                setOnClickPendingIntent(R.id.imageViewUpdate, updatePI)
            }


        if (intent.hasExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS)) {

            // Если в вызывающем intent уже содержится trading config,
            // извлечь его и обновить данные
            @Suppress("DEPRECATION")
            val config = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(
                    AppWidgetManager.EXTRA_CUSTOM_EXTRAS,
                    TradingConfig::class.java
                )
            } else {
                intent.getSerializableExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS)
                        as TradingConfig
            } ?: TradingConfig()
            println(config)
            updateRemoteViews(config)
            println("Widget Updated")
        } else {

            // Если в вызывающем intent не содержится trading config,
            // обратиться к роботу и прочитать данные
            val receiver = setReceiver()
            intentRobot.putExtra(RECEIVER, receiver)
            //pbLoading.visibility = View.VISIBLE
            intentRobot.putExtra(SCHEDULE_NEXT, false)
            println("Widget runs Robot Receiver")
            context?.sendBroadcast(intentRobot)
        }

    }

    private fun setReceiver() =
        object : ResultReceiver(Handler(Looper.getMainLooper())) {
            @Suppress("DEPRECATION")
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                super.onReceiveResult(resultCode, resultData)

                val config = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    resultData?.getSerializable(JsonKeys.CONFIG, TradingConfig::class.java)
                } else {
                    resultData?.getSerializable(JsonKeys.CONFIG) as TradingConfig
                } ?: TradingConfig()

                updateRemoteViews(config)
                println("Widget Updated")
            }
        }

    private fun updateRemoteViews(config: TradingConfig) {

        val isError = config.accountId.isEmpty()

        val orderIsSet = config.activeOrders > 0
        val order = config.activeOrderDirection
        val trades = config.tradesAvailable

        var orderText: String
        var orderTextColor: Int
        var tradesText: String
        var tradesTextColor: Int

        views.apply {
            //setInt(R.id.widgetMainLayout, "setBackgroundResource", R.drawable.background3)
            if (isError) {
                orderText = "Ошибка получения"
                orderTextColor = negativeColor
                tradesText = "Данные недоступны"
                tradesTextColor = negativeColor
            } else {
                orderText = if (orderIsSet) {
                    orderTextColor = neutralColor
                    when (order) {
                        Direction.ORDER_DIRECTION_SELL -> "Заявка: продажа"
                        Direction.ORDER_DIRECTION_BUY -> "Заявка: покупка"
                        else -> "Заявка выставлена"
                    }
                } else {
                    orderTextColor = negativeColor
                    "Заявка НЕ выставлена"
                }
                tradesText = if (trades) {
                    tradesTextColor = positiveColor
                    "Торги идут"
                } else {
                    tradesTextColor = neutralColor
                    "Торги НЕ ведутся"
                }
            }
            setTextViewText(R.id.textViewOrder, orderText)
            setTextColor(R.id.textViewOrder, orderTextColor)
            setTextViewText(R.id.textViewTrades, tradesText)
            setTextColor(R.id.textViewTrades, tradesTextColor)
        }

        appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)
    }

}