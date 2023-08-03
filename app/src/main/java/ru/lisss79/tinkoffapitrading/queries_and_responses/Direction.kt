package ru.lisss79.tinkoffapitrading.queries_and_responses

enum class Direction(val rus_name: String) : java.io.Serializable {
    ORDER_DIRECTION_UNSPECIFIED("не определено"),
    ORDER_DIRECTION_BUY("покупка"),
    ORDER_DIRECTION_SELL("продажа");

    companion object{
        fun parse(response: String): Direction {
            for(direction in Direction.values()) {
                if(direction.name == response) return direction
            }
            return ORDER_DIRECTION_UNSPECIFIED
        }
    }

}