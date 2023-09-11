package ru.lisss79.tinkofftradingrobot.activities.fragments

import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import androidx.preference.*
import ru.lisss79.tinkofftradingrobot.R
import ru.lisss79.tinkofftradingrobot.SellingPriceHigher
import ru.lisss79.tinkofftradingrobot.TRADING_CURRENCY

class AlgorithmSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.algorithm_preferences, rootKey)

        // Меню выбора цены продажи, более высокой, чем цена покупки
        val sellingPricePref =
            findPreference<ListPreference>(getString(R.string.selling_price_higher))
        sellingPricePref?.entryValues = SellingPriceHigher.getEntries()
        sellingPricePref?.entries = SellingPriceHigher.getRusNames()
        if (sellingPricePref?.value == null)
            sellingPricePref?.setValueIndex(SellingPriceHigher.defaultValue.ordinal)

        // Меню разрешения замены заявок и замены только вверх
        val replaceOrder =
            findPreference<CheckBoxPreference>(getString(R.string.replace_order_enabled))
        val replaceOrderUp =
            findPreference<CheckBoxPreference>(getString(R.string.replace_order_up))
        replaceOrder?.setOnPreferenceChangeListener { _, newValue ->
            replaceOrderUp?.isEnabled = newValue as Boolean
            true
        }
        if (replaceOrder?.isChecked == true) replaceOrderUp?.isEnabled = true

        // Меню ввода минимального остатка денег
        val minMoney = findPreference<EditTextPreference>(getString(R.string.money_after_spent))
        minMoney?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
        minMoney?.summaryProvider = Preference.SummaryProvider<EditTextPreference> {
            val text = it.text
            if (TextUtils.isEmpty(text)) "Не указано"
            else "${text}$TRADING_CURRENCY"
        }

        // Меню ввода интервала вызова робота
        val mainInterval =
            findPreference<EditTextPreference>(getString(R.string.main_request_delay_min))
        mainInterval?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
        mainInterval?.summaryProvider = Preference.SummaryProvider<EditTextPreference> {
            val text = it.text
            if (TextUtils.isEmpty(text)) "Не указано"
            else "${text}мин"
        }

        // Меню ввода интервала, который считается началом торгов
        val startInterval =
            findPreference<EditTextPreference>(getString(R.string.recent_interval_min))
        startInterval?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
        startInterval?.summaryProvider = Preference.SummaryProvider<EditTextPreference> {
            val text = it.text
            if (TextUtils.isEmpty(text)) "Не указано"
            else "${text}мин"
        }

    }
}