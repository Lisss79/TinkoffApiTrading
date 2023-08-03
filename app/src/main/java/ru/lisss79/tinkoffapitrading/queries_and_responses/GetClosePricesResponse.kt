package ru.lisss79.tinkoffapitrading.queries_and_responses

import org.json.JSONObject
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.CLOSE_PRICES
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.PRICE

class GetClosePricesResponse(val closePrice: Price = Price.ZERO) {

    companion object{
        fun parse(response: String?): GetClosePricesResponse? {
            if(response == null) return null
            return try {
                val json = JSONObject(response)
                parse(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        fun parse(responseJson: JSONObject): GetClosePricesResponse {
            return try {
                val jsonArray = responseJson.getJSONArray(CLOSE_PRICES)
                val element = jsonArray.getJSONObject(0).getString(PRICE)
                val closePrice = Price.parse(element)
                GetClosePricesResponse(closePrice)
            } catch (e: Exception) {
                e.printStackTrace()
                GetClosePricesResponse()
            }

        }
    }
}