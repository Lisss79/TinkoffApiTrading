package ru.lisss79.tinkofftradingrobot.activities.fragments

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import ru.lisss79.tinkofftradingrobot.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class TimeSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.time_preferences, rootKey)

        // Меню ввода времени заявок в дневной аукцион
        val dayAuction =
            findPreference<EditTextPreference>(getString(R.string.day_auction_time))
        dayAuction?.setOnBindEditTextListener { editText ->
            editText.inputType =
                InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_TIME
            val filter = InputFilter.LengthFilter(8)
            editText.filters = arrayOf(filter)
            editText.gravity = Gravity.CENTER_HORIZONTAL
        }
        dayAuction?.setOnPreferenceChangeListener { preference, newValue ->
            if (checkForCorrectTime(newValue.toString())) true
            else {
                Toast.makeText(context, "Введено некорректное время!", Toast.LENGTH_SHORT).show()
                false
            }

        }

        // Меню ввода времени заявок в вечерний аукцион
        val eveningAuction =
            findPreference<EditTextPreference>(getString(R.string.evening_auction_time))
        eveningAuction?.setOnBindEditTextListener { editText ->
            editText.inputType =
                InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_TIME
            val filter = InputFilter.LengthFilter(8)
            editText.filters = arrayOf(filter)
            editText.gravity = Gravity.CENTER_HORIZONTAL
        }
    }

    private fun checkForCorrectTime(value: String) =
        try {
            val localTime = LocalTime.parse(value, DateTimeFormatter.ISO_TIME)
            true
        } catch (e: DateTimeParseException) {
            e.printStackTrace()
            false
        }
}