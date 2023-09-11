package ru.lisss79.tinkofftradingrobot

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference


class EditTimePreference(
    context: Context,
    attrs: AttributeSet? = null
) : DialogPreference(context, attrs) {
    init {
        dialogMessage = "123456"
    }


}
