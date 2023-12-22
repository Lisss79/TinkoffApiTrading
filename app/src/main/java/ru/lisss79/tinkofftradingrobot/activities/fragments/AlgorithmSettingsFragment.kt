package ru.lisss79.tinkofftradingrobot.activities.fragments

import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import ru.lisss79.tinkofftradingrobot.R
import ru.lisss79.tinkofftradingrobot.TRADING_CURRENCY
import ru.lisss79.tinkofftradingrobot.enums.MarketOrders
import ru.lisss79.tinkofftradingrobot.enums.SellingPriceHigher

class AlgorithmSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.algorithm_preferences, rootKey)

        // Меню выбора цены продажи, более высокой, чем цена покупки
        val sellingPrice =
            findPreference<ListPreference>(getString(R.string.selling_price_higher))
        sellingPrice?.entryValues = SellingPriceHigher.getEntries()
        sellingPrice?.entries = SellingPriceHigher.getRusNames()
        if (sellingPrice?.value == null)
            sellingPrice?.setValueIndex(SellingPriceHigher.defaultValue.ordinal)

        // Меню выставления рыночных заявок на покупку
        val marketOrders =
            findPreference<ListPreference>(getString(R.string.market_orders))
        marketOrders?.entryValues = MarketOrders.getEntries()
        marketOrders?.entries = MarketOrders.getRusNames()
        if (marketOrders?.value == null)
            marketOrders?.setValueIndex(MarketOrders.defaultValue.ordinal)

        // Меню ввода коэффициента повышенного спроса
        val increasedBidRatio =
            findPreference<EditTextPreference>(getString(R.string.increased_bid_ratio))
        increasedBidRatio?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

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