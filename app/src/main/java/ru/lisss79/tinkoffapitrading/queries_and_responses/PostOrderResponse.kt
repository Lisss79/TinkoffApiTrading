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

class PostOrderResponse(val orderId: String = "",
                        val figi: String = "",
                        val executionReportStatus: ExecutionReportStatus =
                            ExecutionReportStatus.EXECUTION_REPORT_STATUS_UNSPECIFIED,
                        val initialOrderPrice: Money = Money.ZERO,
                        val direction: Direction = Direction.ORDER_DIRECTION_UNSPECIFIED,
                        val initialSecurityPrice: Money = Money.ZERO) {

    companion object{
        fun parse(response: String?): PostOrderResponse? {
            if(response == null) return null
            return try {
                val json = JSONObject(response)
                parse(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        fun parse(responseJson: JSONObject): PostOrderResponse {
            return try {
                val orderId = responseJson.getString(ORDER_ID)
                val figi = responseJson.getString(FIGI)
                val executionReportStatus =
                    ExecutionReportStatus.parse(responseJson.getString(EXECUTION_REPORT_STATUS))
                val initialOrderPrice = Money.parse(responseJson.getString(INITIAL_ORDER_PRICE))
                val direction = Direction.parse(responseJson.getString(DIRECTION))
                val initialSecurityPrice = Money.parse(responseJson.getString(INITIAL_SECURITY_PRICE))
                PostOrderResponse(orderId, figi, executionReportStatus,
                    initialOrderPrice, direction, initialSecurityPrice)
            } catch (e: Exception) {
                e.printStackTrace()
                PostOrderResponse()
            }

        }
    }

    override fun toString(): String {
        return "Id заявки: '$orderId', статус: ${executionReportStatus.rus_name},\n" +
                "Общая цена: ${initialOrderPrice.value}, направление: ${direction.rus_name},\n" +
                "Цена лота: ${initialSecurityPrice.value}\n"
    }

    fun toJsonLog(): String {
        val jsonString = JSONObject()
        jsonString.put(ORDER_ID, orderId)
        jsonString.put(FIGI, figi)
        jsonString.put(EXECUTION_REPORT_STATUS, executionReportStatus.name)
        jsonString.put(DIRECTION, direction.name)
        jsonString.put(INITIAL_ORDER_PRICE, initialOrderPrice.value)
        jsonString.put(INITIAL_SECURITY_PRICE, initialSecurityPrice.value)
        jsonString.put(ORDER_DATE, Instant.now().toString())
        return jsonString.toString()
    }

}