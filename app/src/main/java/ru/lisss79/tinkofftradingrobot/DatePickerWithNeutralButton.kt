package ru.lisss79.tinkofftradingrobot

import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import android.widget.FrameLayout
import androidx.fragment.app.FragmentManager
import com.google.android.material.datepicker.MaterialDatePicker

class DatePickerWithNeutralButton(
    val materialDatePicker: MaterialDatePicker<androidx.core.util.Pair<Long, Long>>,
    val onClickListener: View.OnClickListener
) {
    private lateinit var neutralButton: Button

    init {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            val view = materialDatePicker.dialog?.window?.decorView
            val context = materialDatePicker.context
            val params =
                FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            params.gravity = Gravity.BOTTOM or Gravity.START

            val valueAttr = TypedValue()
            context?.theme?.resolveAttribute(
                android.R.attr.selectableItemBackground,
                valueAttr,
                true
            )

            neutralButton = Button(
                context,
                null,
                androidx.appcompat.R.attr.buttonBarPositiveButtonStyle
            ).apply {
                text = "Сброс"
                setBackgroundResource(valueAttr.resourceId)
                layoutParams = params
                setOnClickListener {
                    onClickListener.onClick(it)
                    materialDatePicker.dismiss()
                }
            }
            (view as FrameLayout).addView(neutralButton)
        }, 50)
    }

    fun show(supportFragmentManager: FragmentManager, tag: String) {
        materialDatePicker.show(supportFragmentManager, tag)
    }
}