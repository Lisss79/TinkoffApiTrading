package ru.lisss79.tinkofftradingrobot.queries_and_responses

import org.json.JSONObject
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.CURRENCY
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.NANO
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.UNITS
import kotlin.math.floor

class Money(val currency: String = "rub",
            val units: String = "0",
            val nano: Long = 0L) {

    val value: Float
        get() {
            return try {
                (units.toFloat() + nano / 1E9).toFloat()
            } catch (e: NumberFormatException) {
                e.printStackTrace()
                0f
            }
        }

    companion object{

        val ZERO: Money
        get() = parse(0f)

        fun parse(response: String): Money {
            return try {
                parse(response.toFloat())
            } catch (e: NumberFormatException) {
                try {
                    parse(JSONObject(response))
                } catch (e: Exception) {
                    ZERO
                }
            }
        }

        fun parse(responseJson: JSONObject): Money {
            return try {
                val currency = responseJson.getString(CURRENCY)
                val units = responseJson.getString(UNITS)
                val nano = responseJson.getLong(NANO)
                return Money(currency, units, nano)
            } catch (e: Exception) {
                e.printStackTrace()
                ZERO
            }

        }

        fun parse(value: Float): Money {
            val units = floor(value.toDouble())
            val nano = (value.toBigDecimal() - units.toBigDecimal()) * 1E9.toBigDecimal()
            return Money(units = units.toString(), nano = nano.toLong())
        }

    }

    override fun toString(): String {
        return "$value $currency"
    }
}