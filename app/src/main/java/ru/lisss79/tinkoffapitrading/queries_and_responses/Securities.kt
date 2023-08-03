package ru.lisss79.tinkoffapitrading.queries_and_responses

import org.json.JSONObject
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.BALANCE
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.BLOCKED
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.FIGI

class Securities(val figi: String = "",
                 val blocked: Int = 0,
                 val balance: Int = 0) {

    companion object{
        fun parse(response: String): Securities {
            val json = JSONObject(response)
            return (parse(json))
        }
        fun parse(responseJson: JSONObject): Securities {
            return try {
                val figi = responseJson.getString(FIGI)
                val blocked = responseJson.getInt(BLOCKED)
                val balance = responseJson.getInt(BALANCE)
                Securities(figi, blocked, balance)
            } catch (e: Exception) {
                e.printStackTrace()
                Securities()
            }

        }
    }

}