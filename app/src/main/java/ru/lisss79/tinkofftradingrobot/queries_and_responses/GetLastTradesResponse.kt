package ru.lisss79.tinkofftradingrobot.queries_and_responses

import org.json.JSONObject
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.PRICE
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.QUANTITY
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.TIME
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.TRADES
import java.time.Instant

class GetLastTradesResponse(val trades: List<Trade>) {

    companion object{
        fun parse(response: String?): GetLastTradesResponse? {
            if(response == null) return null
            return try {
                val json = JSONObject(response)
                parse(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        fun parse(responseJson: JSONObject): GetLastTradesResponse {
            val list = mutableListOf<Trade>()
            try {
                val jsonArray = responseJson.getJSONArray(TRADES)
                for(index in 0 until jsonArray.length()) {
                    val element = jsonArray.getJSONObject(index)
                    list.add(Trade.parse(element))
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
            return GetLastTradesResponse(list)
        }
    }

    class Trade(val price: Price = Price.ZERO,
                val quantity: Int = 0,
                val time: Instant = Instant.now()) {

        companion object{
            fun parse(response: String): Trade {
                val json = JSONObject(response)
                return (parse(json))
            }
            fun parse(responseJson: JSONObject): Trade {
                return try {
                    val price = Price.parse(responseJson.getJSONObject(PRICE))
                    val quantity = responseJson.getInt(QUANTITY)
                    val time = Instant.parse(responseJson.getString(TIME))
                    Trade(price, quantity, time)
                }
                catch (e: Exception) {
                    e.printStackTrace()
                    Trade()
                }

            }
        }
    }

}