package ru.lisss79.tinkoffapitrading

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.lisss79.tinkoffapitrading.queries_and_responses.*
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.ACCOUNT_ID
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.DEPTH
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.EXCHANGE
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.FROM
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.ID
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.ID_TYPE
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.INSTRUMENTS
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.INSTRUMENT_ID
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.INSTRUMENT_KIND
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.ORDER_ID
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.QUERY
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.TO
import java.io.IOException
import java.time.Instant
import java.util.concurrent.CompletableFuture

const val API_URL = "https://invest-public-api.tinkoff.ru/rest/"
const val EMPTY_BODY = "{}"

class TinkoffOpenApi(val token: String) {

    fun getAccounts(): CompletableFuture<GetAccountsResponse?> {

        val future: CompletableFuture<GetAccountsResponse?> = CompletableFuture.supplyAsync {
            var accounts: GetAccountsResponse? = null
            val client = OkHttpClient()

            val url = API_URL + "tinkoff.public.invest.api.contract.v1.UsersService/GetAccounts"
            val jsonRequest = EMPTY_BODY
            val request = getRequestWithAuth(url, jsonRequest)

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Запрос к серверу не был успешен (метод getAccounts):" +
                                " ${response.code} ${response.body?.string()}")
                    }
                    accounts = GetAccountsResponse.parse(response.body?.string())
                }
            } catch (e: IOException) {
                println("$e")
            }
            return@supplyAsync accounts
        }
        return future
    }

    fun findInstruments(query: String, instrumentKind: String):
            CompletableFuture<FindInstrumentsResponse?> {

        val future: CompletableFuture<FindInstrumentsResponse?> = CompletableFuture.supplyAsync {
            var instruments: FindInstrumentsResponse? = null
            val client = OkHttpClient()

            val url =
                API_URL + "tinkoff.public.invest.api.contract.v1.InstrumentsService/FindInstrument"
            val mapRequest = mapOf(QUERY to query, INSTRUMENT_KIND to instrumentKind)
            val request = getRequestWithAuth(url, createBody(mapRequest))

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException(
                            "Запрос к серверу не был успешен (метод findInstruments):" +
                                    " ${response.code} ${response.body?.string()}"
                        )
                    }
                    instruments = FindInstrumentsResponse.parse(response.body?.string())
                }
            } catch (e: IOException) {
                println("$e")
            }
            return@supplyAsync instruments
        }
        return future
    }

    fun etfBy(id: String, idType: String):
            CompletableFuture<EtfResponse?> {

        val future: CompletableFuture<EtfResponse?> = CompletableFuture.supplyAsync {
            var instruments: EtfResponse? = null
            val client = OkHttpClient()

            val url = API_URL + "tinkoff.public.invest.api.contract.v1.InstrumentsService/EtfBy"
            val mapRequest = mapOf(ID to id, ID_TYPE to idType)
            val request = getRequestWithAuth(url, createBody(mapRequest))

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException(
                            "Запрос к серверу не был успешен (метод etfBy):" +
                                    " ${response.code} ${response.body?.string()}"
                        )
                    }
                    instruments = EtfResponse.parse(response.body?.string())
                }
            } catch (e: IOException) {
                println("$e")
            }
            return@supplyAsync instruments
        }
        return future
    }

    fun getPositions(accountId: String): CompletableFuture<GetPositionsResponse?> {

        val future: CompletableFuture<GetPositionsResponse?> = CompletableFuture.supplyAsync {
            var positions: GetPositionsResponse? = null
            val client = OkHttpClient()

            val url =
                API_URL + "tinkoff.public.invest.api.contract.v1.OperationsService/GetPositions"
            val mapRequest = mapOf(ACCOUNT_ID to accountId)
            val request = getRequestWithAuth(url, createBody(mapRequest))

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException(
                            "Запрос к серверу не был успешен (метод getPositions):" +
                                    " ${response.code} ${response.body?.string()}"
                        )
                    }
                    positions = GetPositionsResponse.parse(response.body?.string())
                }
            } catch (e: IOException) {
                println("$e")
            }
            return@supplyAsync positions
        }
        return future
    }

    fun getLastTrades(from: Instant, to: Instant, instrumentId: String):
            CompletableFuture<GetLastTradesResponse?> {

        val future: CompletableFuture<GetLastTradesResponse?> = CompletableFuture.supplyAsync {
            var trades: GetLastTradesResponse? = null
            val client = OkHttpClient()

            val url =
                API_URL + "tinkoff.public.invest.api.contract.v1.MarketDataService/GetLastTrades"
            val mapRequest =
                mapOf(FROM to from.toString(), TO to to.toString(), INSTRUMENT_ID to instrumentId)
            val request = getRequestWithAuth(url, createBody(mapRequest))

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException(
                            "Запрос к серверу не был успешен (метод getLastTrades):" +
                                    " ${response.code} ${response.body?.string()}"
                        )
                    }
                    trades = GetLastTradesResponse.parse(response.body?.string())
                }
            } catch (e: IOException) {
                println("$e")
            }
            return@supplyAsync trades
        }
        return future
    }

    fun getTradingStatus(instrumentId: String): CompletableFuture<GetTradingStatusResponse?> {

        val future: CompletableFuture<GetTradingStatusResponse?> = CompletableFuture.supplyAsync {
            var status: GetTradingStatusResponse? = null
            val client = OkHttpClient()

            val url = API_URL + "tinkoff.public.invest.api.contract.v1.MarketDataService/GetTradingStatus"
            val mapRequest = mapOf(INSTRUMENT_ID to instrumentId)
            val request = getRequestWithAuth(url, createBody(mapRequest))

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException(
                            "Запрос к серверу не был успешен (метод getTradingStatus):" +
                                    " ${response.code} ${response.body?.string()}"
                        )
                    }
                    status = GetTradingStatusResponse.parse(response.body?.string())
                }
            } catch (e: IOException) {
                println("$e")
            }
            return@supplyAsync status
        }
        return future
    }

    fun getOrders(accountId: String): CompletableFuture<GetOrdersResponse?> {

        val future: CompletableFuture<GetOrdersResponse?> = CompletableFuture.supplyAsync {
            var orders: GetOrdersResponse? = null
            val client = OkHttpClient()

            val url = API_URL + "tinkoff.public.invest.api.contract.v1.OrdersService/GetOrders"
            val mapRequest = mapOf(ACCOUNT_ID to accountId)
            val request = getRequestWithAuth(url, createBody(mapRequest))

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException(
                            "Запрос к серверу не был успешен (метод getOrders):" +
                                    " ${response.code} ${response.body?.string()}"
                        )
                    }
                    orders = GetOrdersResponse.parse(response.body?.string())
                }
            } catch (e: IOException) {
                println("$e")
            }
            return@supplyAsync orders
        }
        return future
    }

    fun getOrderState(accountId: String, orderId: String): CompletableFuture<OrderState?> {

        val future: CompletableFuture<OrderState?> = CompletableFuture.supplyAsync {
            var orderState: OrderState? = null
            val client = OkHttpClient()

            val url = API_URL + "tinkoff.public.invest.api.contract.v1.OrdersService/GetOrderState"
            val mapRequest = mapOf(ACCOUNT_ID to accountId, ORDER_ID to orderId)
            val request = getRequestWithAuth(url, createBody(mapRequest))

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException(
                            "Запрос к серверу не был успешен (метод getOrderState):" +
                                    " ${response.code} ${response.body?.string()}"
                        )
                    }
                    orderState = OrderState.parse(response.body?.string())
                }
            } catch (e: IOException) {
                println("$e")
            }
            return@supplyAsync orderState
        }
        return future
    }

    fun postOrder(order: PostOrder): CompletableFuture<PostOrderResponse?> {

        val future: CompletableFuture<PostOrderResponse?> = CompletableFuture.supplyAsync {
            var orderResponse: PostOrderResponse? = null
            val client = OkHttpClient()

            val url = API_URL + "tinkoff.public.invest.api.contract.v1.OrdersService/PostOrder"
            val jsonRequest = order.toJson().toString()
            val request = getRequestWithAuth(url, jsonRequest)

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException(
                            "Запрос к серверу не был успешен (метод postOrder):" +
                                    " ${response.code} ${response.body?.string()}"
                        )
                    }
                    orderResponse = PostOrderResponse.parse(response.body?.string())
                }
            } catch (e: IOException) {
                println("$e")
            }
            return@supplyAsync orderResponse
        }
        return future
    }

    fun replaceOrder(order: ReplaceOrder): CompletableFuture<PostOrderResponse?> {

        val future: CompletableFuture<PostOrderResponse?> = CompletableFuture.supplyAsync {
            var orderResponse: PostOrderResponse? = null
            val client = OkHttpClient()

            val url = API_URL + "tinkoff.public.invest.api.contract.v1.OrdersService/ReplaceOrder"
            val jsonRequest = order.toJson().toString()
            val request = getRequestWithAuth(url, jsonRequest)

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException(
                            "Запрос к серверу не был успешен (метод replaceOrder):" +
                                    " ${response.code} ${response.body?.string()}"
                        )
                    }
                    orderResponse = PostOrderResponse.parse(response.body?.string())
                }
            } catch (e: IOException) {
                println("$e")
            }
            return@supplyAsync orderResponse
        }
        return future
    }

    fun tradingSchedules(exchange: String, from: Instant, to: Instant):
            CompletableFuture<TradingSchedulesResponse?> {

        val future: CompletableFuture<TradingSchedulesResponse?> = CompletableFuture.supplyAsync {
            var tradingSchedules: TradingSchedulesResponse? = null
            val client = OkHttpClient()

            val url = API_URL + "tinkoff.public.invest.api.contract.v1.InstrumentsService/TradingSchedules"
            val mapRequest =
                mapOf(EXCHANGE to exchange, FROM to from.toString(), TO to to.toString())
            val request = getRequestWithAuth(url, createBody(mapRequest))

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException(
                            "Запрос к серверу не был успешен (метод tradingSchedules):" +
                                    " ${response.code} ${response.body?.string()}"
                        )
                    }
                    tradingSchedules = response.body?.string()
                        ?.let { TradingSchedulesResponse.parse(it) }
                }
            } catch (e: IOException) {
                println("$e")
            }
            return@supplyAsync tradingSchedules
        }
        return future
    }

    fun getOrderBook(instrumentId: String, depth: Int = 10): CompletableFuture<GetOrderBookResponse?> {

        val future: CompletableFuture<GetOrderBookResponse?> = CompletableFuture.supplyAsync {
            var orderBook: GetOrderBookResponse? = null
            val client = OkHttpClient()

            val url = API_URL + "tinkoff.public.invest.api.contract.v1.MarketDataService/GetOrderBook"
            val mapRequest =
                mapOf(INSTRUMENT_ID to instrumentId, DEPTH to depth.toString())
            val request = getRequestWithAuth(url, createBody(mapRequest))

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException(
                            "Запрос к серверу не был успешен (метод getOrderBook):" +
                                    " ${response.code} ${response.body?.string()}"
                        )
                    }
                    orderBook = response.body?.string()
                        ?.let { GetOrderBookResponse.parse(it) }
                }
            } catch (e: IOException) {
                println("$e")
            }
            return@supplyAsync orderBook
        }
        return future
    }

    fun getClosePrices(instrumentId: String): CompletableFuture<GetClosePricesResponse?> {

        val future: CompletableFuture<GetClosePricesResponse?> = CompletableFuture.supplyAsync {
            var closePrice: GetClosePricesResponse? = null
            val client = OkHttpClient()

            val url = API_URL + "tinkoff.public.invest.api.contract.v1.MarketDataService/GetClosePrices"
            val stringRequest = "{\"$INSTRUMENTS\": [ { \"$INSTRUMENT_ID\": \"$instrumentId\" } ] }"
            val request = getRequestWithAuth(url, stringRequest)

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException(
                            "Запрос к серверу не был успешен (метод getClosePrice):" +
                                    " ${response.code} ${response.body?.string()}"
                        )
                    }
                    closePrice = response.body?.string()
                        ?.let { GetClosePricesResponse.parse(it) }
                }
            } catch (e: IOException) {
                println("$e")
            }
            return@supplyAsync closePrice
        }
        return future
    }


    private fun getRequestWithAuth(url: String, body: String) =
        Request.Builder().addHeader("Authorization", "Bearer $token")
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .url(url).build()

    private fun createBody(map: Map<String, String>): String {
        val sb = java.lang.StringBuilder("{\n")
        map.forEach { (k, v) -> sb.append("\"$k\": \"$v\",\n") }
        sb.append("}")
        return sb.toString().replace(",\n}", "\n}")
    }

}