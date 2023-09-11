package ru.lisss79.tinkofftradingrobot.activities.fragments

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import ru.lisss79.tinkofftradingrobot.PricePriority
import ru.lisss79.tinkofftradingrobot.R

class PriceSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prices_preferences, rootKey)

        // Настраиваем меню настроек
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
        if (tradingEveningPref?.value == null)
            tradingEveningPref?.setValueIndex(PricePriority.defaultValue.ordinal)

        // Меню выбора приоритета для начала торгового дня
        val startPref = findPreference<ListPreference>(getString(R.string.start_day_priority))
        startPref?.entryValues = PricePriority.getEntries()
        startPref?.entries = PricePriority.getRusNames()
        if (startPref?.value == null)
            startPref?.setValueIndex(PricePriority.defaultValue.ordinal)

        // Меню выбора приоритета для аукциона открытия
        val auctionPref = findPreference<ListPreference>(getString(R.string.auction_priority))
        auctionPref?.entryValues = PricePriority.getEntries()
        auctionPref?.entries = PricePriority.getRusNames()
        if (auctionPref?.value == null)
            auctionPref?.setValueIndex(PricePriority.defaultValue.ordinal)

        // Меню выбора приоритета для иных случаев
        val otherPref = findPreference<ListPreference>(getString(R.string.other_priority))
        otherPref?.entryValues = PricePriority.getEntries()
        otherPref?.entries = PricePriority.getRusNames()
        if (otherPref?.value == null)
            otherPref?.setValueIndex(PricePriority.defaultValue.ordinal)

    }
}