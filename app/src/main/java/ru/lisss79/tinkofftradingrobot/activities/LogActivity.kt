package ru.lisss79.tinkofftradingrobot.activities

import android.os.Bundle
import android.view.ContextMenu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import ru.lisss79.tinkofftradingrobot.R
import java.io.File
import java.io.IOException

class LogActivity : AppCompatActivity() {
    private val LOG_FILE = 1
    private val LOG_ROBOT = 2
    private val SCROLL_DELAY_MS = 100L
    private var scrollPositionLog = -1
    private var scrollPositionRobot = -1
    private var selectedLog = LOG_FILE
    private lateinit var logFile: File
    private lateinit var robotTrades: File
    private lateinit var scrollViewLog: ScrollView
    private lateinit var textViewLog: TextView

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        logFile = File(getExternalFilesDir(null), getString(R.string.logfile_name))
        robotTrades = File(getExternalFilesDir(null), getString(R.string.robotfile_name))
        scrollViewLog = findViewById(R.id.scrollViewLog)
        textViewLog = findViewById(R.id.textViewLog)
        readAndShow(logFile)
        registerForContextMenu(textViewLog)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationViewLog)
        bottomNavigationView.setOnItemSelectedListener {
            textViewLog.text = ""
            when (it.itemId) {
                R.id.logFile -> {
                    selectedLog = LOG_FILE
                    readAndShow(logFile)
                }
                R.id.robotLog -> {
                    selectedLog = LOG_ROBOT
                    readAndShow(robotTrades)
                }
            }
            true
        }

        scrollViewLog.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (selectedLog == LOG_FILE) scrollPositionLog = scrollY
            else scrollPositionRobot = scrollY
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
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                } else if (selectedLog == LOG_ROBOT) {
                    try {
                        robotTrades.delete()
                        recreate()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun readAndShow(file: File) {
        val text = try {
            file.readText()
        } catch (e: IOException) {
            "Ошибка чтения данных"
        }
        textViewLog.text = text

        when (file) {
            logFile -> {
                if (scrollPositionLog == -1) {
                    scrollViewLog
                        .postDelayed({ scrollViewLog.fullScroll(View.FOCUS_DOWN) }, SCROLL_DELAY_MS)
                } else {
                    val k = scrollPositionLog
                    scrollViewLog
                        .postDelayed({ scrollViewLog.scrollTo(0, k) }, SCROLL_DELAY_MS)
                }
            }
            robotTrades -> {
                if (scrollPositionRobot == -1) {
                    scrollViewLog
                        .postDelayed({ scrollViewLog.fullScroll(View.FOCUS_DOWN) }, SCROLL_DELAY_MS)
                } else {
                    val k = scrollPositionRobot
                    scrollViewLog
                        .postDelayed({ scrollViewLog.scrollTo(0, k) }, SCROLL_DELAY_MS)
                }
            }
        }
    }

}