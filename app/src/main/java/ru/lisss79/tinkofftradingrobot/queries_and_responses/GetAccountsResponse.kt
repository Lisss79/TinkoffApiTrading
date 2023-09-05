package ru.lisss79.tinkofftradingrobot.queries_and_responses

import org.json.JSONObject
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.ACCESS_LEVEL
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.ACCOUNTS
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.ID
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.NAME
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.STATUS

class GetAccountsResponse(val accounts: List<Account>) {

    companion object{
        fun parse(response: String?): GetAccountsResponse? {
            if(response == null) return null
            return try {
                val json = JSONObject(response)
                parse(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        fun parse(responseJson: JSONObject): GetAccountsResponse {
            val list = mutableListOf<Account>()
            try {
                val jsonArray = responseJson.getJSONArray(ACCOUNTS)
                for(index in 0 until jsonArray.length()) {
                    val element = jsonArray.getJSONObject(index)
                    list.add(Account.parse(element))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return GetAccountsResponse(list)
        }
    }

    class Account(
        val id: String = "",
        val name: String = "",
        val status: String = "",
        val accessLevel: String = ""
    ) {

        companion object {
            fun parse(response: String): Account {
                val json = JSONObject(response)
                return (parse(json))
            }

            fun parse(responseJson: JSONObject): Account {
                return try {
                    val id = responseJson.getString(ID)
                    val name = responseJson.getString(NAME)
                    val status = responseJson.getString(STATUS)
                    val accessLevel = responseJson.getString(ACCESS_LEVEL)
                    Account(id, name, status, accessLevel)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Account()
                }

            }
        }
    }

}