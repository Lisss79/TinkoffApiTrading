package ru.lisss79.tinkofftradingrobot

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.*
import android.content.Context.MODE_PRIVATE
import android.os.*
import androidx.preference.PreferenceManager
import ru.lisss79.tinkofftradingrobot.data_classes.InfoForWidget
import ru.lisss79.tinkofftradingrobot.data_classes.TradingConfig
import ru.lisss79.tinkofftradingrobot.enums.MarketOrders
import ru.lisss79.tinkofftradingrobot.enums.PricePriorityWithData
import ru.lisss79.tinkofftradingrobot.enums.PricePriorityWithData.PricePriority
import ru.lisss79.tinkofftradingrobot.enums.SellingPriceHigher
import ru.lisss79.tinkofftradingrobot.enums.TradingDayState
import ru.lisss79.tinkofftradingrobot.queries_and_responses.*
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.CONFIG
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.LAST_PURCHASE_PRICE
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.ORDER
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.ORDER_ID
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.SCHEDULE_NEXT
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.floor

// Тип рабочего инструмента
const val MY_INSTRUMENT_KIND = "INSTRUMENT_TYPE_ETF"

// Тип id для запроса данных по инструменту
const val MY_ID_TYPE = "INSTRUMENT_ID_TYPE_UID"

// Основная рабочая валюта
const val TRADING_CURRENCY = "rub"

// Идентификатор биржи для работы
const val EXCHANGE_TRADES = "MOEX_PLUS"

// Значение по умолчанию для времени начала дневного аукциона
const val DAY_AUCTION_TIME_DEFAULT = "06:51:01"

// Значение по умолчанию для времени начала вечернего аукциона
const val EVENING_AUCTION_TIME_DEFAULT = "16:01:01"

// Ключ для передачи данных из activity в broadcast receiver
const val RECEIVER = "receiver"

// Ключ для сохранения в shared preferences времени следующего аларма
const val PLAN_TIME = "plan_time"

// Ключ для сохранения в shared preferences о том, запущен ли робот
const val ROBOT_IS_RUNNING = "robot_is_running"

/**
 * Класс - Broadcast Receiver, получающий сообщение от Alarm Manager'а
 */
@SuppressLint("UnspecifiedImmutableFlag")
class RobotReceiver : BroadcastReceiver() {
    private lateinit var alarmManager: AlarmManager
    private lateinit var api: TinkoffOpenApi
    private lateinit var context: Context
    private val config = TradingConfig()

    // Основные настройки
    // Токен для работы API
    var token = ""

    // Тикер рабочего инструмента
    private var instrumentTicker = ""

    // Приоритет определения цены
    // Текущий приоритет
    private var currentPriority: PricePriorityWithData = PricePriorityWithData()

    // Для дневной сессии
    private var tradingDayPriority: PricePriorityWithData = PricePriorityWithData()

    // Для вечерней сессии
    private var tradingEveningPriority: PricePriorityWithData = PricePriorityWithData()

    // Для начала торгового дня
    private var startDayPriority: PricePriorityWithData = PricePriorityWithData()

    // Для аукциона открытия
    private var auctionPriority: PricePriorityWithData = PricePriorityWithData()

    // Для остальных случаев
    private var otherPriority: PricePriorityWithData = PricePriorityWithData()

    // Допуск количества при определении цены по последним
    private var recentTradesQuantityTolerance =
        PricePriorityWithData.getDefaultData(PricePriority.PRIORITY_RECENT_PRICE)

    // Допуск объема при определении по стакану
    private var bestPriceQuantityTolerance = PricePriorityWithData
        .getDefaultData(PricePriority.PRIORITY_PRICE_ORDER_BOOK_WITH_QUANTITY_TOLERANCE)

    // Настройки алгоритма
    // Интервал времени с начала торгов, когда можно рассчитывать цену по сегодняшнему дню
    private var recentIntervalMin = 60

    // Основной интервал обращений к API
    private var mainDelayRequestsMin = 30

    // Сколько денег оставлять после покупки (руб.)
    private var moneyAfterSpending = 900

    // Разрешить ли заменять заявки при изменении цены
    private var replaceOrdersEnabled = false

    // Разрешить ли заменять заявки при изменении цены только вверх по стакану
    // (в сторону скорейшей сделки)
    private var replaceOrdersUp = true

    // Торговать ли в вечернюю сессию
    private var eveningTrades = false

    // Цена продажи выше цены покупки?
    private var sellingPriceHigher = SellingPriceHigher.defaultValue

    // Покупка по рынку?
    private var marketOrders = MarketOrders.defaultValue

    // Коэффициент для определения повышенного спроса (во сколько раз спрос выше предложения)
    private var increasedBidRatio = 2f

    // Запрещать ли покупки в выбранные даты
    private var stopPurchase = false

    // Даты начала и конца периода, когда запрещены покупки
    private var startDate: LocalDate = LocalDate.of(1970, 1, 1)
    private var endDate: LocalDate = startDate

    // Запрещены ли покупки сейчас
    private var stopPurchaseNow = false

    // Покупка по рынку сейчас
    private var marketOrdersNow = false

