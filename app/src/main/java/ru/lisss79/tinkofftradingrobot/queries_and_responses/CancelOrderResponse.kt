package ru.lisss79.tinkofftradingrobot.queries_and_responses

import org.json.JSONObject
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.TIME

class CancelOrderResponse(val time: String) {

    companion object{
        fun parse(response: String?): CancelOrderResponse? {
            if(response == null) return null
            return try {
                val json = JSONObject(response)
                parse(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        fun parse(responseJson: JSONObject): CancelOrderResponse {
            var cancelTime = ""
            try {
                cancelTime = responseJson.getString(TIME)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return CancelOrderResponse(cancelTime)
        }
    }

}