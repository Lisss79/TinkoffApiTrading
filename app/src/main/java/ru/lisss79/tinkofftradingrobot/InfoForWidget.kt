package ru.lisss79.tinkofftradingrobot

import ru.lisss79.tinkofftradingrobot.queries_and_responses.Direction
import ru.lisss79.tinkofftradingrobot.queries_and_responses.PostOrder
import java.io.Serializable

/**
 * Класс данных для передачи в виджет и отображения
 */
data class InfoForWidget(
    val isError: Boolean = true,
    val tradesAvailable: Boolean = false,
    val orderDirection: Direction = Direction.ORDER_DIRECTION_UNSPECIFIED
) :
    Serializable {
    companion object {

        // Создает экземпляр из конфигурации и выставленной заявки
        fun createFromConfig(config: TradingConfig, order: PostOrder): InfoForWidget {
            val isError = config.accountId.isEmpty()
            val tradesAvailable = config.tradesAvailable
            var orderDirection = Direction.ORDER_DIRECTION_UNSPECIFIED
            if (config.activeOrders > 0) orderDirection = config.activeOrderDirection
            else if (order.accountId.isNotEmpty()) orderDirection = order.direction
            return InfoForWidget(isError, tradesAvailable, orderDirection)
        }
    }
}
