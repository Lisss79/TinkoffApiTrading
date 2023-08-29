package ru.lisss79.tinkofftradingrobot.queries_and_responses

enum class OrderType(val rus_name: String): java.io.Serializable {
    ORDER_TYPE_UNSPECIFIED("не определено"),
    ORDER_TYPE_LIMIT("лимитированная"),
    ORDER_TYPE_MARKET("по рыночной цене"),
    ORDER_TYPE_BESTPRICE("лучшая цена")
}