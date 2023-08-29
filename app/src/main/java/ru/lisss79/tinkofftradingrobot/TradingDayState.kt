package ru.lisss79.tinkofftradingrobot

/**
 * Состояния дня в отношении торговли
 * @param isTradingAvailable доступны ли торги
 * @param isEvening время ли вечерней сессии
 */
enum class TradingDayState(val isTradingAvailable: Boolean, val isEvening: Boolean = false) {

    // Не прочитаны данные
    ERROR(false, false),

    // Выходной
    DAY_OFF(false, false),

    // Утро, более чем за час до начала торгов
    MORNING_BEFORE_TRADES(false),

    // Утро, менее чем за час до начала торгов
    ONE_HOUR_BEFORE_TRADES(false),

    // Вечер, после окончания торгов
    EVENING_AFTER_TRADES(false),

    // Дневная торговая сессия, начало (прошло меньше RECENT_INTERVAL_MIN)
    TRADING_DAY_START(true),

    // Дневная торговая сессия (прошло больше RECENT_INTERVAL_MIN)
    TRADING_DAY(true),

    // Интервал между дневной и вечерней сессией
    PAUSE_BETWEEN_DAY_AND_EVENING(false, true),

    // Вечерняя торговая сессия
    TRADING_EVENING(true, true),

    // Аукцион открытия перед дневной сессией
    OPENING_AUCTION_DAY(true),
    CLOSING_AUCTION_DAY(true),

    // Аукцион открытия перед вечерней сессией
    OPENING_AUCTION_EVENING(true, true),
    CLOSING_AUCTION_EVENING(true)
}