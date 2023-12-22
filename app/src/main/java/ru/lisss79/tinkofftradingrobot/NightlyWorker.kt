package ru.lisss79.tinkofftradingrobot

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import ru.lisss79.tinkofftradingrobot.queries_and_responses.OperationsResponse
import ru.lisss79.tinkofftradingrobot.queries_and_responses.OperationsResponse.Operation.OperationType
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

const val NIGHTLY_WORKER_ID = "nightly_worker_tag"
const val SCHEDULED_TIME = "03:00:00"
val ROBOT_START_DATE: LocalDate = LocalDate.of(2023, 7, 22)
const val INITIAL_MONEY: Float = 10000f

/**
 * Класс Worker, который используется для запуска задач каждую ночь
 */
class NightlyWorker(val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        println("Nightly Worker is executing doWork!")
        val prefs =
            context.getSharedPreferences(context.packageName, AppCompatActivity.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(ROBOT_IS_RUNNING, false)
            apply()
        }
        val logFile =
            File(context.getExternalFilesDir(null), context.getString(R.string.logfile_name))
        logFile.appendText("${Instant.now()} сбрасываем состояние флага isRunning\n")

        val result = updateOperations(context)
        logFile.appendText(
            "Лог робота " +
                    "${if (result) "успешно обновлен" else "не удалось обновить"}\n"
        )

        return Result.success()
    }

    private fun updateOperations(context: Context): Boolean {
        val robotLog = File(
            context.getExternalFilesDir(null),
            context.getString(R.string.robotfile_name)
        )
        val moneyLog = File(
            context.getExternalFilesDir(null),
            context.getString(R.string.moneyfile_name)
        )

        val settingsPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val accountId = settingsPrefs.getString(context.getString(R.string.ACCOUNT), "") ?: ""
        val token = settingsPrefs.getString(context.getString(R.string.TOKEN), "") ?: ""
        val api = TinkoffOpenApi(token)

        var from = Instant.from(ROBOT_START_DATE.atStartOfDay(ZoneId.systemDefault()))
        val to = Instant.now()
        val tradesLog = RobotTradesLog.fromFile(robotLog)
        val operationsResponse = OperationsResponse.fromFile(moneyLog)
        if (tradesLog == null || operationsResponse == null) return false

        val ordersFromLocal = tradesLog.orders.takeWhile { it?.loadedFromServer == true }
        val operationsFromLocal = operationsResponse.operations

        if (ordersFromLocal.isNotEmpty())
            from = (ordersFromLocal.last()?.orderState?.orderDate)
                ?.plus(1, ChronoUnit.SECONDS) ?: to

        val operationsFromServer = api.getOperations(accountId, from, to).get() ?: return false

        val orders = operationsFromServer.operations.filter {
            it.operationType == OperationType.OPERATION_TYPE_BUY
                    || it.operationType == OperationType.OPERATION_TYPE_SELL
        }.reversed()

        val moneyOperations = operationsFromServer.operations.filter {
            it.operationType == OperationType.OPERATION_TYPE_INPUT
        }.reversed()

        val newRobotTradesLog =
            RobotTradesLog(ordersFromLocal) + RobotTradesLog.from(orders)
        val newMoneyOperations =
            OperationsResponse(operationsFromLocal) + OperationsResponse(moneyOperations)

        return RobotTradesLog.toFile(robotLog, newRobotTradesLog.orders) &&
                OperationsResponse.toFile(moneyLog, newMoneyOperations.operations)
    }

}