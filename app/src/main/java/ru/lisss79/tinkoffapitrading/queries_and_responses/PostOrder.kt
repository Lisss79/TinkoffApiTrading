package ru.lisss79.tinkoffapitrading.queries_and_responses

import org.json.JSONObject
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.ACCOUNT_ID
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.DIRECTION
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.INSTRUMENT_ID
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.ORDER_TYPE
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.PRICE
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.QUANTITY

class PostOrder(
    val quantity: String = "0",
    val price: Price = Price.ZERO,
    var direction: Direction = Direction.ORDER_DIRECTION_UNSPECIFIED,
    val accountId: String = "",
    val orderType: OrderType = OrderType.ORDER_TYPE_UNSPECIFIED,
    val instrumentId: String = ""
) : java.io.Serializable {
    private val CR = System.lineSeparator()

    fun toJson(): JSONObject {
        val json = JSONObject()
        return json.apply {
            put(QUANTITY, quantity)
            put(PRICE, price.toJson())
            put(DIRECTION, direction)
            put(ACCOUNT_ID, accountId)
            put(ORDER_TYPE, orderType)
            put(INSTRUMENT_ID, instrumentId)
        }
    }

    override fun toString(): String {
        return if (direction != Direction.ORDER_DIRECTION_UNSPECIFIED)
            "Бумага c uid $instrumentId$CR" +
                    "${direction.rus_name} $quantity шт.$CR" +
                    "${orderType.rus_name} по цене $price"
        else "Нет"
    }

}