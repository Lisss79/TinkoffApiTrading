package ru.lisss79.tinkofftradingrobot.queries_and_responses

import org.json.JSONObject
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.NANO
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.UNITS
import java.io.Serializable
import kotlin.math.floor

class Price(
    val units: String = "0",
    val nano: Long = 0L
) : Serializable {

    val value: Float
        get() {
            return try {
                (units.toFloat() + nano / 1E9).toFloat()
            } catch (e: java.lang.NumberFormatException) {
                e.printStackTrace()
                0f
            }
        }

    companion object {

        val ZERO: Price
            get() = parse(0f)

        fun parse(response: String): Price {
            val json = JSONObject(response)
            return (parse(json))
        }
        fun parse(responseJson: JSONObject): Price {
            return try {
                val units = responseJson.getString(UNITS)
                val nano = responseJson.getLong(NANO)
                Price(units, nano)
            } catch (e: Exception) {
                e.printStackTrace()
                ZERO
            }

        }

        fun parse(value: Float): Price {
            val units = floor(value.toDouble())
            val nano = (value.toBigDecimal() - units.toBigDecimal()) * 1E9.toBigDecimal()
            return Price(units.toString(), nano.toLong())
        }
    }

    fun toJson(): JSONObject {
        val json = JSONObject()
        return json.apply {
            put(UNITS, units)
            put(NANO, nano)
        }
    }

    override fun toString(): String {
        return "$value"
    }
}