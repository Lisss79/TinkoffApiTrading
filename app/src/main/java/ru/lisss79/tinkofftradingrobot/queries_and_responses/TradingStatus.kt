package ru.lisss79.tinkofftradingrobot.queries_and_responses

enum class TradingStatus(val rus_name: String, val isTradingAvailable: Boolean) {
    SECURITY_TRADING_STATUS_UNSPECIFIED("не определен", false),
    SECURITY_TRADING_STATUS_NOT_AVAILABLE_FOR_TRADING("", false),
    SECURITY_TRADING_STATUS_OPENING_PERIOD("", false),
    SECURITY_TRADING_STATUS_CLOSING_PERIOD("", false),
    SECURITY_TRADING_STATUS_BREAK_IN_TRADING("", false),
    SECURITY_TRADING_STATUS_NORMAL_TRADING("обычная торговля", true),
    SECURITY_TRADING_STATUS_CLOSING_AUCTION("аукцион закрытия", false),
    SECURITY_TRADING_STATUS_DARK_POOL_AUCTION("", false),
    SECURITY_TRADING_STATUS_DISCRETE_AUCTION("дискретный аукцион", false),
    SECURITY_TRADING_STATUS_OPENING_AUCTION_PERIOD("аукцион открытия", true),
    SECURITY_TRADING_STATUS_TRADING_AT_CLOSING_AUCTION_PRICE("", false),
    SECURITY_TRADING_STATUS_SESSION_ASSIGNED("", false),
    SECURITY_TRADING_STATUS_SESSION_CLOSE("", false),
    SECURITY_TRADING_STATUS_SESSION_OPEN("", false),
    SECURITY_TRADING_STATUS_DEALER_NORMAL_TRADING("", false),
    SECURITY_TRADING_STATUS_DEALER_BREAK_IN_TRADING("", false),
    SECURITY_TRADING_STATUS_DEALER_NOT_AVAILABLE_FOR_TRADING("", false);

    companion object{
        fun parse(response: String): TradingStatus {
            for(status in TradingStatus.values()) {
                if(status.name == response) return status
            }
            return SECURITY_TRADING_STATUS_UNSPECIFIED
        }
    }

}