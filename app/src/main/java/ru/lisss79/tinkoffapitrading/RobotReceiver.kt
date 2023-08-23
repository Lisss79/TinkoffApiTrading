package ru.lisss79.tinkoffapitrading

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.os.*
import androidx.preference.PreferenceManager
import ru.lisss79.tinkoffapitrading.queries_and_responses.*
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.CONFIG
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.LAST_PURCHASE_PRICE
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.ORDER
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.ORDER_ID
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.SCHEDULE_NEXT
import java.io.File
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.floor

// Токен для работы API
//"t.O-a9O0hxOuer7O696kab-gSPVXxztxytHTvsP3KEUsdlxL87C5TnOrbHp76HnLHsiGxra7FfYTMuEVlupbxM1w"
var TOKEN = ""

// Тикер рабочего инструмента
var INSTRUMENT_TICKER = ""

// Основная рабочая валюта
const val TRADING_CURRENCY = "rub"

// Идентификатор биржи для работы
const val EXCHANGE_TRADES = "MOEX_PLUS"

// Интервал времени с начала торгов, когда можно рассчитывать цену по сегодняшнему дню
var RECENT_INTERVAL_MIN = 60

// Основной интервал обращений к API
var MAIN_DELAY_REQUESTS_MIN = 30

// Допуск в объеме торгов, ниже которого можно не учитывать сделки (< 1)
// для расчета цены по последним сделкам
var RECENT_TRADES_QUANTITY_TOLERANCE = 0.05f

// Сколько денег оставлять после покупки (руб.)
var MONEY_AFTER_SPENT = 900

// Разрешить ли заменять заявки при изменении цены
var REPLACE_ORDERS_ENABLED = false

// Разрешить ли заменять заявки при изменении цены только вверх по стакану
// (в сторону скорейшей сделки)
var REPLACE_ORDERS_UP = true

// Торговать ли в вечернюю сессию
var EVENING_TRADES = false

// Допуск в объеме торгов (от максимума) для определения лучшей цены
var BEST_PRICE_QUANTITY_TOLERANCE = 0.7f

// Цена продажи выше цены покупки?
var SELLING_PRICE_HIGHER = SellingPriceHigher.defaultValue

// Шаг цены лота. Как же его сцуко получить через API??
var PRICE_STEP = 0.01f

// Приоритет определения цены
// Для торгового дня
var TRADING_DAY_PRIORITY = PricePriority.defaultValue

// Для вечерней сессии
var TRADING_EVENING_PRIORITY = PricePriority.defaultValue

// Для начала торгового дня
var START_DAY_PRIORITY = PricePriority.defaultValue

// Для аукциона открытия
var AUCTION_PRIORITY = PricePriority.defaultValue

// Для остальных случаев
var OTHER_PRIORITY = PricePriority.defaultValue

// Ключ для передачи данных из activity в broadcast receiver
const val RECEIVER = "receiver"

// Ключ для сохранения в shared preferences времени следующего аларма
const val PLAN_TIME = "plan_time"

/**
 * Класс - Broadcast Receiver, получающий сообщение от Alarm Manager'а
 */
@SuppressLint("UnspecifiedImmutableFlag")
class RobotReceiver : BroadcastReceiver() {
    private lateinit var alarmManager: AlarmManager
    private lateinit var api: TinkoffOpenApi
    private lateinit var context: Context
    private val config = TradingConfig()

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
    private val startTimeAuctionDay = getCalendarFromTime(6, 51, 1)
    private val startTimeDay = getCalendarFromTime(7, 0, 1)
    private val startTimeDayPlusInterval = (startTimeDay.clone() as Calendar).apply {
        add(Calendar.MINUTE, RECENT_INTERVAL_MIN)
    }
    private val endTimeDay = getCalendarFromTime(15, 30, 1)
    private val startTimeAuctionEvening = getCalendarFromTime(16, 1, 1)
    private val startTimeEvening = getCalendarFromTime(16, 5, 1)
    private val endTimeEvening = getCalendarFromTime(20, 50, 1)

