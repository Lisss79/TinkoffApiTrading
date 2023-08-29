package ru.lisss79.tinkofftradingrobot.queries_and_responses

import org.json.JSONObject
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.FIGI
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.INSTRUMENTS
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.NAME
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.UID

class FindInstrumentsResponse(val instruments: List<Instrument>) {

    companion object{
        fun parse(response: String?): FindInstrumentsResponse? {
            if(response == null) return null
            return try {
                val json = JSONObject(response)
                parse(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        fun parse(responseJson: JSONObject): FindInstrumentsResponse {
            val list = mutableListOf<Instrument>()
            try {
                val jsonArray = responseJson.getJSONArray(INSTRUMENTS)
                for(index in 0 until jsonArray.length()) {
                    val element = jsonArray.getJSONObject(index)
                    list.add(Instrument.parse(element))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return FindInstrumentsResponse(list)
        }
    }

    class Instrument(val uid: String = "",
                     val figi: String = "",
                     val name: String= "") {

        companion object{
            fun parse(response: String): Instrument {
                val json = JSONObject(response)
                return (parse(json))
            }
            fun parse(responseJson: JSONObject): Instrument {
                return try {
                    val uid = responseJson.getString(UID)
                    val figi = responseJson.getString(FIGI)
                    val name = responseJson.getString(NAME)
                    Instrument(uid, figi, name)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Instrument()
                }

            }
        }

    }

}