    private var order = PostOrder()                         // заявка для выставления
    private lateinit var dayInfo: TradingDayState           // состояние торгов сейчас
    private lateinit var orderBook: GetOrderBookResponse    // стакан по инструменту
    private lateinit var activeOrder: GetOrdersResponse.Order  // активная заявка
    private lateinit var logFile: File                      // лог-файл
    private lateinit var robotTrades: File                  // лог-файл с результатами выставления заявок
    private lateinit var prefs: SharedPreferences
    private lateinit var settingsPrefs: SharedPreferences
    private var scheduleNext = true

    // Режим работы Мосбиржи
    private val hourBeforeStartTimeDay = getCalendarFromTime(6, 0, 1)
    private var startTimeAuctionDay = getCalendarFromString(DAY_AUCTION_TIME_DEFAULT)
    private val startTimeDay = getCalendarFromTime(7, 0, 1)
    private val startTimeDayPlusInterval = (startTimeDay.clone() as Calendar).apply {
        add(Calendar.MINUTE, recentIntervalMin)
    }
    private val endTimeDay = getCalendarFromTime(15, 39, 58)
    private val endingTimeDay = (endTimeDay.clone() as Calendar).apply {
        add(Calendar.MINUTE, -mainDelayRequestsMin)
    }
    private var startTimeAuctionEvening = getCalendarFromString(EVENING_AUCTION_TIME_DEFAULT)
    private val startTimeEvening = getCalendarFromTime(16, 5, 1)
    private val endTimeEvening = getCalendarFromTime(20, 50, 1)

    /**
     * Метод, вызываемый при приходе сообщения от Alarm Manager'а
     */
    override fun onReceive(context: Context, intent: Intent) {
        val executorService = Executors.newCachedThreadPool()
        executorService.execute {
            this.context = context
            val powerManager = context.getSystemService(Service.POWER_SERVICE) as PowerManager
            alarmManager = context.getSystemService(Service.ALARM_SERVICE) as AlarmManager
            prefs = context.getSharedPreferences(context.packageName, MODE_PRIVATE)
            getSettingsFromPrefs()
            api = TinkoffOpenApi(token)
            val bcIntent = Intent(context, RobotReceiver::class.java)
            scheduleNext = intent.getBooleanExtra(SCHEDULE_NEXT, true)

            // Начинаем лог, записываем данные о состоянии телефона,
            // получаем данные о дне и следующем запуске робота
            println("Calling onReceive of Robot")
            logFile = File(
                context.getExternalFilesDir(null),
                context.getString(R.string.logfile_name)
            )
            robotTrades = File(
                context.getExternalFilesDir(null),
                context.getString(R.string.robotfile_name)
            )
            logFile.appendText("${Instant.now()} запускаем робота\n")

            // Проверка спящего режима и оптимизации батареи
            if (powerManager.isDeviceIdleMode)
                logFile.appendText("Устройство находится в спящем режиме\n")
            if (!powerManager.isIgnoringBatteryOptimizations(context.packageName))
                logFile.appendText("Включена оптимизация расхода батареи!\n")

            // Определяем режим запуска. Для рабочего режима обнуляем планировщик.
            if (scheduleNext) {
                val pi = PendingIntent.getBroadcast(
                    context, 0, bcIntent,
                    PendingIntent.FLAG_NO_CREATE
                )
                pi?.cancel()
                logFile.appendText("Запуск в рабочем режиме\n")
            } else logFile.appendText("Запуск в режиме просмотра\n")

            dayInfo = getTradingDayState()
            logFile.appendText("Состояние торгового дня: $dayInfo\n")
            val (alarmTime, exact) = getMilliSecOfNextWork(dayInfo)

            // Проверяем, запущена ли другая копия работа
            val isRunning = prefs.getBoolean(ROBOT_IS_RUNNING, false)

            // Завершаем работе, если запущена и перепланируем
            if (isRunning) {
                logFile.appendText("Другая копия работа уже запущена. Завершаем работу\n")
                planNextAlarm(alarmTime, exact, context, intent)
                return@execute
            }

            // Иначе устанавливаем флаг запуска и работаем дальше
            else {
                prefs.edit().apply {
                    putBoolean(ROBOT_IS_RUNNING, true)
                    apply()
                }
            }


            // Если произошла ошибка чтения данных о торговом дне, просто перепланировать робота
            if (dayInfo == TradingDayState.ERROR) {
                logFile.appendText("Ошибка получения данных о торговом дне\n")
                println("Ошибка получения данных о торговом дне")
                config.error = "Невозможно получить данные о торговом дне"
                planNextAlarm(alarmTime, exact, context, intent)
                return@execute
            }

            // Получить основные данные и прекратить работу, если не удалось
            if (!getTradingData()) {
                logFile.appendText("Ошибка получения торговых данных\n")
                println("Ошибка получения торговых данных")
                config.error = "Невозможно получить торговые данные"
                planNextAlarm(alarmTime, exact, context, intent)
                return@execute
            }

            // Проверить состояние последней заявки и прочитать последнюю цену по инструменту
            checkForLastOrder()
            config.closePrice = getClosePrice()

            // Если торги на бирже закрыты, просто перепланировать робота
            if (!dayInfo.isTradingAvailable) {
                logFile.appendText("Сейчас торги не ведутся\n")
                println("Сейчас торги не ведутся")
                planNextAlarm(alarmTime, exact, context, intent)
                return@execute
            }

            // Если вечер и торги вечером запрещены
            else if (dayInfo.isEvening && !eveningTrades) {
                logFile.appendText("Торги вечером запрещены в настройках\n")
                println("Торги вечером запрещены в настройках")
                getActiveOrders()
                planNextAlarm(alarmTime, exact, context, intent)
                return@execute
            }

            // Если торги по инструменту закрыты, просто перепланировать робота
            else if (!config.tradesAvailable) {
                logFile.appendText("Торги инструментом сейчас недоступны\n")
                println("Торги инструментом сейчас недоступны")
                planNextAlarm(alarmTime, exact, context, intent)
                return@execute
            }

            // Если торги доступны
            else {
                // Получаем и сохраняем цены, выбранные по заданному критерию
                val (bidPrice, askPrice) = getPrices()

                // Если значения цен не получены, перепланировать робота и закончить
                if (bidPrice == 0f || askPrice == 0f) {
                    logFile.appendText("Невозможно получить данные о текущих ценах\n")
                    println("Невозможно получить данные о текущих ценах")
                    config.error = "Невозможно получить данные о текущих ценах"
                    planNextAlarm(alarmTime, exact, context, intent)
                    return@execute
                }

                config.selectedPurchasePrice = bidPrice
                config.selectedSellingPrice = askPrice
                getActiveOrders()
                if (scheduleNext) {
                    order = robotCycle(bidPrice, askPrice)
                    logFile.appendText("\nТорговые данные:\n")
                    logFile.appendText(config.toString() + "\n\n")
                    if (order.instrumentId.isNotEmpty()) {
                        logFile.appendText("Создана заявка: $order \n")
                    } else if (order.direction == Direction.ORDER_DIRECTION_UNSPECIFIED) {
                        logFile.appendText("Создавать заявку не нужно\n")
                    } else {
                        logFile.appendText(
                            "Невозможно создать заявку в направлении" +
                                    " ${order.direction.rus_name}\n"
                        )
                    }
                } else {
                    logFile.appendText("Режим просмотра, создавать заявку не нужно\n")
                }

                logFile.appendText("${Instant.now()} завершаем работу работа\n\n")
            }

            // Установить Alarm на новое время
            planNextAlarm(alarmTime, exact, context, intent)
        }

    }

