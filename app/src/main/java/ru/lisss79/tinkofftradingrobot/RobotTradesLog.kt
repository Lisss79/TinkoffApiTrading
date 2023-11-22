package ru.lisss79.tinkofftradingrobot

import android.content.SharedPreferences
import ru.lisss79.tinkofftradingrobot.data_classes.Deal
import ru.lisss79.tinkofftradingrobot.queries_and_responses.Direction
import ru.lisss79.tinkofftradingrobot.queries_and_responses.ExecutionReportStatus
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.LAST_PURCHASE_PRICE
import ru.lisss79.tinkofftradingrobot.queries_and_responses.OperationsResponse
import ru.lisss79.tinkofftradingrobot.queries_and_responses.OrderState
import ru.lisss79.tinkofftradingrobot.queries_and_responses.OrderStateWithSource
import java.io.File
import java.io.IOException
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Класс для обработки лога торгов робота и поиска ошибок в нем
 */
class RobotTradesLog(val orders: List<OrderStateWithSource?>) {
    companion object {

        // Создание экземпляра из данных файла
        fun fromFile(file: File) =
            try {
                val orderLines = file.readLines()
                val orders = orderLines.map { OrderStateWithSource.parse(it) }
                RobotTradesLog(orders)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }

        // Создание экземпляра из списка Operation
        fun from(operations: List<OperationsResponse.Operation>?): RobotTradesLog {
            val newOrders = mutableListOf<OrderStateWithSource?>()
            if (operations == null) return RobotTradesLog(listOf())
            operations.forEach {
                newOrders.add(OrderStateWithSource.from(it).first)
                newOrders.add(OrderStateWithSource.from(it).second)
            }
            return RobotTradesLog(newOrders)
        }

        // Записать в файл данные о заявках
        fun toFile(file: File, newOrders: List<OrderStateWithSource?>?) =
            try {
                if (newOrders.isNullOrEmpty()) throw Exception("No data to write to file")
                file.writeText("")
                newOrders.forEach { orderStateWithSource ->
                    orderStateWithSource?.apply {
                        file.appendText(toJsonLog())
                        file.appendText("\n")
                    }
                }
                true
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }

    }

    /**
     * Объявляем оператор "плюс"
     */
    operator fun plus(other: RobotTradesLog): RobotTradesLog {
        val newOrders = orders + other.orders
        return RobotTradesLog(newOrders)
    }

    /**
     * Проверяет список заявок в файле на предмет пропусков состояний
     */
    fun checkForCorrectOrdersList(): List<Pair<Int, String>> {
        val errors = mutableListOf<Pair<Int, String>>()
        var index = 0
        while (index < orders.size - 1) {
            if ((orders[index]?.orderState?.executionReportStatus
                        == ExecutionReportStatus.EXECUTION_REPORT_STATUS_NEW)
                && (orders[index + 1]?.orderState?.executionReportStatus
                        == ExecutionReportStatus.EXECUTION_REPORT_STATUS_NEW)
            ) {
                errors.add(index to (orders[index]?.orderState?.orderId ?: ""))
            }
            index++
        }
        return errors
    }

    /**
     * Проверяет установлена ли корректно цена последней покупки
     * @return пара: float - корректное значение,
     * boolean - корректность сохраненного значения
     */
    fun checkForLastPurchasePrice(prefs: SharedPreferences): Pair<Float, Boolean> {
        val lastPurchasePrice = prefs.getFloat(LAST_PURCHASE_PRICE, 0f)
        val lastDeal = getLastSuccessfulDeal()
        val lastDealDirection = lastDeal.direction
        val lastDealPrice = abs(lastDeal.price)
        return when {
            (lastDealDirection == Direction.ORDER_DIRECTION_BUY)
                    && (lastDealPrice == lastPurchasePrice) -> lastDealPrice to true
            (lastDealDirection == Direction.ORDER_DIRECTION_BUY)
                    && (lastDealPrice != lastPurchasePrice) -> lastDealPrice to false
            (lastDealDirection == Direction.ORDER_DIRECTION_SELL)
                    && (lastPurchasePrice == 0f) -> 0f to true
            (lastDealDirection == Direction.ORDER_DIRECTION_SELL)
                    && (lastPurchasePrice != 0f) -> 0f to false
            else -> 0f to true
        }
    }

