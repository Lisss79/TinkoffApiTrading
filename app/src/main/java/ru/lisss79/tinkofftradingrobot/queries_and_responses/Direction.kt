package ru.lisss79.tinkofftradingrobot.queries_and_responses

import ru.lisss79.tinkofftradingrobot.queries_and_responses.OperationsResponse.Operation.OperationType

enum class Direction(val rus_name: String) : java.io.Serializable {
    ORDER_DIRECTION_UNSPECIFIED("не определено"),
    ORDER_DIRECTION_BUY("покупка"),
    ORDER_DIRECTION_SELL("продажа");

    companion object {
        fun parse(response: String): Direction {
            for (direction in Direction.values()) {
                if (direction.name == response) return direction
            }
            return ORDER_DIRECTION_UNSPECIFIED
        }

        fun from(operationType: OperationType): Direction {
            return if (operationType == OperationType.OPERATION_TYPE_BUY) ORDER_DIRECTION_BUY
            else ORDER_DIRECTION_SELL
        }

    }

}