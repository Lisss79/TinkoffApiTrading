package ru.lisss79.tinkofftradingrobot.queries_and_responses

import org.json.JSONObject
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.ACCOUNT_ID
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.IDEMPOTENCY_KEY
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.ORDER_ID
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.PRICE
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.QUANTITY

class ReplaceOrder(
    val quantity: String = "0",
    val price: Price = Price.ZERO,
    val accountId: String = "",
    val orderId: String = "",
    val idempotencyKey: String = ""
) {
    private val CR = System.lineSeparator()

    fun toJson(): JSONObject {
        val json = JSONObject()
        return json.apply {
            put(QUANTITY, quantity)
            put(PRICE, price.toJson())
            put(ACCOUNT_ID, accountId)
            put(ORDER_ID, orderId)
            put(IDEMPOTENCY_KEY, idempotencyKey)
        }
    }

    override fun toString(): String {
        return "Заменена заявка c id $orderId$CR$quantity шт. по цене $price"
    }

}