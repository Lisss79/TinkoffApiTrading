package ru.lisss79.tinkofftradingrobot

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File
import java.time.Instant

const val NIGHTLY_WORKER_ID = "nightly_worker_tag"
const val SCHEDULED_TIME = "00:03:00"

/**
 * Класс Worker, который используется для запуска задач каждую ночь
 */
class NightlyWorker(val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        println("Nightly Worker is executing doWork!")
        val prefs = context.getSharedPreferences(context.packageName, AppCompatActivity.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(ROBOT_IS_RUNNING, false)
            apply()
        }
        val logFile = File(
            context.getExternalFilesDir(null),
            context.getString(R.string.logfile_name)
        )
        logFile.appendText("${Instant.now()} сбрасываем состояние флага isRunning\n")

        return Result.success()
    }
}