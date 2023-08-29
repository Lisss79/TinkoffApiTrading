package ru.lisss79.tinkoffapitrading.queries_and_responses

import org.json.JSONObject
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.INSTRUMENT
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.MIN_PRICE_INCREMENT
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.UID

class EtfResponse(val instrumentId: String = "", val minPriceIncrement: Price = Price.ZERO) {

    companion object {
        fun parse(response: String?): EtfResponse? {
            if (response == null) return null
            return try {
                val json = JSONObject(response)
                parse(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun parse(responseJson: JSONObject): EtfResponse {
            return try {
                val json = responseJson.getJSONObject(INSTRUMENT)
                val instrumentId = json.getString(UID)
                val minPriceIncrement = Price.parse(json.getString(MIN_PRICE_INCREMENT))
                EtfResponse(instrumentId, minPriceIncrement)
            } catch (e: Exception) {
                e.printStackTrace()
                EtfResponse()
            }
        }
    }

}