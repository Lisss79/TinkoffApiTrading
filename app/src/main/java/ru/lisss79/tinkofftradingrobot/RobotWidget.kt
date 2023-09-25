package ru.lisss79.tinkofftradingrobot

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.*
import android.widget.RemoteViews
import ru.lisss79.tinkofftradingrobot.queries_and_responses.Direction
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.SCHEDULE_NEXT
import ru.lisss79.tinkofftradingrobot.queries_and_responses.PostOrder
import java.util.*

class RobotWidget : AppWidgetProvider() {
    private lateinit var views: RemoteViews
    lateinit var appWidgetManager: AppWidgetManager
    lateinit var appWidgetIds: IntArray
    private var inputIntent: Intent? = null
    private var screenDensity = 1f
    private var updateValue: Int = 0
    private var isUpdated: Boolean = false
    private lateinit var context: Context
    private var width: Float = 0f
    private var height: Float = 0f
    private val bitmap: Bitmap by lazy {
        Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
    }

    // Цвета
    private val positiveColor = Color.GREEN
    private val negativeColor = Color.RED

    //private val neutralColor = Color.parseColor("#A0000000")
    private val neutralColor = Color.WHITE

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        this.context = context!!
        println("Calling onReceive of Widget")
        inputIntent = intent
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = intent?.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
        onUpdate(context, appWidgetManager, appWidgetIds)

    }

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        screenDensity = context?.resources?.displayMetrics?.density ?: 1f

        isUpdated = false
        updateValue = 0

        if (inputIntent == null || appWidgetIds == null) return
        val intent = inputIntent!!
        this.appWidgetManager = appWidgetManager!!
        this.appWidgetIds = appWidgetIds
        val options = appWidgetManager.getAppWidgetOptions(appWidgetIds[0])
        width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) * screenDensity
        height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) * screenDensity

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
                // Устанавливаем фон для каждого виджета
                appWidgetIds.forEach { _ ->
                    setImageViewBitmap(
                        R.id.widgetBackground,
                        createBackground(updateValue)
                    )
                }
                setTextViewText(R.id.textViewAlarm, alarmText)
                setTextColor(R.id.textViewAlarm, alarmTextColor)
                appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, this)

                if (!isUpdated) planUpdateAnimation()
                setUpdateKeyListener(isUpdated, this)
            }


        if (intent.hasExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS)) {

            // Если в вызывающем intent уже содержится trading config,
            // извлечь его и обновить данные
            @Suppress("DEPRECATION")
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(
                    AppWidgetManager.EXTRA_CUSTOM_EXTRAS,
                    InfoForWidget::class.java
                )
            } else {
                intent.getSerializableExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS)
                        as InfoForWidget
            } ?: InfoForWidget()
            println(info)
            updateRemoteViews(info)
            println("Widget Updated")
        } else {

            // Если в вызывающем intent не содержится trading config,
            // обратиться к роботу и прочитать данные
            val receiver = setReceiver()
            intentRobot.putExtra(RECEIVER, receiver)
            intentRobot.putExtra(SCHEDULE_NEXT, false)
            println("Widget runs Robot Receiver")
            context?.sendBroadcast(intentRobot)
        }

    }

    /**
     * Устанавливает или сбрасывает обработчик кнопки "обновить"
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun setUpdateKeyListener(register: Boolean, views: RemoteViews) {

        // Обработка нажатия на кнопку "Обновить"
        val updateIntent = Intent(context, RobotWidget::class.java)
        updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        val updatePI = if (register) PendingIntent.getBroadcast(
            context, appWidgetIds[0],
            updateIntent, PendingIntent.FLAG_UPDATE_CURRENT
        ) else null
        views.setOnClickPendingIntent(R.id.imageViewUpdate, updatePI)
    }

    /**
     * Обновить анимацию виджетов
     */
    private fun planUpdateAnimation() {
        val handler = Handler(Looper.getMainLooper())
        if (!isUpdated) {
            handler.postDelayed({
                createBackground(updateValue)
                val shaderWidth = width / 4
                updateValue += 20
                if (updateValue > width + shaderWidth) {
                    updateValue = 0
                    isUpdated = true
                    setUpdateKeyListener(isUpdated, views)
                }
                appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)
                println(updateValue)
                planUpdateAnimation()
            }, 40)
        } else {
            createBackground(null)
            appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)
        }
    }

    private fun createBackground(animValue: Int?): Bitmap {
        val shaderWidth = width / 4
        val cornerRadius = 30f
        bitmap.eraseColor(Color.TRANSPARENT)
        val gradient = if (animValue != null) {
            val value = animValue.toFloat()
            LinearGradient(
                value - shaderWidth, 0f, value, 0f,
                intArrayOf(Color.GRAY, Color.WHITE, Color.GRAY), null, Shader.TileMode.CLAMP
            )
        } else null

        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            if (animValue != null) {
                shader = gradient
                style = Paint.Style.FILL
                alpha = 150
            } else {
                color = Color.GRAY
                shader = null
                style = Paint.Style.FILL
                alpha = 150
            }
        }
        canvas.drawRoundRect(0f, 0f, width, height, cornerRadius, cornerRadius, paint)
        return bitmap
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

                val order = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    resultData?.getSerializable(JsonKeys.ORDER, PostOrder::class.java)
                } else {
                    resultData?.getSerializable(JsonKeys.ORDER) as PostOrder
                } ?: PostOrder()

                updateRemoteViews(InfoForWidget.createFromConfig(config, order))
                println("Widget Updated")
            }
        }

    private fun updateRemoteViews(info: InfoForWidget) {

        var orderText: String
        var orderTextColor: Int
        var tradesText: String
        var tradesTextColor: Int

        views.apply {
            //setInt(R.id.widgetMainLayout, "setBackgroundResource", R.drawable.background3)
            if (info.isError) {
                orderText = "Ошибка получения"
                orderTextColor = negativeColor
                tradesText = "Данные недоступны"
                tradesTextColor = negativeColor
            } else {
                orderText = when (info.orderDirection) {
                    Direction.ORDER_DIRECTION_SELL -> {
                        orderTextColor = neutralColor
                        "Заявка: продажа"
                    }
                    Direction.ORDER_DIRECTION_BUY -> {
                        orderTextColor = neutralColor
                        "Заявка: покупка"
                    }
                    else -> {
                        orderTextColor = negativeColor
                        "Заявка НЕ выставлена"
                    }
                }
            }
            tradesText = if (info.isError) {
                tradesTextColor = negativeColor
                "Нет данных"
            } else if (info.tradesAvailable) {
                tradesTextColor = positiveColor
                "Торги идут"
            } else {
                tradesTextColor = neutralColor
                "Торги НЕ ведутся"
            }
            setTextViewText(R.id.textViewOrder, orderText)
            setTextColor(R.id.textViewOrder, orderTextColor)
            setTextViewText(R.id.textViewTrades, tradesText)
            setTextColor(R.id.textViewTrades, tradesTextColor)
        }
        //isUpdated = true
        //updateValue = 0
        appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)
    }
}