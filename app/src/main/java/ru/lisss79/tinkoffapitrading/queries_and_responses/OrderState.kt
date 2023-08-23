package ru.lisss79.tinkoffapitrading.queries_and_responses

import org.json.JSONObject
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.DIRECTION
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.EXECUTION_REPORT_STATUS
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.FIGI
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.INITIAL_ORDER_PRICE
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.INITIAL_SECURITY_PRICE
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.ORDER_DATE
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.ORDER_ID
import java.time.Instant

class OrderState(val orderId: String = "",
                 val figi: String = "",
                 val executionReportStatus: ExecutionReportStatus =
                     ExecutionReportStatus.EXECUTION_REPORT_STATUS_UNSPECIFIED,
                 // Общая цена заявки
                 val initialOrderPrice: Money = Money.ZERO,
                 val direction: Direction = Direction.ORDER_DIRECTION_UNSPECIFIED,
                 // Цена заявки одного лота
                 val initialSecurityPrice: Money = Money.ZERO,
                 var orderDate: Instant = Instant.now()) {

    companion object{
        fun parse(response: String?): OrderState? {
            if(response == null) return null
            return try {
                val json = JSONObject(response)
                parse(json)
            } catch (e: java.lang.Exception) {
                println("Not correct response to parse: $response")
                e.printStackTrace()
                null
            }
        }
        fun parse(responseJson: JSONObject): OrderState {
            return try {
                val orderId = responseJson.getString(ORDER_ID)
                val figi = responseJson.getString(FIGI)
                val executionReportStatus =
                    ExecutionReportStatus.parse(responseJson.getString(EXECUTION_REPORT_STATUS))
                val initialOrderPrice =
                    Money.parse(responseJson.getString(INITIAL_ORDER_PRICE))
                val direction = Direction.parse(responseJson.getString(DIRECTION))
                val initialSecurityPrice =
                    Money.parse(responseJson.getString(INITIAL_SECURITY_PRICE))
                val orderDate =
                    Instant.parse(responseJson.getString(ORDER_DATE))
                OrderState(orderId, figi, executionReportStatus,
                    initialOrderPrice, direction, initialSecurityPrice, orderDate)
            } catch (e: Exception) {
                e.printStackTrace()
                OrderState()
            }
        }
    }

    fun toJsonLog(): String {
        val jsonString = JSONObject()
        jsonString.put(ORDER_ID, orderId)
        jsonString.put(FIGI, figi)
        jsonString.put(EXECUTION_REPORT_STATUS, executionReportStatus.name)
        jsonString.put(DIRECTION, direction.name)
        jsonString.put(INITIAL_ORDER_PRICE, initialOrderPrice.value)
        jsonString.put(INITIAL_SECURITY_PRICE, initialSecurityPrice.value)
        jsonString.put(ORDER_DATE, orderDate.toString())
        return jsonString.toString()
    }


}