package ru.lisss79.tinkofftradingrobot.queries_and_responses

import org.json.JSONObject
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.DIRECTION
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.EXECUTED_ORDER_PRICE
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.EXECUTION_REPORT_STATUS
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.FIGI
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.INITIAL_ORDER_PRICE
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.INITIAL_SECURITY_PRICE
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.LOADED_FROM_SERVER
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.ORDER_DATE
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.ORDER_ID

data class OrderStateWithSource(
    val orderState: OrderState?,
    val loadedFromServer: Boolean = false
) {

    companion object {
        fun parse(response: String?): OrderStateWithSource? {
            if (response == null) return null
            return try {
                val json = JSONObject(response)
                parse(json)
            } catch (e: Exception) {
                println("Not correct response to parse: $response")
                e.printStackTrace()
                null
            }
        }

        fun parse(responseJson: JSONObject): OrderStateWithSource {
            return try {
                val orderState = OrderState.parse(responseJson.toString())
                val loadedFromServer = if (responseJson.has(LOADED_FROM_SERVER))
                    responseJson.getBoolean(LOADED_FROM_SERVER)
                else false
                OrderStateWithSource(orderState, loadedFromServer)
            } catch (e: Exception) {
                e.printStackTrace()
                OrderStateWithSource(null)
            }
        }

        /**
         * Получаем пару OrderStateWithSource из Operation
         * @return пара (первое значение - выставленная заявка, второе - выполненная)
         */
        fun from(operation: OperationsResponse.Operation):
                Pair<OrderStateWithSource, OrderStateWithSource> {
            return OrderStateWithSource(
                OrderState(
                    operation.id,
                    operation.figi,
                    ExecutionReportStatus.EXECUTION_REPORT_STATUS_NEW,
                    operation.payment,
                    Direction.from(operation.operationType),
                    operation.price,
                    Money.ZERO,
                    operation.date
                ), true
            ) to OrderStateWithSource(
                OrderState(
                    operation.id,
                    operation.figi,
                    ExecutionReportStatus.EXECUTION_REPORT_STATUS_FILL,
                    operation.payment,
                    Direction.from(operation.operationType),
                    operation.price,
                    operation.payment,
                    operation.getLastTradesInstant()
                ), true
            )
        }
    }

    fun toJsonLog(): String {
        return JSONObject().apply {
            put(ORDER_ID, orderState?.orderId)
            put(FIGI, orderState?.figi)
            put(EXECUTION_REPORT_STATUS, orderState?.executionReportStatus?.name)
            put(DIRECTION, orderState?.direction?.name)
            put(INITIAL_ORDER_PRICE, orderState?.initialOrderPrice?.value)
            put(INITIAL_SECURITY_PRICE, orderState?.initialSecurityPrice?.value)
            put(EXECUTED_ORDER_PRICE, orderState?.executedOrderPrice?.value)
            put(ORDER_DATE, orderState?.orderDate.toString())
            put(LOADED_FROM_SERVER, loadedFromServer)
        }.toString()
    }

}
