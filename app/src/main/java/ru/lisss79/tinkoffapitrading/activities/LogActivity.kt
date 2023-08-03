package ru.lisss79.tinkoffapitrading.activities

import android.os.Bundle
import android.view.ContextMenu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import ru.lisss79.tinkoffapitrading.R
import java.io.File
import java.io.IOException

class LogActivity : AppCompatActivity() {
    private val LOG_FILE = 1
    private val LOG_ROBOT = 2
    private var text: String = ""
    private var selectedLog = LOG_FILE
    private lateinit var logFile: File
    private lateinit var robotTrades: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        logFile = File(getExternalFilesDir(null), "logfile.txt")
        robotTrades = File(getExternalFilesDir(null), "robot.txt")
        val textViewLog = findViewById<TextView>(R.id.textViewLog)
        text = readTextFromFile(logFile)
        textViewLog.text = text
        registerForContextMenu(textViewLog)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener {
            text = when (it.itemId) {
                R.id.logFile -> {
                    selectedLog = LOG_FILE
                    readTextFromFile(logFile)
                }
                R.id.robotLog -> {
                    selectedLog = LOG_ROBOT
                    readTextFromFile(robotTrades)
                }
                else -> ""
            }
            textViewLog.text = text
            true
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.log_context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete_log -> {
                if (selectedLog == LOG_FILE) {
                    try {
                        logFile.delete()
                        recreate()
                    }
                    catch (e: IOException) {
                        e.printStackTrace()
                    }

                } else if (selectedLog == LOG_ROBOT) {
                    try {
                        robotTrades.delete()
                        recreate()
                    }
                    catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun readTextFromFile(file: File) =
        try {
            file.readText()
        }
        catch (e: IOException) {
            "Ошибка чтения данных"
        }

}