    private fun getLastSuccessfulDeal() =
        getDeals().last()

    /**
     * Получает список успешных сделок (результат по которым не ноль)
     */
    fun getDeals() =
        try {
            orders.map {
                it?.orderState?.run {
                    val quantity = if (initialSecurityPrice.value != 0f)
                        (initialOrderPrice.value / initialSecurityPrice.value).roundToInt()
                    else 0
                    Deal(
                        figi,
                        orderDate,
                        getFinancialResult(it.orderState),
                        initialSecurityPrice.value,
                        quantity,
                        direction
                    )
                } ?: Deal()
            }.filter { it.result != 0f }
        } catch (e: IOException) {
            listOf()
        }

    /**
     * Получает финансовый результат сделки
     * @return пара - финансовый результат / число сделок в одном направлении
     */
    private fun getFinancialResult(order: OrderState?): Float {
        if (order?.executionReportStatus?.final != true) return 0f
        var price = 0f
        val ordersWithId = orders.filter {
            it?.orderState?.orderId == order.orderId && it.orderState.figi == order.figi
        }
        ordersWithId.forEach {
            val state = it?.orderState?.executionReportStatus
                ?: ExecutionReportStatus.EXECUTION_REPORT_STATUS_UNSPECIFIED
            val tempPrice = it?.orderState?.executedOrderPrice?.value ?: 0f
            if (state == ExecutionReportStatus.EXECUTION_REPORT_STATUS_FILL) {
                price += if (tempPrice != 0f) tempPrice
                else it?.orderState?.initialOrderPrice?.value ?: 0f
            } else if (state == ExecutionReportStatus.EXECUTION_REPORT_STATUS_CANCELLED
                || state == ExecutionReportStatus.EXECUTION_REPORT_STATUS_REJECTED
            ) {
                price += tempPrice
            }
        }

        return when (order.direction) {
            Direction.ORDER_DIRECTION_BUY -> -abs(price)
            Direction.ORDER_DIRECTION_SELL -> abs(price)
            else -> 0f
        }

    }

    /**
     * Исправляет цену последней сделки
     * @param correct результат checkForLastPurchasePrice
     */
    fun correctLastPurchasePrice(prefs: SharedPreferences, correct: Pair<Float, Boolean>) {
        if (!correct.second) {
            prefs.edit().apply() {
                putFloat(LAST_PURCHASE_PRICE, correct.first)
                apply()
            }
        }
    }

    /**
     * Исправляет список заявок
     * @param errors результат checkForCorrectOrdersList
     */
    fun correctOrderList(
        api: TinkoffOpenApi,
        settingsPrefs: SharedPreferences,
        errors: List<Pair<Int, String>>
    ): List<OrderStateWithSource?> {
        val newOrders = orders.toMutableList()
        try {
            val accountId = settingsPrefs.getString("account", "") ?: ""
            errors.forEach { entry ->
                val state = api.getOrderState(accountId, entry.second).get()
                val currIndex =
                    newOrders.withIndex().first {
                        it.value?.orderState?.orderId == entry.second
                    }.index
                newOrders.add(currIndex + 1, OrderStateWithSource(state))
                Thread.sleep(700)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            newOrders.clear()
        }
        return newOrders
    }

    /**
     * Возвращает обновленный список сделок
     */
    fun updateOrderList(
        api: TinkoffOpenApi,
        settingsPrefs: SharedPreferences,
        updatedOrders: List<OrderStateWithSource?>
    ): List<OrderStateWithSource?> {
        val newOrders = orders.toMutableList()
        val updatedOrderWithIndex = updatedOrders.map { orders.indexOf(it) to it }

        try {
            val accountId = settingsPrefs.getString("account", "") ?: ""
            updatedOrderWithIndex.forEach { entry ->
                val state = api.getOrderState(
                    accountId, entry.second?.orderState?.orderId ?: ""
                ).get()
                newOrders[entry.first] = OrderStateWithSource(state)
                Thread.sleep(700)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            newOrders.clear()
        }

        return newOrders

    }

}