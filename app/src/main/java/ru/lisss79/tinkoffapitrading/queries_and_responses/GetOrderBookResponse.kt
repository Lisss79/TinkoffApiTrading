package ru.lisss79.tinkoffapitrading.queries_and_responses

import org.json.JSONObject
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.ASKS
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.BIDS
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.PRICE
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.QUANTITY

class GetOrderBookResponse(val orders: Pair<List<Bid>, List<Ask>>) {
    val bestBidPrice: Float             // Лучшая цена в стакане на покупку
    private val bidMaxQuantity: Int     // Максимальный объем на покупку
    val bidPriceWithMaxQuantity: Float  // Цена в стакане на покупку с максимальным объемом
    val bestAskPrice: Float             // Лучшая цена в стакане на продажу
    private val askMaxQuantity: Int     // Максимальный объем на продажу
    val askPriceWithMaxQuantity: Float  // Цена в стакане на продажу с максимальным объемом

    private val bid: List<Bid> = orders.first
    private val ask: List<Ask> = orders.second

    init {
        if (bid.isNotEmpty()) {
            bestBidPrice = bid[0].price.value
            val maxBid = bid.maxByOrNull { it.quantity }
            bidMaxQuantity = maxBid?.quantity ?: 0
            bidPriceWithMaxQuantity = maxBid?.price?.value ?: 0f
        }
        else {
            bestBidPrice = 0f
            bidMaxQuantity = 0
            bidPriceWithMaxQuantity = 0f
        }

        if (ask.isNotEmpty()) {
            bestAskPrice = ask[0].price.value
            val maxAsk = ask.maxByOrNull { it.quantity }
            askMaxQuantity = maxAsk?.quantity ?: 0
            askPriceWithMaxQuantity = maxAsk?.price?.value ?: 0f
        }
        else {
            bestAskPrice = 0f
            askMaxQuantity = 0
            askPriceWithMaxQuantity = 0f
        }

    }

    fun getBidPriceWithQuantityTolerance(tolerance: Float): Float {
        val minPossibleQuantity = bidMaxQuantity * tolerance
        return if (bid.isNotEmpty()) {
            val bestBid = bid.maxByOrNull {
                if (it.quantity >= minPossibleQuantity) it.price.value
                else 0f
            }
            bestBid?.price?.value ?: 0f
        } else 0f
    }

    fun getAskPriceWithQuantityTolerance(tolerance: Float): Float {
        val minPossibleQuantity = askMaxQuantity * tolerance
        return if (ask.isNotEmpty()) {
            val bestAsk = ask.minByOrNull {
                if (it.quantity >= minPossibleQuantity) it.price.value
                else Float.MAX_VALUE
            }
            bestAsk?.price?.value ?: 0f
        } else 0f
    }

    fun getQuantity(order: GetOrdersResponse.Order): Int {
        return when (order.direction) {
            Direction.ORDER_DIRECTION_BUY -> {
                val b = bid.find { it.price.value == order.initialSecurityPrice.value }
                b?.quantity ?: 0
            }
            Direction.ORDER_DIRECTION_SELL -> {
                val a = ask.find { it.price.value == order.initialSecurityPrice.value }
                a?.quantity ?: 0
            }
            else -> 0
        }
    }

    companion object {
        fun parse(response: String?): GetOrderBookResponse? {
            if (response == null) return null
            return try {
                val json = JSONObject(response)
                parse(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun parse(responseJson: JSONObject): GetOrderBookResponse {
            val listB = mutableListOf<Bid>()
            val listA = mutableListOf<Ask>()
            try {
                val jsonArrayB = responseJson.getJSONArray(BIDS)
                val jsonArrayA = responseJson.getJSONArray(ASKS)
                if (jsonArrayB.length() > 0 && jsonArrayA.length() > 0) {
                    for (index in 0 until jsonArrayB.length()) {
                        val elementB = jsonArrayB.getJSONObject(index)
                        val elementA = jsonArrayA.getJSONObject(index)
                        listB.add(Bid.parse(elementB))
                        listA.add(Ask.parse(elementA))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return GetOrderBookResponse(listB to listA)
        }
    }

    override fun toString(): String {
        return bid.toString().replace(", ", "\n") + "\n" +
                ask.toString().replace(", ", "\n")
    }

    class Bid(
        val price: Price = Price.ZERO,
        val quantity: Int = 0
    ) {

        companion object {
            fun parse(response: String): Bid {
                val json = JSONObject(response)
                return (parse(json))
            }

            fun parse(responseJson: JSONObject): Bid {
                return try {
                    val price = Price.parse(responseJson.getJSONObject(PRICE))
                    val quantity = responseJson.getString(QUANTITY).toInt()
                    Bid(price, quantity)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Bid()
                }
            }
        }
        override fun toString(): String {
            return "Bid: price = ${price.value}; quantity = $quantity"
        }
    }

    class Ask(
        val price: Price = Price.ZERO,
        val quantity: Int = 0
    ) {

        companion object {
            fun parse(response: String): Ask {
                val json = JSONObject(response)
                return (parse(json))
            }

            fun parse(responseJson: JSONObject): Ask {
                return try {
                    val price = Price.parse(responseJson.getJSONObject(PRICE))
                    val quantity = responseJson.getString(QUANTITY).toInt()
                    Ask(price, quantity)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Ask()
                }
            }
        }
        override fun toString(): String {
            return "Ask: price = ${price.value}; quantity = $quantity"
        }
    }

}