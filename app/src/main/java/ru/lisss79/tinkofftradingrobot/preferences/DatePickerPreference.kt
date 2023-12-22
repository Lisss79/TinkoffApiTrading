package ru.lisss79.tinkofftradingrobot.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import ru.lisss79.tinkofftradingrobot.DatePickerWithNeutralButton
import ru.lisss79.tinkofftradingrobot.activities.SettingsActivity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar

class DatePickerPreference(context: Context, attrs: AttributeSet?) :
    Preference(context, attrs), OnPreferenceClickListener {
    private var key1: String = ""
    private var key2: String = ""
    private val settingsPrefs: SharedPreferences
    private val editor: SharedPreferences.Editor
    var valueStart: LocalDate = LocalDate.of(1970, 1, 1)
    var valueEnd: LocalDate = valueStart
    val defaultValue: LocalDate = valueStart

    init {
        onPreferenceClickListener = this
        settingsPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        editor = settingsPrefs.edit()
        key1 = "${key}_1"
        key2 = "${key}_2"

        val value1 = if (settingsPrefs.contains(key1)) settingsPrefs.getString(key1, "") else ""
        val value2 = if (settingsPrefs.contains(key2)) settingsPrefs.getString(key2, "") else ""
        try {
            valueStart = LocalDate.parse(value1)
            valueEnd = LocalDate.parse(value2)
        } catch (e: DateTimeParseException) {
            valueStart = defaultValue
            valueEnd = defaultValue
        }
        summary = getSummaryFromDates()
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val constraints = CalendarConstraints.Builder()
            .setStart(Calendar.getInstance().timeInMillis)
            .build()

        val dateRangePickerBuilder = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Выберите даты:")
            .setCalendarConstraints(constraints)
            .setTheme(
                com.google.android.material
                    .R.style.ThemeOverlay_MaterialComponents_MaterialCalendar
            )

        if (!(valueStart.isEqual(defaultValue) && valueEnd.isEqual(defaultValue))) {
            val selection = androidx.core.util.Pair(
                valueStart.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
                valueEnd.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            )
            dateRangePickerBuilder.setSelection(selection)
        }
        val dateRangePicker = dateRangePickerBuilder.build()

        dateRangePicker.addOnPositiveButtonClickListener {
            valueStart = LocalDate.from(
                Instant
                    .ofEpochMilli(it.first).atZone(ZoneId.systemDefault())
            )
            valueEnd = LocalDate.from(
                Instant
                    .ofEpochMilli(it.second).atZone(ZoneId.systemDefault())
            )
            updateDateRange(valueStart, valueEnd)
        }

        val picker = DatePickerWithNeutralButton(dateRangePicker) {
            updateDateRange(defaultValue, defaultValue)
        }

        (context as? SettingsActivity)?.supportFragmentManager?.let {
            picker.show(it, "range")
        }

        return true
    }

    private fun updateDateRange(date1: LocalDate, date2: LocalDate) {
        valueStart = date1
        valueEnd = date2
        settingsPrefs.edit {
            putString(key1, valueStart.toString())
            putString(key2, valueEnd.toString())
            apply()
            summary = getSummaryFromDates()
        }
    }

    private fun getSummaryFromDates(): String {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        return if ((valueStart == defaultValue) && (valueEnd == defaultValue)) "Не выбран"
        else "${valueStart.format(formatter)} - ${valueEnd.format(formatter)}"
    }

}