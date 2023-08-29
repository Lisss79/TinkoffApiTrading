package ru.lisss79.tinkofftradingrobot

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.NumberPicker
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceManager

class PercentPreference(context: Context, attrs: AttributeSet?):
    Preference(context, attrs), OnPreferenceClickListener {

    private lateinit var numberPicker: NumberPicker
    private val settingsPrefs: SharedPreferences
    private val editor: SharedPreferences.Editor
    private var defaultValue: Float = 0f
    private var value: Float = 0f
    init {
        onPreferenceClickListener = this
        settingsPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        editor = settingsPrefs.edit()
        defaultValue = attrs?.getAttributeFloatValue(
            "http://schemas.android.com/apk/res-auto",
            "defaultValue", 0f
        ) ?: 0f
        value = if (settingsPrefs.contains(key)) settingsPrefs.getFloat(key, 0f)
        else defaultValue
        summary = "${(value * 100).toInt()}%"
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val builder = AlertDialog.Builder(context)
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val viewGroup = inflater.inflate(R.layout.percent_picker, null)
        builder.setView(viewGroup).setPositiveButton("OK") { _, _ ->
            editor.putFloat(key, numberPicker.value / 100f)
            editor.apply()
            summary = "${numberPicker.value}%"
        }
            .setNegativeButton("Отмена", null).setTitle("Выберите значение")
        value = settingsPrefs.getFloat(key, 0f)
        numberPicker = viewGroup.findViewById(R.id.numberPicker)
        numberPicker.minValue = 0
        numberPicker.maxValue = 100
        numberPicker.value = (value * 100).toInt()
        builder.show()
        return true
    }

}