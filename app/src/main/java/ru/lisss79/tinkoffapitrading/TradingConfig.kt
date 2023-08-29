package ru.lisss79.tinkoffapitrading

import ru.lisss79.tinkoffapitrading.queries_and_responses.Direction
import java.io.Serializable

/**
 * Класс, содержащий информацию о состоянии торгов, счета, цен, заявок
 */
data class TradingConfig(
    var accountId: String = "",                     // Id торгового счета пользователя
    var figi: String = "",                          // FIGI инструмента для торгов
    var instrumentId: String = "",                  // Id инструмента для торгов
    var minPriceIncrement: Float = 0f,              // минимальный шаг цены
    var currencyQuantity: Float = 0f,               // количество денег на торговом счету
    var currencyIso: String = "rub",                // валюта торгового счета
    var minPriceRecent: Float = 0f,                 // минимальная цена за последний период
    var maxPriceRecent: Float = 0f,                 // максимальная цена за последний период
    var tradesAvailable: Boolean = false,           // доступны ли торги сейчас
    var positionQuantity: Int = 0,                  // число позиций на торговом счету
    var activeOrders: Int = 0,                      // число активных заявок по инструменту
    var activeOrdersPrice: Float = 0f,              // цена активной заявки
    var activeOrdersQuantity: Int = 0,              // количество позиций активной заявки
    var activeOrderDirection: Direction = Direction.ORDER_DIRECTION_UNSPECIFIED,    // направление активной заявки
    var activeOrderId: String = "",                 // id активной заявки
    var bestBidPrice: Float = 0f,                   // лучшая цена в стакане на покупку
    var bestAskPrice: Float = 0f,                   // лучшая цена в стакане на продажу
    var bidPriceWithMaxQuantity: Float = 0f,        // цена в стакане на покупку с наибольшим количеством заявок
    var askPriceWithMaxQuantity: Float = 0f,        // цена в стакане на продажу с наибольшим количеством заявок
    var bidPriceWithQuantityTolerance: Float = 0f,  // цена в стакане на покупку с допуском объема
    var askPriceWithQuantityTolerance: Float = 0f,  // цена в стакане на продажу с допуском объема
    var closePrice: Float = 0f,                     // цена закрытия прошлого дня
    var lastPurchasePrice: Float = 0f,              // цена лота последней покупки
    var selectedPurchasePrice: Float = 0f,          // выбранная цена на покупку
    var selectedSellingPrice: Float = 0f,           // выбранная на продажу
    var error: String = ""                          // текст ошибки, если она произошла
) : Serializable {
    val CR = System.lineSeparator()

    override fun toString(): String {
        val data1 = "Идентификатор счета: $accountId$CR" +
                "Тикер бумаги: $INSTRUMENT_TICKER$CR" +
                "Uid бумаги: $instrumentId$CR" +
                "FIGI бумаги: $figi$CR" +
                "Цена закрытия - $closePrice, шаг цены - $minPriceIncrement$CR" +
                "Цена последней покупки - $lastPurchasePrice$CR" +
                "На счету денег - $currencyQuantity$currencyIso, бумаг - $positionQuantity$CR$CR"
        val data2 =
            if (tradesAvailable && selectedPurchasePrice != 0f && selectedSellingPrice != 0f) {
                "Цены последних торгов: мин. - $minPriceRecent, макс. - $maxPriceRecent$CR" +
                        "Лучшие цены в стакане: bid - $bestBidPrice, ask - $bestAskPrice$CR" +
                        "Цены с макс. объемом: bid - $bidPriceWithMaxQuantity, " +
                        "ask - $askPriceWithMaxQuantity$CR" +
                        "Лучшие цены с допуском объема: bid - $bidPriceWithQuantityTolerance, " +
                        "ask - $askPriceWithQuantityTolerance$CR" +
                        "Выбранные цены: покупка - $selectedPurchasePrice, продажа - $selectedSellingPrice $CR$CR"
            } else if (tradesAvailable && selectedPurchasePrice == 0f && selectedSellingPrice == 0f) {
                "Данные по текущим ценам недоступны, т.к. цены не получены$CR$CR"
            } else "Данные по текущим ценам недоступны, т.к. торги сейчас не ведутся$CR$CR"
        val data3 = if (activeOrders > 0) {
            "Активных заявок - $activeOrders, ${activeOrdersQuantity}шт по цене $activeOrdersPrice,$CR" +
                    "id = $activeOrderId, направление - $activeOrderDirection"
        } else "Нет активных заявок$CR"

        return data1 + data2 + data3
    }

}