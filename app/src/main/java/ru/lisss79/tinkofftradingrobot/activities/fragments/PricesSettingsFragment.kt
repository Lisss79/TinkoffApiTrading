package ru.lisss79.tinkofftradingrobot.activities.fragments

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import ru.lisss79.tinkofftradingrobot.PercentPreference
import ru.lisss79.tinkofftradingrobot.PricePriorityWithData
import ru.lisss79.tinkofftradingrobot.PricePriorityWithData.PricePriority
import ru.lisss79.tinkofftradingrobot.R

class PriceSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prices_preferences, rootKey)

        // Настраиваем меню настроек
        // Меню выбора приоритета для торгового дня
        findPreference<ListPreference>(getString(R.string.trading_day_priority))?.also { tradingPref ->
            tradingPref.entryValues = PricePriority.getEntries()
            tradingPref.entries = PricePriority.getRusNames()
            if (tradingPref.value == null)
                tradingPref.setValueIndex(PricePriority.defaultValue.ordinal)

            val tradingDataPref =
                findPreference<PercentPreference>(getString(R.string.trading_day_priority_data))
            showDataPreference(tradingPref, tradingDataPref)
            tradingPref.setOnPreferenceChangeListener { _, newValue ->
                showDataPreference(tradingPref, tradingDataPref, newValue)
                true
            }
        }

        // Меню выбора приоритета для вечерней сессии
        findPreference<ListPreference>(getString(R.string.trading_evening_priority))?.also { tradingEveningPref ->
            tradingEveningPref.entryValues = PricePriority.getEntries()
            tradingEveningPref.entries = PricePriority.getRusNames()
            if (tradingEveningPref.value == null)
                tradingEveningPref.setValueIndex(PricePriority.defaultValue.ordinal)

            val tradingEveningDataPref =
                findPreference<PercentPreference>(getString(R.string.trading_evening_priority_data))
            showDataPreference(tradingEveningPref, tradingEveningDataPref)
            tradingEveningPref.setOnPreferenceChangeListener { _, newValue ->
                showDataPreference(tradingEveningPref, tradingEveningDataPref, newValue)
                true
            }
        }

        // Меню выбора приоритета для начала торгового дня
        findPreference<ListPreference>(getString(R.string.start_day_priority))?.also { startPref ->
            startPref.entryValues = PricePriority.getEntries()
            startPref.entries = PricePriority.getRusNames()
            if (startPref.value == null)
                startPref.setValueIndex(PricePriority.defaultValue.ordinal)

            val startDataPref =
                findPreference<PercentPreference>(getString(R.string.start_day_priority_data))
            showDataPreference(startPref, startDataPref)
            startPref.setOnPreferenceChangeListener { _, newValue ->
                showDataPreference(startPref, startDataPref, newValue)
                true
            }
        }


        // Меню выбора приоритета для аукциона открытия
        findPreference<ListPreference>(getString(R.string.auction_priority))?.also { auctionPref ->
            auctionPref.entryValues = PricePriority.getEntries()
            auctionPref.entries = PricePriority.getRusNames()
            if (auctionPref.value == null)
                auctionPref.setValueIndex(PricePriority.defaultValue.ordinal)

            val auctionDataPref =
                findPreference<PercentPreference>(getString(R.string.auction_priority_data))
            showDataPreference(auctionPref, auctionDataPref)
            auctionPref.setOnPreferenceChangeListener { _, newValue ->
                showDataPreference(auctionPref, auctionDataPref, newValue)
                true
            }
        }

        // Меню выбора приоритета для иных случаев
        findPreference<ListPreference>(getString(R.string.other_priority))?.also { otherPref ->
            otherPref.entryValues = PricePriority.getEntries()
            otherPref.entries = PricePriority.getRusNames()
            if (otherPref.value == null)
                otherPref.setValueIndex(PricePriority.defaultValue.ordinal)

            val otherDataPref =
                findPreference<PercentPreference>(getString(R.string.other_priority_data))
            showDataPreference(otherPref, otherDataPref)
            otherPref.setOnPreferenceChangeListener { _, newValue ->
                showDataPreference(otherPref, otherDataPref, newValue)
                true
            }
        }

    }

    /**
     * Показывает, если необходимо, настройку выбора допуска цены
     */
    private fun showDataPreference(
        pref: ListPreference,
        dataPref: PercentPreference?,
        newValue: Any = ""
    ) {

        val enumValue = if ((newValue as String).isEmpty()) PricePriority.valueOf(pref.value)
        else PricePriority.valueOf(newValue)
        when (enumValue) {
            PricePriority.PRIORITY_PRICE_ORDER_BOOK_WITH_QUANTITY_TOLERANCE -> {
                dataPref?.apply {
                    title = "Допуск объема"
                    isVisible = true
                    if (value == 0f) value = PricePriorityWithData.getDefaultData(
                        PricePriority.PRIORITY_PRICE_ORDER_BOOK_WITH_QUANTITY_TOLERANCE
                    )
                }

            }
            PricePriority.PRIORITY_RECENT_PRICE -> {
                dataPref?.apply {
                    title = "Допуск цены"
                    isVisible = true
                    if (value == 0f) value = PricePriorityWithData.getDefaultData(
                        PricePriority.PRIORITY_RECENT_PRICE
                    )
                }
            }
            else -> {
                dataPref?.isVisible = false
            }
        }

    }
}