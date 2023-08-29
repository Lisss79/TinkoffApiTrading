package ru.lisss79.tinkofftradingrobot.queries_and_responses

import org.json.JSONObject
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.MONEY
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.SECURITIES

class GetPositionsResponse(val moneys: List<Money>, val securities: List<Securities>) {

    companion object{
        fun parse(response: String?): GetPositionsResponse? {
            if(response == null) return null
            return try {
                val json = JSONObject(response)
                parse(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        fun parse(responseJson: JSONObject): GetPositionsResponse {
            val listM = mutableListOf<Money>()
            val listS = mutableListOf<Securities>()
            try {
                val jsonArrayM = responseJson.getJSONArray(MONEY)
                val jsonArrayS = responseJson.getJSONArray(SECURITIES)
                for(index in 0 until jsonArrayM.length()) {
                    val element = jsonArrayM.getJSONObject(index)
                    listM.add(Money.parse(element))
                }
                for(index in 0 until jsonArrayS.length()) {
                    val element = jsonArrayS.getJSONObject(index)
                    listS.add(Securities.parse(element))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return GetPositionsResponse(listM, listS)

        }
    }

}