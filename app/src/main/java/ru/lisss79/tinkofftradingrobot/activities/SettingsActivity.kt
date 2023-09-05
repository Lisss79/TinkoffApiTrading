package ru.lisss79.tinkofftradingrobot.activities

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*
import ru.lisss79.tinkofftradingrobot.PricePriority
import ru.lisss79.tinkofftradingrobot.R
import ru.lisss79.tinkofftradingrobot.SellingPriceHigher
import ru.lisss79.tinkofftradingrobot.TRADING_CURRENCY
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.ACCOUNTS
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys.ACCOUNTS_NAMES
import kotlin.system.exitProcess


var accounts: Array<String>? = null
var accountsNames: Array<String>? = null

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (intent.hasExtra(ACCOUNTS) && intent.hasExtra(ACCOUNTS_NAMES)) {
            accounts = intent.getStringArrayExtra(ACCOUNTS)
            accountsNames = intent.getStringArrayExtra(ACCOUNTS_NAMES)
        }

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
            // Меню ввода токена
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

            // Меню выбора инструмента
            val instrumentPref = findPreference<ListPreference>(getString(R.string.TICKER))
            if (instrumentPref?.value == null) instrumentPref?.setValueIndex(0)

            // Меню выбора аккаунта
            val accountPref = findPreference<ListPreference>(getString(R.string.ACCOUNT))
            if (accounts != null) {
                accountPref?.entryValues = accounts
                accountPref?.entries = accountsNames
            } else {
                accountPref?.entryValues = Array(0) { "Нет данных" }
                accountPref?.entries = Array(0) { "Нет данных" }
            }
            accountPref?.setOnPreferenceChangeListener { _, newValue ->
                val text = String.format("Id выбранного счета: %s", newValue)
                Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                false
            }

            // Меню выбора приоритета для торгового дня
            val tradingPref =
                findPreference<ListPreference>(getString(R.string.trading_day_priority))
            tradingPref?.entryValues = PricePriority.getEntries()
            tradingPref?.entries = PricePriority.getRusNames()
            if (tradingPref?.value == null)
                tradingPref?.setValueIndex(PricePriority.defaultValue.ordinal)

            // Меню выбора приоритета для вечерней сессии
            val tradingEveningPref =
                findPreference<ListPreference>(getString(R.string.trading_evening_priority))
            tradingEveningPref?.entryValues = PricePriority.getEntries()
            tradingEveningPref?.entries = PricePriority.getRusNames()
            if(tradingEveningPref?.value == null)
                tradingEveningPref?.setValueIndex(PricePriority.defaultValue.ordinal)

            // Меню выбора приоритета для начала торгового дня
            val startPref = findPreference<ListPreference>(getString(R.string.start_day_priority))
            startPref?.entryValues = PricePriority.getEntries()
            startPref?.entries = PricePriority.getRusNames()
            if(startPref?.value == null)
                startPref?.setValueIndex(PricePriority.defaultValue.ordinal)

            // Меню выбора приоритета для аукциона открытия
            val auctionPref = findPreference<ListPreference>(getString(R.string.auction_priority))
            auctionPref?.entryValues = PricePriority.getEntries()
            auctionPref?.entries = PricePriority.getRusNames()
            if (accountPref?.value == null)
                auctionPref?.setValueIndex(PricePriority.defaultValue.ordinal)

            // Меню выбора приоритета для иных случаев
            val otherPref = findPreference<ListPreference>(getString(R.string.other_priority))
            otherPref?.entryValues = PricePriority.getEntries()
            otherPref?.entries = PricePriority.getRusNames()
            if (otherPref?.value == null)
                otherPref?.setValueIndex(PricePriority.defaultValue.ordinal)

            // Меню выбора цены продажи, более высокой, чем цена покупки
            val sellingPricePref =
                findPreference<ListPreference>(getString(R.string.selling_price_higher))
            sellingPricePref?.entryValues = SellingPriceHigher.getEntries()
            sellingPricePref?.entries = SellingPriceHigher.getRusNames()
            if (sellingPricePref?.value == null)
                otherPref?.setValueIndex(SellingPriceHigher.defaultValue.ordinal)

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
            val mainInterval = findPreference<EditTextPreference>(getString(R.string.main_request_delay_min))
            mainInterval?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            mainInterval?.summaryProvider = Preference.SummaryProvider<EditTextPreference> {
                val text = it.text
                if (TextUtils.isEmpty(text)) "Не указано"
                else "${text}мин"
            }

            // Меню ввода интервала, который считается началом торгов
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