package ru.lisss79.tinkofftradingrobot.data_classes

import ru.lisss79.tinkofftradingrobot.queries_and_responses.Direction
import java.time.Instant

/**
 * Успешно выполненная сделка покупки или продажи
 */
data class Deal(
    val figi: String = "",
    val dateTime: Instant = Instant.now(),
    val result: Float = 0f,
    val price: Float = 0f,
    val quantity: Int = 0,
    val direction: Direction = Direction.ORDER_DIRECTION_UNSPECIFIED
)
