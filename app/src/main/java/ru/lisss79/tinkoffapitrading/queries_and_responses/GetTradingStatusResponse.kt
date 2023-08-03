package ru.lisss79.tinkoffapitrading.queries_and_responses

import org.json.JSONObject
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.API_TRADE_AVAILABLE_FLAG
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.LIMIT_ORDER_AVAILABLE_FLAG
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.TRADING_STATUS

class GetTradingStatusResponse(val tradingStatus: TradingStatus =
                                   TradingStatus.SECURITY_TRADING_STATUS_UNSPECIFIED,
                               val limitOrderAvailableFlag: Boolean = false,
                               val apiTradeAvailableFlag: Boolean = false) {

    companion object{
        fun parse(response: String?): GetTradingStatusResponse? {
            if(response == null) return null
            return try {
                val json = JSONObject(response)
                parse(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        fun parse(responseJson: JSONObject): GetTradingStatusResponse {
            return try {
                val tradingStatus = TradingStatus.parse(responseJson.getString(TRADING_STATUS))
                val limitOrderAvailableFlag = responseJson.getBoolean(LIMIT_ORDER_AVAILABLE_FLAG)
                val apiTradeAvailableFlag = responseJson.getBoolean(API_TRADE_AVAILABLE_FLAG)
                GetTradingStatusResponse(tradingStatus,
                    limitOrderAvailableFlag, apiTradeAvailableFlag)
            }
            catch (e: Exception) {
                e.printStackTrace()
                GetTradingStatusResponse()
            }

        }
    }

}