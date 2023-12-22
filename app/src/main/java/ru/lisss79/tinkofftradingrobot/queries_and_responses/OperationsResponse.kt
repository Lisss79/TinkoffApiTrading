package ru.lisss79.tinkofftradingrobot.queries_and_responses

import org.json.JSONObject
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.DATE
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.DATE_TIME
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.FIGI
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.ID
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.OPERATIONS
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.OPERATION_TYPE
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.PAYMENT
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.PRICE
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.QUANTITY
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.STATE
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.TRADES
import java.io.File
import java.io.IOException
import java.time.Instant

class OperationsResponse(val operations: List<Operation>) {

    companion object {
        fun parse(response: String?): OperationsResponse? {
            if (response == null) return null
            return try {
                val json = JSONObject(response)
                parse(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun parse(responseJson: JSONObject): OperationsResponse {
            val list = mutableListOf<Operation>()
            try {
                val jsonArray = responseJson.getJSONArray(OPERATIONS)
                for (index in 0 until jsonArray.length()) {
                    val element = jsonArray.getJSONObject(index)
                    list.add(Operation.parse(element))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return OperationsResponse(list)
        }

        // Получение данных из файла
        fun fromFile(file: File) =
            try {
                val moneyLines = file.readLines()
                val moneyOperations = moneyLines.map { Operation.parse(it) }
                OperationsResponse(moneyOperations)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }

        // Записать в файл данные об операциях
        fun toFile(file: File, operations: List<Operation>) =
            try {
                if (operations.isEmpty()) throw Exception("No data to write to file")
                file.writeText("")
                operations.forEach { operation ->
                    operation.apply {
                        file.appendText(toJsonLog())
                        file.appendText("\n")
                    }
                }
                true
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
    }

    /**
     * Объявляем оператор "плюс"
     */
    operator fun plus(other: OperationsResponse): OperationsResponse {
        val newOperations = operations + other.operations
        return OperationsResponse(newOperations)
    }

    class Operation(
        val id: String = "",
        val figi: String = "",
        val price: Money = Money.ZERO,
        val payment: Money = Money.ZERO,
        val quantity: Int = 0,
        val state: State = State.OPERATION_STATE_UNSPECIFIED,
        val operationType: OperationType = OperationType.OPERATION_TYPE_UNSPECIFIED,
        val date: Instant = Instant.now(),
        val trades: List<Trade> = listOf()
    ) {

        companion object {
            fun parse(response: String): Operation {
                val json = JSONObject(response)
                return (parse(json))
            }

            fun parse(responseJson: JSONObject): Operation {
                return try {
                    val id = responseJson.optString(ID)
                    val figi = responseJson.optString(FIGI)
                    val price = Money.parse(responseJson.optString(PRICE))
                    val payment = Money.parse(responseJson.optString(PAYMENT))
                    val quantity = responseJson.optString(QUANTITY, "0").toInt()
                    val state = State.parse(responseJson.optString(STATE))
                    val operationType = OperationType.parse(responseJson.optString(OPERATION_TYPE))
                    val instant = Instant.parse(responseJson.optString(DATE))

                    val trades = mutableListOf<Trade>()
                    try {
                        val jsonArray = responseJson.getJSONArray(TRADES)
                        for (index in 0 until jsonArray.length()) {
                            val element = jsonArray.getJSONObject(index)
                            trades.add(Trade.parse(element))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    Operation(
                        id, figi, price, payment, quantity,
                        state, operationType, instant, trades
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    Operation()
                }

            }
        }

        fun toJsonLog(): String {
            return JSONObject().apply {
                put(ID, id)
                put(OPERATION_TYPE, operationType.name)
                put(STATE, state.name)
                put(PRICE, price.value)
                put(QUANTITY, quantity)
                put(PAYMENT, payment.value)
                put(DATE, date)
            }.toString()
        }

        fun getLastTradesInstant() =
            if (trades.isNotEmpty()) trades.maxOf { it.time } else date

        class Trade(
            val price: Price = Price.ZERO,
            val quantity: Int = 0,
            val time: Instant = Instant.now()
        ) {

            companion object {
                fun parse(response: String): Trade {
                    val json = JSONObject(response)
                    return (parse(json))
                }

                fun parse(responseJson: JSONObject): Trade {
                    return try {
                        val price = Price.parse(responseJson.getJSONObject(PRICE))
                        val quantity = responseJson.getInt(QUANTITY)
                        val time = Instant.parse(responseJson.getString(DATE_TIME))
                        Trade(price, quantity, time)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Trade()
                    }

                }
            }
        }

        /**
         * Состояние операции
         */
        enum class State(val rus_name: String) {
            OPERATION_STATE_UNSPECIFIED("не определено"),
            OPERATION_STATE_EXECUTED("исполнена"),
            OPERATION_STATE_CANCELED("отменена"),
            OPERATION_STATE_PROGRESS("выполняется");

            companion object {
                fun parse(response: String): State {
                    for (state in State.values()) {
                        if (state.name == response) return state
                    }
                    return OPERATION_STATE_UNSPECIFIED
                }
            }
        }

        /**
         * Тип операции
         */
        enum class OperationType(val rus_name: String) {
            OPERATION_TYPE_UNSPECIFIED("не определено"),
            OPERATION_TYPE_INPUT("пополнение счета"),
            OPERATION_TYPE_BUY("покупка"),
            OPERATION_TYPE_SELL("продажа");

            companion object {
                fun parse(response: String): OperationType {
                    for (type in OperationType.values()) {
                        if (type.name == response) return type
                    }
                    return OPERATION_TYPE_UNSPECIFIED
                }
            }
        }

    }

}