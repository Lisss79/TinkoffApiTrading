package ru.lisss79.tinkoffapitrading

import org.junit.Test

import java.time.Instant
import java.util.Calendar

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class UnitTest {
    val robot = RobotReceiver()

    @Test
    fun prices() {
        robot.getTradingData()
        val (min, max) = robot.getMinAndMaxPrices()
        println("$min, $max")
    }
}