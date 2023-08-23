package ru.lisss79.tinkoffapitrading.activities

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*
import ru.lisss79.tinkoffapitrading.PricePriority
import ru.lisss79.tinkoffapitrading.R
import ru.lisss79.tinkoffapitrading.SellingPriceHigher
import ru.lisss79.tinkoffapitrading.TRADING_CURRENCY
import ru.lisss79.tinkoffapitrading.queries_and_responses.JsonKeys.ACCOUNTS
import kotlin.system.exitProcess


var accounts: Array<String>? = null

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (intent.hasExtra(ACCOUNTS)) accounts = intent.getStringArrayExtra(ACCOUNTS)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            // Настраиваем меню настроек
            val tokenPref = findPreference<EditTextPreference>(getString(R.string.TOKEN))
            tokenPref?.setOnPreferenceChangeListener { _, _ ->
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Внимание!")
                    .setMessage("Из-за изменения токена необходимо перезапустить настройки вручную")
                    .setPositiveButton("OK") { _,  _ -> exitProcess(0) }
                    .setCancelable(false)
                    .show()
                true
            }
            tokenPref?.summaryProvider = Preference.SummaryProvider<EditTextPreference> {
                val text = it.text
                if (TextUtils.isEmpty(text)) "Не задан"
                else "Установлен"
            }

            val accountPref = findPreference<ListPreference>(getString(R.string.ACCOUNT))
            if (accounts != null) {
                accountPref?.entryValues = accounts
                accountPref?.entries = accounts
            }
            else {
                accountPref?.entryValues = Array(0) {"Нет данных"}
                accountPref?.entries = Array(0) {"Нет данных"}
            }

            val tradingPref = findPreference<ListPreference>(getString(R.string.trading_day_priority))
            tradingPref?.entryValues = PricePriority.getEntries()
            tradingPref?.entries = PricePriority.getRusNames()
            if(tradingPref?.value == null)
                tradingPref?.setValueIndex(PricePriority.defaultValue.ordinal)

            val tradingEveningPref =
                findPreference<ListPreference>(getString(R.string.trading_evening_priority))
            tradingEveningPref?.entryValues = PricePriority.getEntries()
            tradingEveningPref?.entries = PricePriority.getRusNames()
            if(tradingEveningPref?.value == null)
                tradingEveningPref?.setValueIndex(PricePriority.defaultValue.ordinal)

            val startPref = findPreference<ListPreference>(getString(R.string.start_day_priority))
            startPref?.entryValues = PricePriority.getEntries()
            startPref?.entries = PricePriority.getRusNames()
            if(startPref?.value == null)
                startPref?.setValueIndex(PricePriority.defaultValue.ordinal)

            val auctionPref = findPreference<ListPreference>(getString(R.string.auction_priority))
            auctionPref?.entryValues = PricePriority.getEntries()
            auctionPref?.entries = PricePriority.getRusNames()
            if (accountPref?.value == null)
                auctionPref?.setValueIndex(PricePriority.defaultValue.ordinal)

            val otherPref = findPreference<ListPreference>(getString(R.string.other_priority))
            otherPref?.entryValues = PricePriority.getEntries()
            otherPref?.entries = PricePriority.getRusNames()
            if (otherPref?.value == null)
                otherPref?.setValueIndex(PricePriority.defaultValue.ordinal)

            val sellingPricePref =
                findPreference<ListPreference>(getString(R.string.selling_price_higher))
            sellingPricePref?.entryValues = SellingPriceHigher.getEntries()
            sellingPricePref?.entries = SellingPriceHigher.getRusNames()
            if (sellingPricePref?.value == null)
                otherPref?.setValueIndex(SellingPriceHigher.defaultValue.ordinal)

            val replaceOrder =
                findPreference<CheckBoxPreference>(getString(R.string.replace_order_enabled))
            val replaceOrderUp =
                findPreference<CheckBoxPreference>(getString(R.string.replace_order_up))

            replaceOrder?.setOnPreferenceChangeListener { _, newValue ->
                replaceOrderUp?.isEnabled = newValue as Boolean
                true
            }
            if (replaceOrder?.isChecked == true) replaceOrderUp?.isEnabled = true

            val minMoney = findPreference<EditTextPreference>(getString(R.string.money_after_spent))
            minMoney?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            minMoney?.summaryProvider = Preference.SummaryProvider<EditTextPreference> {
                val text = it.text
                if (TextUtils.isEmpty(text)) "Не указано"
                else "${text}$TRADING_CURRENCY"
            }

            val mainInterval = findPreference<EditTextPreference>(getString(R.string.main_request_delay_min))
            mainInterval?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            mainInterval?.summaryProvider = Preference.SummaryProvider<EditTextPreference> {
                val text = it.text
                if (TextUtils.isEmpty(text)) "Не указано"
                else "${text}мин"
            }

            val startInterval = findPreference<EditTextPreference>(getString(R.string.recent_interval_min))
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
}