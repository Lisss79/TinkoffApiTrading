package ru.lisss79.tinkoffapitrading.queries_and_responses

import org.json.JSONObject
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.DIRECTION
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.FIGI
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.INITIAL_SECURITY_PRICE
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.LOTS_REQUESTED
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.ORDERS
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.ORDER_ID

class GetOrdersResponse(val orders: List<Order>) {

    companion object{
        fun parse(response: String?): GetOrdersResponse? {
            if(response == null) return null
            return try {
                val json = JSONObject(response)
                parse(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        fun parse(responseJson: JSONObject): GetOrdersResponse {
            val list = mutableListOf<Order>()
            try {
                val jsonArray = responseJson.getJSONArray(ORDERS)
                for(index in 0 until jsonArray.length()) {
                    val element = jsonArray.getJSONObject(index)
                    list.add(Order.parse(element))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return GetOrdersResponse(list)
        }
    }

    class Order(val orderId: String = "",
                val figi: String = "",
                val lotsRequested: Int = 0,
                val direction: Direction = Direction.ORDER_DIRECTION_UNSPECIFIED,
                val initialSecurityPrice: Money = Money.ZERO) {

        companion object{
            fun parse(response: String): Order {
                val json = JSONObject(response)
                return (parse(json))
            }
            fun parse(responseJson: JSONObject): Order {
                return try {
                    val orderId = responseJson.getString(ORDER_ID)
                    val figi = responseJson.getString(FIGI)
                    val lotsRequested = responseJson.getString(LOTS_REQUESTED).toInt()
                    val direction = Direction.parse(responseJson.getString(DIRECTION))
                    val initialSecurityPrice = Money.parse(responseJson.getJSONObject(INITIAL_SECURITY_PRICE))
                    Order(orderId, figi, lotsRequested, direction, initialSecurityPrice)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Order()
                }

            }
        }
    }

}