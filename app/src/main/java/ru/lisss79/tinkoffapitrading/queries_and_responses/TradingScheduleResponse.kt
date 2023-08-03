package ru.lisss79.tinkoffapitrading.queries_and_responses

import org.json.JSONObject
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.DATE
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.DAYS
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.END_TIME
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.EXCHANGE
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.EXCHANGES
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.IS_TRADING_DAY
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.START_TIME
import java.time.Instant

class TradingSchedulesResponse(val exchanges: List<Exchange>) {

    companion object{
        fun parse(response: String): TradingSchedulesResponse {
            val json = JSONObject(response)
            return (parse(json))
        }
        fun parse(responseJson: JSONObject): TradingSchedulesResponse {
            val list = mutableListOf<Exchange>()
            try {
                val jsonArray = responseJson.getJSONArray(EXCHANGES)
                for(index in 0 until jsonArray.length()) {
                    val element = jsonArray.getJSONObject(index)
                    list.add(Exchange.parse(element))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return TradingSchedulesResponse(list)
        }
    }

    class Exchange(val exchange: String = "",
                   val days: List<Day>) {

        companion object{
            fun parse(response: String): Exchange {
                val json = JSONObject(response)
                return (parse(json))
            }
            fun parse(responseJson: JSONObject): Exchange {
                val list = mutableListOf<Day>()
                return try {
                    val exchange = responseJson.getString(EXCHANGE)
                    val jsonArray = responseJson.getJSONArray(DAYS)
                    for(index in 0 until jsonArray.length()) {
                        val element = jsonArray.getJSONObject(index)
                        list.add(Day.parse(element))
                    }
                    Exchange(exchange, list)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Exchange(days = list)
                }
            }
        }

        class Day(val date: Instant = Instant.now(),
                  val isTradingDay: Boolean = false,
                  val startTime: Instant = Instant.now(),
                  val endTime: Instant = Instant.now()) {

            companion object{
                fun parse(response: String): Day {
                    val json = JSONObject(response)
                    return (parse(json))
                }
                fun parse(responseJson: JSONObject): Day {
                    return try {
                        val date = Instant.parse(responseJson.getString(DATE))
                        val isTradingDay = responseJson.getBoolean(IS_TRADING_DAY)
                        var startTime = Instant.now()
                        var endTime = Instant.now()
                        if(isTradingDay) {
                            startTime = Instant.parse(responseJson.getString(START_TIME))
                            endTime = Instant.parse(responseJson.getString(END_TIME))
                        }
                        Day(date, isTradingDay, startTime, endTime)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Day()
                    }
                }
            }
        }
    }
}