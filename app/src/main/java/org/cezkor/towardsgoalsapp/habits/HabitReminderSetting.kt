package org.cezkor.towardsgoalsapp.habits

import android.os.Bundle
import android.os.LocaleList
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.etc.OneTimeEvent
import org.cezkor.towardsgoalsapp.etc.errors.ErrorHandling
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat.CLOCK_24H
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale

class HabitReminderSetting : Fragment() {

    companion object {

        const val EDITABLE = "hrsedit"
        const val LOG_TAG = "HRemSet"

        @JvmStatic
        fun newInstance(editable: Boolean) = HabitReminderSetting().apply {
            arguments = Bundle().apply {putBoolean(EDITABLE, editable)}
        }

    }

    private lateinit var viewModel: HabitViewModel

    private var editable: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_habit_reminder_picker, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[HabitViewModel::class.java]

        arguments?.let {
            editable = it.getBoolean(EDITABLE, false)
        }

        val remindSwitch: SwitchCompat = view.findViewById(R.id.remindSwitch)
        val instantShowingTV = view.findViewById<TextView>(R.id.instantShowingTV)
        val timePickerButton: Button = view.findViewById(R.id.reminderTimePickerButton)

        if (! editable) {
            remindSwitch.isEnabled = false
        }
        timePickerButton.isEnabled = false

        if (! remindSwitch.isChecked) timePickerButton.isEnabled = false
        remindSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (! editable) return@setOnCheckedChangeListener
            if (isChecked) {
                timePickerButton.isEnabled = true
                if (viewModel.reminderShouldBeRemoved)
                    viewModel.reminderShouldBeRemoved = false
            }
            else {
                timePickerButton.isEnabled = false
                if (! viewModel.reminderShouldBeRemoved && viewModel.reminderExisted.value == true)
                    viewModel.reminderShouldBeRemoved = true
            }
        }

        viewModel.currentlyRemindOn.observe(viewLifecycleOwner) {
            var textToSet = getString(R.string.reminders_not_set)
            if (it != null) {
                val locale = Locale(LocaleList.getDefault().get(0).language)
                // format - date ' ' time
                val formatter = DateTimeFormatterBuilder()
                    .appendLocalized(FormatStyle.FULL, null)
                    .appendLiteral(' ')
                    .appendLocalized(null, FormatStyle.MEDIUM)
                    .toFormatter(locale)
                val localDate = LocalDateTime.ofInstant(it, ZoneId.systemDefault())
                textToSet = localDate.format(formatter)
            }
            instantShowingTV.text = textToSet
        }

        val timePicker = MaterialTimePicker.Builder().setTimeFormat(CLOCK_24H).build()
        timePicker.addOnPositiveButtonClickListener {
            if (! timePickerButton.isEnabled) return@addOnPositiveButtonClickListener
            if (! remindSwitch.isChecked) return@addOnPositiveButtonClickListener

            val hour = timePicker.hour
            val minute = timePicker.minute
            val lTime = LocalTime.of(hour, minute, 0, 0)

            lifecycleScope.launch(viewModel.exceptionHandler) {
                val maybeInstant = viewModel.setReminder(lTime)
                if (maybeInstant == null) {
                    ErrorHandling.showThrowableAsToast(
                        requireActivity(),
                        Throwable(getString(R.string.reminders_unable_to_set_reminder))
                    )
                    return@launch
                }
                viewModel.currentlyRemindOn.value = maybeInstant
            }
        }
        timePickerButton.setOnClickListener {
            val tabIdx = viewModel.currentTabIdx
            timePicker.show(requireActivity().supportFragmentManager, null)
            viewModel.currentTabIdx = tabIdx
            viewModel.setTabEvent.value = OneTimeEvent()
        }



    }


}