    /**
     * Получить настройки из Shared Preferences
     */
    private fun getSettingsFromPrefs() {
        fun getPricePriorityFromPrefs(
            prefs: SharedPreferences, key: String, keyData: String
        ): PricePriorityWithData {
            val defName = PricePriority.defaultValue.name
            val priority = PricePriority
                .valueOf(prefs.getString(key, defName) ?: defName)
            var data = prefs.getFloat(keyData, 0f)
            if (data == 0f) data = PricePriorityWithData.getDefaultData(priority)
            return PricePriorityWithData(priority, data)
        }

        settingsPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        token = settingsPrefs.getString(context.getString(R.string.TOKEN), "") ?: ""
        instrumentTicker = settingsPrefs.getString(context.getString(R.string.TICKER), "") ?: ""
        sellingPriceHigher = SellingPriceHigher.valueOf(
            settingsPrefs.getString(
                context.getString(R.string.selling_price_higher),
                SellingPriceHigher.defaultValue.name
            ) ?: SellingPriceHigher.defaultValue.name
        )

        marketOrders = MarketOrders.valueOf(
            settingsPrefs.getString(
                context.getString(R.string.market_orders),
                MarketOrders.defaultValue.name
            ) ?: MarketOrders.defaultValue.name
        )

        increasedBidRatio = settingsPrefs.getString(
            context.getString(R.string.increased_bid_ratio),
            increasedBidRatio.toString()
        )?.toFloat() ?: increasedBidRatio

        tradingDayPriority = getPricePriorityFromPrefs(
            settingsPrefs,
            context.getString(R.string.trading_day_priority),
            context.getString(R.string.trading_day_priority_data)
        )


        tradingEveningPriority = getPricePriorityFromPrefs(
            settingsPrefs,
            context.getString(R.string.trading_evening_priority),
            context.getString(R.string.trading_evening_priority_data)
        )

        startDayPriority = getPricePriorityFromPrefs(
            settingsPrefs,
            context.getString(R.string.start_day_priority),
            context.getString(R.string.start_day_priority_data)
        )

        auctionPriority = getPricePriorityFromPrefs(
            settingsPrefs,
            context.getString(R.string.auction_priority),
            context.getString(R.string.auction_priority_data)
        )

        otherPriority = getPricePriorityFromPrefs(
            settingsPrefs,
            context.getString(R.string.other_priority),
            context.getString(R.string.other_priority_data)
        )

        replaceOrdersEnabled = settingsPrefs.getBoolean(
            context
                .getString(R.string.replace_order_enabled), false
        )
        replaceOrdersUp = settingsPrefs.getBoolean(
            context
                .getString(R.string.replace_order_up), false
        )
        eveningTrades = settingsPrefs.getBoolean(
            context
                .getString(R.string.evening_trades), false
        )

        moneyAfterSpending = settingsPrefs
            .getString(
                context.getString(R.string.money_after_spent),
                moneyAfterSpending.toString()
            )?.toInt() ?: moneyAfterSpending
        mainDelayRequestsMin = settingsPrefs
            .getString(
                context.getString(R.string.main_request_delay_min),
                mainDelayRequestsMin.toString()
            )?.toInt() ?: mainDelayRequestsMin
        recentIntervalMin = settingsPrefs
            .getString(
                context.getString(R.string.recent_interval_min),
                recentIntervalMin.toString()
            )?.toInt() ?: recentIntervalMin
        val textDay = settingsPrefs.getString(
            context.getString(R.string.day_auction_time),
            DAY_AUCTION_TIME_DEFAULT
        ) ?: DAY_AUCTION_TIME_DEFAULT
        startTimeAuctionDay = getCalendarFromString(textDay)
        val textEvening = settingsPrefs.getString(
            context.getString(R.string.evening_auction_time),
            EVENING_AUCTION_TIME_DEFAULT
        ) ?: EVENING_AUCTION_TIME_DEFAULT
        startTimeAuctionEvening = getCalendarFromString(textEvening)

        stopPurchase = settingsPrefs.getBoolean(context.getString(R.string.stop_purchase), false)
        if (stopPurchase) {
            val key = context.getString(R.string.stop_purchase_dates)
            val key1 = "${key}_1"
            val key2 = "${key}_2"
            try {
                startDate = LocalDate.parse(settingsPrefs.getString(key1, ""))
                endDate = LocalDate.parse(settingsPrefs.getString(key2, ""))
                val now = LocalDate.now()
                stopPurchaseNow = !(now.isBefore(startDate) || now.isAfter(endDate))
            } catch (e: Exception) {
                e.printStackTrace()
                startDate = LocalDate.of(1970, 1, 1)
                endDate = startDate
                stopPurchaseNow = false

            }
        } else stopPurchaseNow = false
    }