    /**
     * Метод, вызываемый при приходе сообщения от Alarm Manager'а
     */
    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        val executorService = Executors.newCachedThreadPool()
        executorService.execute {
            this.context = context
            val powerManager = context.getSystemService(Service.POWER_SERVICE) as PowerManager
            alarmManager = context.getSystemService(Service.ALARM_SERVICE) as AlarmManager
            prefs = context.getSharedPreferences(context.packageName, MODE_PRIVATE)
            getSettingsFromPrefs()
            api = TinkoffOpenApi(TOKEN)
            val bcIntent = Intent(context, RobotReceiver::class.java)
            scheduleNext = intent.getBooleanExtra(SCHEDULE_NEXT, true)

            if (scheduleNext) {
                val pi = PendingIntent.getBroadcast(context, 0, bcIntent,
                    PendingIntent.FLAG_NO_CREATE)
                pi?.cancel()
            }

            // Начинаем лог, записываем данные о состоянии телефона,
            // получаем данные о дне и следующем запуске робота
            println("OnReceive")
            logFile = File(context.getExternalFilesDir(null), "logfile.txt")
            robotTrades = File(context.getExternalFilesDir(null), "robot.txt")
            logFile.appendText("${Instant.now()} starting robot\n")
            if (powerManager.isDeviceIdleMode) logFile.appendText("Device is in idle mode now\n")
            if (!powerManager.isIgnoringBatteryOptimizations(context.packageName))
                logFile.appendText("Battery optimization turned ON!\n")

            dayInfo = getTradingDayState()
            logFile.appendText("Trading Day State: $dayInfo\n")
            val (alarmTime, exact) = getMilliSecOfNextWork(dayInfo)


            // Если произошла ошибка чтения данных, просто перепланировать робота
            if (dayInfo == TradingDayState.ERROR) {
                logFile.appendText("Stop working due to an error\n")
                println("Stop working due to an error")
                planNextAlarm(alarmTime, exact, context, intent)
                return@execute
            }

            // Получить основные данные и прекратить работу, если не удалось
            if (!getTradingData()) {
                planNextAlarm(alarmTime, exact, context, intent)
                return@execute
            }

            // Проверить состояние последней заявки и прочитать последнюю цену по инструменту
            checkForLastOrder()
            config.closePrice = getClosePrice()

            // Если торги на бирже закрыты, просто перепланировать робота
            if (!dayInfo.isTradingAvailable) {
                logFile.appendText("Now is not trading time\n")
                println("Not trading day")
                planNextAlarm(alarmTime, exact, context, intent)
                return@execute
            }

            // Если вечер и торги вечером запрещены
            else if (dayInfo.isEvening && !EVENING_TRADES) {
                logFile.appendText("Evening trades are not allowed\n")
                println("Evening trades are not allowed")
                planNextAlarm(alarmTime, exact, context, intent)
                return@execute
            }

            // Если торги по инструменту закрыты, просто перепланировать робота
            else if (!config.tradesAvailable) {
                logFile.appendText("Trades of the instrument is not available\n")
                println("Trades of the instrument is not available")
                planNextAlarm(alarmTime, exact, context, intent)
                return@execute
            }

            // Если торги доступны
            else {
                // Получаем и сохраняем цены, выбранные по заданному критерию
                val (bidPrice, askPrice) = getPrices()
                config.selectedPurchasePrice = bidPrice
                config.selectedSellingPrice = askPrice
                getActiveOrders()
                if (scheduleNext) order = robotCycle(bidPrice, askPrice)
                logFile.appendText("Trading data:\n")
                logFile.appendText(config.toString() + "\n\n")
                if (order.instrumentId.isNotEmpty()) {
                    logFile.appendText("Order created: $order \n")
                } else if (order.direction == Direction.ORDER_DIRECTION_UNSPECIFIED) {
                    logFile.appendText("No need to create new order\n")
                } else {
                    logFile.appendText("Can't create order in direction ${order.direction.rus_name}\n")
                }
                logFile.appendText("${Instant.now()} ending robot\n\n")
            }

            // Установить Alarm на новое время
            planNextAlarm(alarmTime, exact, context, intent)
        }

    }

    private fun getSettingsFromPrefs() {
        settingsPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        TOKEN = settingsPrefs.getString(context.getString(R.string.TOKEN), "") ?: ""
        INSTRUMENT_TICKER = settingsPrefs.getString(context.getString(R.string.TICKER), "") ?: ""
        SELLING_PRICE_HIGHER = SellingPriceHigher.valueOf(
            settingsPrefs.getString(
                context.getString(R.string.selling_price_higher),
                SellingPriceHigher.defaultValue.name
            ) ?: SellingPriceHigher.defaultValue.name
        )
        TRADING_DAY_PRIORITY = PricePriority.valueOf(
            settingsPrefs.getString(
                context.getString(R.string.trading_day_priority),
                PricePriority.defaultValue.name
            ) ?: PricePriority.defaultValue.name
        )
        TRADING_EVENING_PRIORITY = PricePriority.valueOf(
            settingsPrefs.getString(
                context.getString(R.string.trading_evening_priority),
                PricePriority.defaultValue.name
            ) ?: PricePriority.defaultValue.name
        )
        START_DAY_PRIORITY = PricePriority.valueOf(
            settingsPrefs.getString(
                context.getString(R.string.start_day_priority),
                PricePriority.defaultValue.name
            ) ?: PricePriority.defaultValue.name
        )
        AUCTION_PRIORITY = PricePriority.valueOf(
            settingsPrefs.getString(context.getString(R.string.auction_priority),
                PricePriority.defaultValue.name) ?: PricePriority.defaultValue.name)
        OTHER_PRIORITY = PricePriority.valueOf(
            settingsPrefs.getString(context.getString(R.string.other_priority),
                PricePriority.defaultValue.name) ?: PricePriority.defaultValue.name)
        BEST_PRICE_QUANTITY_TOLERANCE = settingsPrefs
            .getFloat(context.getString(R.string.best_price_quantity_tolerance),
                BEST_PRICE_QUANTITY_TOLERANCE)
        REPLACE_ORDERS_ENABLED = settingsPrefs.getBoolean(context
            .getString(R.string.replace_order_enabled), false)
        REPLACE_ORDERS_UP = settingsPrefs.getBoolean(context
            .getString(R.string.replace_order_up), false)
        EVENING_TRADES = settingsPrefs.getBoolean(context
            .getString(R.string.evening_trades), false)
        RECENT_TRADES_QUANTITY_TOLERANCE = settingsPrefs
            .getFloat(context.getString(R.string.recent_trades_quantity_tolerance),
                RECENT_TRADES_QUANTITY_TOLERANCE)
        MONEY_AFTER_SPENT = settingsPrefs
            .getString(context.getString(R.string.money_after_spent),
                MONEY_AFTER_SPENT.toString())?.toInt() ?: MONEY_AFTER_SPENT
        MAIN_DELAY_REQUESTS_MIN = settingsPrefs
            .getString(context.getString(R.string.main_request_delay_min),
                MAIN_DELAY_REQUESTS_MIN.toString())?.toInt() ?: MAIN_DELAY_REQUESTS_MIN
        RECENT_INTERVAL_MIN = settingsPrefs
            .getString(context.getString(R.string.recent_interval_min),
                RECENT_INTERVAL_MIN.toString())?.toInt() ?: RECENT_INTERVAL_MIN

    }

    /**
     * Запланировать следующий Alarm и оповестить вызывающий Intent
     */
    @Suppress("DEPRECATION")
    private fun planNextAlarm(alarmTime: Long, exact: Boolean, context: Context, intent: Intent) {

        // Запланировать Alarm, если нужно
        if (scheduleNext) {
            val bcIntent = Intent(context, RobotReceiver::class.java)
            val pi = PendingIntent.getBroadcast(context,0, bcIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)
            if (!exact) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pi)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pi)
            }
            println("New alarm set")
            logFile.appendText(
                "New ${if (exact) "EXACT" else "inexact"} " +
                        "alarm set at ${Date(alarmTime).toInstant()}.\n\n"
            )
            val editor = prefs.edit()
            editor.putLong(PLAN_TIME, alarmTime)
            editor.apply()
        }

        val resultReceiver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(RECEIVER, ResultReceiver::class.java)
        } else {
            intent.getParcelableExtra(RECEIVER)
        }
        resultReceiver?.apply {
            val bundle = Bundle()
            bundle.putSerializable(CONFIG, config)
            bundle.putSerializable(ORDER, order)
            resultReceiver.send(0, bundle)
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
            return PostOrder()
        }

        return when {
            // Есть активные заявки, проверяем, надо ли менять
            config.activeOrders > 0 -> {
                var postOrder = PostOrder()
                println("Заявка уже выставлена")
                if (config.activeOrderDirection == Direction.ORDER_DIRECTION_BUY) {

                    // Условие замены заявки на покупку
                    val condition = ((config.activeOrdersPrice < bidPrice)
                            && REPLACE_ORDERS_UP)
                            || ((config.activeOrdersPrice != bidPrice)
                            && !REPLACE_ORDERS_UP)
                    if (condition) {
                        println("Надо заявку менять!")
                        logFile.appendText("The order will be changed!\n")
                        if (REPLACE_ORDERS_ENABLED) {
                            val quantity = config.activeOrdersQuantity.toString()
                            val price = Price.parse(bidPrice)
                            val accountId = config.accountId
                            val orderId = config.activeOrderId
                            val key = UUID.randomUUID().toString()
                            val replaceOrder =
                                ReplaceOrder(quantity, price, accountId, orderId, key)
                            val replaceOrderResponse = api.replaceOrder(replaceOrder).get()
                            replaceOrderResponse?.apply {
                                logFile.appendText("Order is changed\n")
                                checkForLastOrder()
                                robotTrades.appendText(toJsonLog())
                                robotTrades.appendText("\n")
                                val editor = prefs.edit()
                                editor.putString(ORDER_ID, replaceOrderResponse.orderId)
                                editor.apply()
                                postOrder = PostOrder(
                                    quantity, price, config.activeOrderDirection,
                                    accountId, OrderType.ORDER_TYPE_LIMIT, config.instrumentId
                                )
                            }
                        } else {
                            println("Но замена заявок запрещена.")
                        }

                    }
                } else if (config.activeOrderDirection == Direction.ORDER_DIRECTION_SELL) {

                    // Условие замены заявки на продажу
                    val condition = ((config.activeOrdersPrice > askPrice)
                            && REPLACE_ORDERS_UP)
                            || ((config.activeOrdersPrice != askPrice)
                            && !REPLACE_ORDERS_UP)
                    if (condition) {
                        println("Надо заявку менять!")
                        robotTrades.appendText("The order will be changed!\n")
                        if (REPLACE_ORDERS_ENABLED) {
                            val quantity = config.activeOrdersQuantity.toString()
                            val price = Price.parse(askPrice)
                            val accountId = config.accountId
                            val orderId = config.activeOrderId
                            val key = UUID.randomUUID().toString()
                            val replaceOrder =
                                ReplaceOrder(quantity, price, accountId, orderId, key)
                            val replaceOrderResponse = api.replaceOrder(replaceOrder).get()
                            replaceOrderResponse?.apply {
                                logFile.appendText("Order is changed\n")
                                checkForLastOrder()
                                robotTrades.appendText(toJsonLog())
                                robotTrades.appendText("\n")
                                val editor = prefs.edit()
                                editor.putString(ORDER_ID, replaceOrderResponse.orderId)
                                editor.apply()
                                postOrder = PostOrder(
                                    quantity, price, config.activeOrderDirection,
                                    accountId, OrderType.ORDER_TYPE_LIMIT, config.instrumentId
                                )
                            }
                        } else {
                            println("Но замена заявок запрещена.")
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
                        robotTrades.appendText(toJsonLog())
                        robotTrades.appendText("\n")
                        val editor = prefs.edit()
                        editor.putString(ORDER_ID, orderId)
                        editor.apply()
                    }
                    postOrder
                } else {
                    println("Невозможно создать заявку: не определена цена")
                    PostOrder().apply {
                        direction = Direction.ORDER_DIRECTION_SELL
                    }
                }
            }

            // Нет бумаг на счету, выставляем заявку на покупку
            config.positionQuantity == 0 -> {
                if (bidPrice > 0) {

                    val quantity =
                        floor((config.currencyQuantity - MONEY_AFTER_SPENT) / bidPrice).toString()
                    val price = Price.parse(bidPrice)
                    val direction = Direction.ORDER_DIRECTION_BUY
                    val accountId = config.accountId
                    val orderType = OrderType.ORDER_TYPE_LIMIT
                    val instrumentId = config.instrumentId
                    val postOrder =
                        PostOrder(quantity, price, direction, accountId, orderType, instrumentId)
                    println("Создаем заявку:\n$postOrder")
                    val postOrderResponse = api.postOrder(postOrder).get()
                    postOrderResponse?.apply {
                        robotTrades.appendText(toJsonLog())
                        robotTrades.appendText("\n")
                        val editor = prefs.edit()
                        editor.putString(ORDER_ID, orderId)
                        editor.apply()
                    }

                    postOrder
                } else {
                    println("Невозможно создать заявку: не определена цена")
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
            val lastOrder = api.getOrderState(config.accountId, lastOrderId).get()
            when (lastOrder?.executionReportStatus) {
                // Заявка отменена отклонена
                ExecutionReportStatus.EXECUTION_REPORT_STATUS_CANCELLED,
                ExecutionReportStatus.EXECUTION_REPORT_STATUS_REJECTED -> {
                    lastOrder.orderDate = Instant.now()
                    robotTrades.appendText(lastOrder.toJsonLog())
                    robotTrades.appendText("\n")
                    val editor = prefs.edit()
                    editor.putString(ORDER_ID, "")
                    editor.apply()
                }
                // Заявка успешно выполнена
                ExecutionReportStatus.EXECUTION_REPORT_STATUS_FILL -> {
                    lastOrder.orderDate = Instant.now()
                    robotTrades.appendText(lastOrder.toJsonLog())
                    robotTrades.appendText("\n")
                    val editor = prefs.edit()
                    editor.putString(ORDER_ID, "")

                    // Цена последней покупки (0 - если была продажа)
                    lastPurchasePrice = if (lastOrder.direction == Direction.ORDER_DIRECTION_BUY)
                        lastOrder.initialSecurityPrice.value
                    else 0f
                    editor.putFloat(LAST_PURCHASE_PRICE, lastPurchasePrice)
                    editor.apply()
                }

                else -> {}
            }
        }
        config.lastPurchasePrice = lastPurchasePrice
    }

    /**
     * Возвращает состояние торгового дня в текущий момент
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

        return when {
            nowUtc.before(hourBeforeStartTimeDay) -> TradingDayState.MORNING_BEFORE_TRADES
            nowUtc.before(startTimeAuctionDay) -> TradingDayState.ONE_HOUR_BEFORE_TRADES
            nowUtc.before(startTimeDay) -> TradingDayState.OPENING_AUCTION_DAY
            nowUtc.before(startTimeDayPlusInterval) -> TradingDayState.TRADING_DAY_START
            nowUtc.before(endTimeDay) -> TradingDayState.TRADING_DAY
            nowUtc.before(startTimeAuctionEvening) -> TradingDayState.PAUSE_BETWEEN_DAY_AND_EVENING
            nowUtc.before(startTimeEvening) -> TradingDayState.OPENING_AUCTION_EVENING
            nowUtc.before(endTimeEvening) -> TradingDayState.TRADING_EVENING
            nowUtc.after(endTimeEvening) -> TradingDayState.EVENING_AFTER_TRADES
            else -> TradingDayState.DAY_OFF
        }
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

            val instruments = api.findInstruments(INSTRUMENT_TICKER).get()
                ?: throw Exception("Can't find the instrument")
            config.instrumentId = instruments.instruments[0].uid
            config.figi = instruments.instruments[0].figi

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
            logFile.appendText("Trading data got successfully\n")

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
            val from = to.minusSeconds(60L * RECENT_INTERVAL_MIN)

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
                    it.value >= maxQuantity * RECENT_TRADES_QUANTITY_TOLERANCE
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
                orderBook.getBidPriceWithQuantityTolerance(BEST_PRICE_QUANTITY_TOLERANCE)
            config.askPriceWithQuantityTolerance =
                orderBook.getAskPriceWithQuantityTolerance(BEST_PRICE_QUANTITY_TOLERANCE)
        }

        getRecentPrices()
        getPricesFromOrderBook()
        config.closePrice = getClosePrice()

        var result = when (dayInfo) {
            TradingDayState.TRADING_DAY ->
                getPairPrices(TRADING_DAY_PRIORITY)
            TradingDayState.TRADING_EVENING ->
                getPairPrices(TRADING_EVENING_PRIORITY)
            TradingDayState.OPENING_AUCTION_DAY, TradingDayState.OPENING_AUCTION_EVENING ->
                getPairPrices(AUCTION_PRIORITY)
            TradingDayState.TRADING_DAY_START ->
                getPairPrices(START_DAY_PRIORITY)
            else ->
                getPairPrices(OTHER_PRIORITY)
        }
        if (result == Pair(0f, 0f)) result = getPairPrices(PricePriority.PRIORITY_CLOSE_PRICE)
        return result
    }

    private fun getClosePrice(): Float {
        val closePrice = api.getClosePrices(config.instrumentId).get()
        return closePrice?.closePrice?.value ?: 0f
    }

    /**
     * Возвращает пару цены покупка/продажа исходя из заданного приоритета
     */
    private fun getPairPrices(priority: PricePriority): Pair<Float, Float> {
        val pair = when (priority) {
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
        val result = when {
            SELLING_PRICE_HIGHER == SellingPriceHigher.EXACTLY_ONE_STEP &&
                    config.lastPurchasePrice > 0f -> pair.first to config.lastPurchasePrice + PRICE_STEP
            SELLING_PRICE_HIGHER == SellingPriceHigher.ONE_STEP_OR_MORE -> pair.first to
                    pair.second.coerceAtLeast(config.lastPurchasePrice + PRICE_STEP)
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
            return nowUtc.timeInMillis + MAIN_DELAY_REQUESTS_MIN * 60000L
        }

        return when (day) {
            TradingDayState.TRADING_DAY,
            TradingDayState.TRADING_DAY_START -> nextTime() to false
            TradingDayState.TRADING_EVENING -> {
                if (EVENING_TRADES) nextTime() to false
                else tomorrowHourBeforeTrades() to false
            }
            TradingDayState.PAUSE_BETWEEN_DAY_AND_EVENING -> {
                if (EVENING_TRADES) {
                    val newCalendar = startTimeAuctionEvening.clone() as Calendar
                    newCalendar.timeInMillis to true
                } else tomorrowHourBeforeTrades() to false
            }
            TradingDayState.MORNING_BEFORE_TRADES -> tomorrowHourBeforeTrades() to false
            TradingDayState.ONE_HOUR_BEFORE_TRADES -> {
                val newCalendar = startTimeAuctionDay.clone() as Calendar
                newCalendar.timeInMillis to true
            }
            TradingDayState.EVENING_AFTER_TRADES -> tomorrowHourBeforeTrades() to false
            TradingDayState.DAY_OFF -> tomorrowHourBeforeTrades() to false
            else -> nextTime() to false
        }
    }
}