    /**
     * Запланировать следующий Alarm и оповестить вызывающий Intent
     */
    @Suppress("DEPRECATION")
    private fun planNextAlarm(alarmTime: Long, exact: Boolean, context: Context, intent: Intent) {

        // Запланировать Alarm, если нужно
        if (scheduleNext) {
            val bcIntent = Intent(context, RobotReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context, 0, bcIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            if (!exact) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pi)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pi)
            }
            println("Запланирован новый запуск робота")
            logFile.appendText(
                "Новый ${if (exact) "ТОЧНЫЙ" else "неточный"} " +
                        "запуск робота запланирован на ${Date(alarmTime).toInstant()}.\n\n"
            )
            prefs.edit().apply {
                putLong(PLAN_TIME, alarmTime)
                apply()
            }
        } else logFile.appendText("\n")

        val resultReceiver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(RECEIVER, ResultReceiver::class.java)
        } else {
            intent.getParcelableExtra(RECEIVER)
        }
        if (resultReceiver != null) {
            val bundle = Bundle()
            bundle.putSerializable(CONFIG, config)
            bundle.putSerializable(ORDER, order)
            resultReceiver.send(0, bundle)
        } else {
            updateWidget(context, InfoForWidget.createFromConfig(config, order))
        }

        // Сохраняем флаг, что робот завершил работу
        prefs.edit().apply {
            putBoolean(ROBOT_IS_RUNNING, false)
            apply()
        }
    }

    private fun updateWidget(context: Context, info: InfoForWidget) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, RobotWidget::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isNotEmpty()) {
            val widgetIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            widgetIntent.putExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, info)
            context.sendBroadcast(widgetIntent)
        }
    }

    /**
     * Получает
     */
    private fun getCalendarFromString(time: String): Calendar {
        val nowUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val localTime = LocalTime.parse(time, DateTimeFormatter.ISO_TIME)
        return nowUtc.apply {
            set(Calendar.HOUR_OF_DAY, localTime.hour)
            set(Calendar.MINUTE, localTime.minute)
            set(Calendar.SECOND, localTime.second)
        }
    }

    /**
     * Возвращает экземпляр Calendar с заданным временем в часовом поясе UTC
     */
    private fun getCalendarFromTime(hours: Int, minutes: Int, seconds: Int): Calendar {
        val nowUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        return nowUtc.apply {
            set(Calendar.HOUR_OF_DAY, hours)
            set(Calendar.MINUTE, minutes)
            set(Calendar.SECOND, seconds)
        }
    }

    /**
     * Основной цикл робота, принимающий решение о выставлении или нет заявки
     */
    private fun robotCycle(bidPrice: Float, askPrice: Float): PostOrder {

        if (!dayInfo.isTradingAvailable) {
            println("Сейчас торгов нет")
            logFile.appendText("Сейчас торги не ведутся\n")
            return PostOrder()
        }

        return when {
            // Есть активные заявки, проверяем, надо ли менять
            config.activeOrders > 0 -> {
                var postOrder = PostOrder()
                println("Заявка уже выставлена")
                logFile.appendText("Заявка уже выставлена\n")
                if (config.activeOrderDirection == Direction.ORDER_DIRECTION_BUY) {

                    // Условие замены заявки на покупку
                    val condition = ((config.activeOrdersPrice < bidPrice)
                            && replaceOrdersUp)
                            || ((config.activeOrdersPrice != bidPrice)
                            && !replaceOrdersUp)
                    if (condition) {
                        println("Надо заявку менять!")
                        logFile.appendText("Заявка будет заменена!\n")
                        if (replaceOrdersEnabled) {
                            val quantity = config.activeOrdersQuantity.toString()
                            val price = Price.parse(bidPrice)
                            val accountId = config.accountId
                            val orderId = config.activeOrderId
                            val key = UUID.randomUUID().toString()
                            val replaceOrder =
                                ReplaceOrder(quantity, price, accountId, orderId, key)
                            val replaceOrderResponse = api.replaceOrder(replaceOrder).get()
                            replaceOrderResponse?.apply {
                                logFile.appendText("Заявка заменена\n")
                                checkForLastOrder()
                                robotTrades.appendText(toJsonLog())
                                robotTrades.appendText("\n")
                                prefs.edit().apply {
                                    putString(ORDER_ID, replaceOrderResponse.orderId)
                                    apply()
                                }
                                postOrder = PostOrder(
                                    quantity, price, config.activeOrderDirection,
                                    accountId, OrderType.ORDER_TYPE_LIMIT, config.instrumentId
                                )
                            }
                        } else {
                            println("Но замена заявок запрещена")
                            logFile.appendText("Но замена заявок запрещена!\n")
                        }

                    }
                } else if (config.activeOrderDirection == Direction.ORDER_DIRECTION_SELL) {

                    // Условие замены заявки на продажу
                    val condition = ((config.activeOrdersPrice > askPrice)
                            && replaceOrdersUp)
                            || ((config.activeOrdersPrice != askPrice)
                            && !replaceOrdersUp)
                    if (condition) {
                        println("Надо заявку менять!")
                        logFile.appendText("Заявка будет заменена!\n")
                        if (replaceOrdersEnabled) {
                            val quantity = config.activeOrdersQuantity.toString()
                            val price = Price.parse(askPrice)
                            val accountId = config.accountId
                            val orderId = config.activeOrderId
                            val key = UUID.randomUUID().toString()
                            val replaceOrder =
                                ReplaceOrder(quantity, price, accountId, orderId, key)
                            val replaceOrderResponse = api.replaceOrder(replaceOrder).get()
                            replaceOrderResponse?.apply {
                                logFile.appendText("Заявка заменена\n")
                                checkForLastOrder()
                                robotTrades.appendText(toJsonLog())
                                robotTrades.appendText("\n")
                                prefs.edit().apply {
                                    putString(ORDER_ID, replaceOrderResponse.orderId)
                                    apply()
                                }
                                postOrder = PostOrder(
                                    quantity, price, config.activeOrderDirection,
                                    accountId, OrderType.ORDER_TYPE_LIMIT, config.instrumentId
                                )
                            }
                        } else {
                            println("Но замена заявок запрещена")
                            logFile.appendText("Но замена заявок запрещена!\n")
                        }

                    }
                }
                postOrder
            }

            // Есть бумаги на счету, выставляем заявку на продажу
            config.positionQuantity > 0 -> {
                if (askPrice > 0) {

                    val quantity = config.positionQuantity.toString()
                    val price = Price.parse(askPrice)
                    val direction = Direction.ORDER_DIRECTION_SELL
                    val accountId = config.accountId
                    val orderType = OrderType.ORDER_TYPE_LIMIT
                    val instrumentId = config.instrumentId
                    val postOrder =
                        PostOrder(quantity, price, direction, accountId, orderType, instrumentId)
                    println("Создаем заявку:\n$postOrder")
                    val postOrderResponse = api.postOrder(postOrder).get()
                    postOrderResponse?.apply {
                        checkForLastOrder()
                        robotTrades.appendText(toJsonLog())
                        robotTrades.appendText("\n")
                        prefs.edit().apply {
                            putString(ORDER_ID, orderId)
                            apply()
                        }
                    }
                    postOrder
                } else {
                    println("Невозможно создать заявку: не определена цена")
                    logFile.appendText("Невозможно создать заявку: не определена цена\n")
                    PostOrder().apply {
                        direction = Direction.ORDER_DIRECTION_SELL
                    }
                }
            }

            // Нет бумаг на счету, выставляем заявку на покупку
            config.positionQuantity == 0 -> {
                if (bidPrice > 0) {

                    // Если не запрещено выставлять заявки на покупку, то выставляем
                    if (!stopPurchaseNow) {
                        val quantityInt = floor(
                            (config.currencyQuantity -
                                    moneyAfterSpending) / bidPrice
                        ).toInt()

                        // Выставляем заявку только если хватает денег хотя бы на 1 лог
                        if (quantityInt >= 1) {
                            val quantity = quantityInt.toString()
                            val price = Price.parse(bidPrice)
                            val direction = Direction.ORDER_DIRECTION_BUY
                            val accountId = config.accountId
                            val orderType = if (!marketOrdersNow) OrderType.ORDER_TYPE_LIMIT
                            else OrderType.ORDER_TYPE_MARKET
                            val instrumentId = config.instrumentId
                            val postOrder = PostOrder(
                                quantity, price, direction,
                                accountId, orderType, instrumentId
                            )
                            println("Создаем заявку:\n$postOrder")
                            val postOrderResponse = api.postOrder(postOrder).get()
                            postOrderResponse?.apply {
                                checkForLastOrder()
                                robotTrades.appendText(toJsonLog())
                                robotTrades.appendText("\n")
                                prefs.edit().apply {
                                    putString(ORDER_ID, orderId)
                                    apply()
                                }
                            }
                            postOrder
                        } else {
                            logFile.appendText("Недостаточно денег для выставления заявки\n")
                            PostOrder()
                        }
                    }

                    // Если запрещено выставлять заявки на покупку, просто пишем об этом в лог
                    else {
                        logFile.appendText(
                            "Выставление заявок" +
                                    " на покупку сегодня запрещено в настройках!\n"
                        )
                        PostOrder()
                    }

                } else {
                    println("Невозможно создать заявку: не определена цена")
                    logFile.appendText("Невозможно создать заявку: не определена цена\n")
                    PostOrder().apply {
                        direction = Direction.ORDER_DIRECTION_BUY
                    }
                }
            }

            // Непонятно что произошло
            else -> {
                PostOrder()
            }
        }
    }

    /**
     * Проверить состояние последней заявки и обнулить данные о ней,
     * если она выполнена, отменена или отклонена.
     * Обновить и записать в конфиг цену последней покупки
     */
    private fun checkForLastOrder() {
        var lastPurchasePrice = prefs.getFloat(LAST_PURCHASE_PRICE, 0f)
        val lastOrderId = prefs.getString(ORDER_ID, "") ?: ""
        if (lastOrderId.isNotEmpty()) {
            logFile.appendText("Проверяем статус заявки с id=$lastOrderId\n")
            val lastOrder = api.getOrderState(config.accountId, lastOrderId).get()
            when (lastOrder?.executionReportStatus) {
                // Заявка отменена или отклонена
                ExecutionReportStatus.EXECUTION_REPORT_STATUS_CANCELLED,
                ExecutionReportStatus.EXECUTION_REPORT_STATUS_REJECTED -> {
                    lastOrder.orderDate = Instant.now()
                    robotTrades.appendText(lastOrder.toJsonLog())
                    robotTrades.appendText("\n")
                    logFile.appendText("Заявка с id=$lastOrderId была успешно отменена\n")
                    prefs.edit().apply {
                        putString(ORDER_ID, "")
                        apply()
                    }
                }
                // Заявка успешно выполнена
                ExecutionReportStatus.EXECUTION_REPORT_STATUS_FILL -> {
                    lastOrder.orderDate = Instant.now()
                    robotTrades.appendText(lastOrder.toJsonLog())
                    robotTrades.appendText("\n")
                    logFile.appendText("Заявка с id=$lastOrderId была успешно выполнена\n")

                    // Средняя цена последней покупки (0 - если была продажа)
                    lastPurchasePrice = if (lastOrder.direction == Direction.ORDER_DIRECTION_BUY)
                        lastOrder.executedOrderPrice.value / lastOrder.lotsExecuted
                    else 0f

                    prefs.edit().apply {
                        putString(ORDER_ID, "")
                        putFloat(LAST_PURCHASE_PRICE, lastPurchasePrice)
                        apply()
                    }
                }

                // Заявка активна
                ExecutionReportStatus.EXECUTION_REPORT_STATUS_NEW,
                ExecutionReportStatus.EXECUTION_REPORT_STATUS_PARTIALLYFILL -> {
                    logFile.appendText("Заявка с id=$lastOrderId активна\n")
                }

                // Прочие случаи
                else -> {
                    logFile.appendText("Статус заявки с id=$lastOrderId неизвестен\n")
                }
            }
        } else {
            logFile.appendText("Статус заявок не проверяем\n")
        }
        config.lastPurchasePrice = lastPurchasePrice
    }

    /**
     * Возвращает состояние торгового дня в текущий момент и приоритета цены
     */
    private fun getTradingDayState(): TradingDayState {
        val now = Instant.now()
        val isTradingDay: Boolean

        // Получаем данные от сервера, проверяем на null
        val result = api.tradingSchedules(EXCHANGE_TRADES, now, now).get()
        if (result != null && result.exchanges.isNotEmpty()
            && result.exchanges[0].days.isNotEmpty()
        ) {
            val schedule = result.exchanges[0].days[0]
            isTradingDay = schedule.isTradingDay
        } else return TradingDayState.ERROR
        if (!isTradingDay) return TradingDayState.DAY_OFF

        val nowUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        val dayState = when {
            nowUtc.before(hourBeforeStartTimeDay) -> TradingDayState.MORNING_BEFORE_TRADES
            nowUtc.before(startTimeAuctionDay) -> TradingDayState.ONE_HOUR_BEFORE_TRADES
            nowUtc.before(startTimeDay) -> TradingDayState.OPENING_AUCTION_DAY
            nowUtc.before(startTimeDayPlusInterval) -> TradingDayState.TRADING_DAY_START
            nowUtc.before(endingTimeDay) -> TradingDayState.TRADING_DAY
            nowUtc.before(endTimeDay) -> TradingDayState.TRADING_DAY_ENDING
            nowUtc.before(startTimeAuctionEvening) -> TradingDayState.PAUSE_BETWEEN_DAY_AND_EVENING
            nowUtc.before(startTimeEvening) -> TradingDayState.OPENING_AUCTION_EVENING
            nowUtc.before(endTimeEvening) -> TradingDayState.TRADING_EVENING
            nowUtc.after(endTimeEvening) -> TradingDayState.EVENING_AFTER_TRADES
            else -> TradingDayState.DAY_OFF
        }

        currentPriority = when (dayState) {
            TradingDayState.TRADING_DAY,
            TradingDayState.TRADING_DAY_ENDING -> tradingDayPriority
            TradingDayState.TRADING_EVENING -> tradingEveningPriority
            TradingDayState.OPENING_AUCTION_DAY,
            TradingDayState.OPENING_AUCTION_EVENING -> auctionPriority
            TradingDayState.TRADING_DAY_START -> startDayPriority
            else -> otherPriority
        }
        bestPriceQuantityTolerance =
            if (currentPriority.pricePriority ==
                PricePriority.PRIORITY_PRICE_ORDER_BOOK_WITH_QUANTITY_TOLERANCE
            )
                currentPriority.tolerance
            else PricePriorityWithData.getDefaultData(
                PricePriority.PRIORITY_PRICE_ORDER_BOOK_WITH_QUANTITY_TOLERANCE
            )
        recentTradesQuantityTolerance =
            if (currentPriority.pricePriority == PricePriority.PRIORITY_RECENT_PRICE)
                currentPriority.tolerance
            else PricePriorityWithData.getDefaultData(PricePriority.PRIORITY_RECENT_PRICE)

        return dayState
    }

    /**
     * Возвращает число активных заявок и прописывает информацию о заявках в config
     */
    private fun getActiveOrders(): Int {
        val figi = config.figi
        val accountId = config.accountId
        val orders = api.getOrders(accountId).get() ?: return 0
        val ordersWithFigi = orders.orders.filter { it.figi == figi }
        val size = ordersWithFigi.size
        config.activeOrders = size
        if (size > 0) {
            activeOrder = ordersWithFigi[0]
            config.activeOrdersPrice = ordersWithFigi[0].initialSecurityPrice.value
            config.activeOrdersQuantity = ordersWithFigi[0].lotsRequested
            config.activeOrderDirection = ordersWithFigi[0].direction
            config.activeOrderId = ordersWithFigi[0].orderId
        }
        return size
    }

    /**
     * Возвращает Boolean, получены ли данные
     * и прописывает в config информацию о балансе счета и текущих позиций
     */
    private fun getTradingData(): Boolean {

        try {
            val accountId = settingsPrefs.getString(context.getString(R.string.ACCOUNT), null)
                ?: throw Exception("No account info")
            config.accountId = accountId

            config.instrumentTicker = instrumentTicker
            val instruments = api.findInstruments(instrumentTicker, MY_INSTRUMENT_KIND).get()
                ?: throw Exception("Can't find the instrument")
            config.instrumentId = instruments.instruments[0].uid
            config.figi = instruments.instruments[0].figi

            val etf = api.etfBy(config.instrumentId, MY_ID_TYPE).get()
                ?: throw Exception("Can't get instrument info")
            config.minPriceIncrement = etf.minPriceIncrement.value

            val positions = api.getPositions(config.accountId).get()
                ?: throw Exception("Can't find the positions")
            config.currencyQuantity =
                positions.moneys.firstOrNull { it.currency == TRADING_CURRENCY }?.value ?: 0f
            config.positionQuantity =
                positions.securities.firstOrNull { it.figi == config.figi }?.balance ?: 0

            val status = api.getTradingStatus(config.instrumentId).get()
                ?: throw Exception("Can't get trading status")
            config.tradesAvailable =
                status.tradingStatus.isTradingAvailable &&
                        status.limitOrderAvailableFlag == true && status.apiTradeAvailableFlag == true
            logFile.appendText("Торговые данные успешно получены\n")

        } catch (e: Exception) {
            e.printStackTrace()
            logFile.appendText("Can't get trading data\n")
            logFile.appendText("$e\n")
            return false
        }
        return true
    }

    /**
     * Возвращает значения цен для покупки (bid) и продажи (ask).
     * Прописывает в config разные цены из стакана и прошедших сделок
     */
    private fun getPrices(): Pair<Float, Float> {

        fun getRecentPrices() {
            val to = Instant.now()
            val from = to.minusSeconds(60L * recentIntervalMin)

            val trades = api.getLastTrades(from, to, config.instrumentId).get() ?: return

            // Список всех сделок
            val list = trades.trades

            // Создание карты цена - объем (общий для всех сделок по заданной цене)
            // в случае наличия данных в ответе сервера
            if (list.isNotEmpty()) {
                val priceQuantityList = list.map { it.price.value to it.quantity }
                val priceMap = TreeMap<Float, Int>()
                priceQuantityList.forEach {
                    val currQuantity = priceMap[it.first] ?: 0
                    priceMap[it.first] = currQuantity + it.second
                }

                // Максимальный объем торгов
                val maxQuantity = priceMap.map { it.value }.maxOrNull() ?: 0


                // Карта цена - объем (из которого исключены с малым объемом
                // (<QUANTITY_TOLERANCE от максимума))
                val priceMapWithTolerance = priceMap.filter {
                    it.value >= maxQuantity * recentTradesQuantityTolerance
                }

                val prices = Pair(priceMapWithTolerance.minOfOrNull { it.key },
                    priceMapWithTolerance.maxOfOrNull { it.key })
                config.minPriceRecent = prices.first ?: 0f
                config.maxPriceRecent = prices.second ?: 0f
            }
        }

        fun getPricesFromOrderBook() {
            orderBook = api.getOrderBook(config.instrumentId).get() ?: return

            config.bestBidPrice = orderBook.bestBidPrice
            config.bestAskPrice = orderBook.bestAskPrice
            config.bidPriceWithMaxQuantity = orderBook.bidPriceWithMaxQuantity
            config.askPriceWithMaxQuantity = orderBook.askPriceWithMaxQuantity
            config.bidPriceWithQuantityTolerance =
                orderBook.getBidPriceWithQuantityTolerance(bestPriceQuantityTolerance)
            config.askPriceWithQuantityTolerance =
                orderBook.getAskPriceWithQuantityTolerance(bestPriceQuantityTolerance)
        }

        getRecentPrices()
        getPricesFromOrderBook()
        config.closePrice = getClosePrice()

        // Определяем, имеется ли сейчас повышенный спрос, раз уж получили стакан
        config.increasedBid = (orderBook.totalBidQuantity.toFloat() /
                orderBook.totalAskQuantity.toFloat()) > increasedBidRatio

        config.increasedAsk = (orderBook.totalAskQuantity.toFloat() /
                orderBook.totalBidQuantity.toFloat()) > increasedBidRatio

        var result = when (dayInfo) {
            TradingDayState.TRADING_DAY, TradingDayState.TRADING_DAY_ENDING ->
                getPairPrices(tradingDayPriority)

            TradingDayState.TRADING_EVENING ->
                getPairPrices(tradingEveningPriority)

            TradingDayState.OPENING_AUCTION_DAY, TradingDayState.OPENING_AUCTION_EVENING ->
                getPairPrices(auctionPriority)

            TradingDayState.TRADING_DAY_START ->
                getPairPrices(startDayPriority)

            else ->
                getPairPrices(otherPriority)
        }
        if (result == Pair(0f, 0f)) result = getPairPrices(PricePriorityWithData())

        marketOrdersNow = when {
            marketOrders == MarketOrders.ALWAYS -> true
            marketOrders == MarketOrders.INCREASED_BID && config.increasedBid -> true
            else -> false
        }

        return result
    }

    private fun getClosePrice(): Float {
        val closePrice = api.getClosePrices(config.instrumentId).get()
        return closePrice?.closePrice?.value ?: 0f
    }

    /**
     * Возвращает пару цены покупка/продажа исходя из заданного приоритета
     */
    private fun getPairPrices(priorityWithData: PricePriorityWithData): Pair<Float, Float> {
        val pair = when (priorityWithData.pricePriority) {
            PricePriority.PRIORITY_RECENT_PRICE ->
                Pair(config.minPriceRecent, config.maxPriceRecent)
            PricePriority.PRIORITY_PRICE_ORDER_BOOK ->
                Pair(config.bestBidPrice, config.bestAskPrice)
            PricePriority.PRIORITY_PRICE_ORDER_BOOK_WITH_QUANTITY ->
                Pair(config.bidPriceWithMaxQuantity, config.askPriceWithMaxQuantity)
            PricePriority.PRIORITY_PRICE_ORDER_BOOK_WITH_QUANTITY_TOLERANCE ->
                Pair(config.bidPriceWithQuantityTolerance, config.askPriceWithQuantityTolerance)
            PricePriority.PRIORITY_CLOSE_PRICE ->
                Pair(config.closePrice, config.closePrice)
        }

        // Изменение цены продажи, если она должна быть выше цены покупки и известна цена покупки
        val oneStepHigherPrice = kotlin.math
            .round((config.lastPurchasePrice + config.minPriceIncrement) * 100) / 100
        val result = when {
            sellingPriceHigher == SellingPriceHigher.EXACTLY_ONE_STEP &&
                    config.lastPurchasePrice > 0f -> pair.first to oneStepHigherPrice
            sellingPriceHigher == SellingPriceHigher.ONE_STEP_OR_MORE -> pair.first to
                    pair.second.coerceAtLeast(oneStepHigherPrice)
            else -> pair
        }
        return result
    }

    /**
     * Возвращает число миллисекунд, соответствующее времени следующего запуска Alarm Manager'а
     * и необходимость точной установки
     */
    private fun getMilliSecOfNextWork(day: TradingDayState): Pair<Long, Boolean> {

        // Следующий день, 1 час перед торгами
        fun tomorrowHourBeforeTrades(): Long {
            val newCalendar = hourBeforeStartTimeDay.clone() as Calendar
            newCalendar.add(Calendar.DAY_OF_MONTH, 1)
            return newCalendar.timeInMillis
        }

        // Следующий раз через интервал MAIN_DELAY_REQUESTS_MIN
        fun nextTime(): Long {
            val nowUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            return nowUtc.timeInMillis + mainDelayRequestsMin * 60000L
        }

        return if (stopPurchaseNow) tomorrowHourBeforeTrades() to false
        else when (day) {
            TradingDayState.MORNING_BEFORE_TRADES -> tomorrowHourBeforeTrades() to false
            TradingDayState.ONE_HOUR_BEFORE_TRADES -> {
                val newCalendar = startTimeAuctionDay.clone() as Calendar
                newCalendar.timeInMillis to true
            }
            TradingDayState.TRADING_DAY,
            TradingDayState.TRADING_DAY_START -> nextTime() to false
            TradingDayState.TRADING_DAY_ENDING,
            TradingDayState.PAUSE_BETWEEN_DAY_AND_EVENING -> {
                if (eveningTrades) {
                    val newCalendar = startTimeAuctionEvening.clone() as Calendar
                    newCalendar.timeInMillis to true
                } else tomorrowHourBeforeTrades() to false
            }
            TradingDayState.TRADING_EVENING -> {
                if (eveningTrades) nextTime() to false
                else tomorrowHourBeforeTrades() to false
            }
            TradingDayState.EVENING_AFTER_TRADES -> tomorrowHourBeforeTrades() to false
            TradingDayState.DAY_OFF -> tomorrowHourBeforeTrades() to false
            else -> nextTime() to false
        }
    }
}