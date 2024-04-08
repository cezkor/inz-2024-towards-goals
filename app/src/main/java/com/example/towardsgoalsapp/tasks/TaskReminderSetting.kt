package com.example.towardsgoalsapp.tasks

import android.os.Bundle
import android.os.LocaleList
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.etc.OneTimeEvent
import com.example.towardsgoalsapp.etc.OneTimeHandleable
import com.example.towardsgoalsapp.etc.errors.ErrorHandling
import com.example.towardsgoalsapp.tasks.details.TaskDetailsViewModel
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat.CLOCK_24H
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale

class TaskReminderSetting : Fragment() {

    companion object {

        const val EDITABLE = "trsedit"
        const val LOG_TAG = "TRemSet"

        @JvmStatic
        fun newInstance(editable: Boolean) = TaskReminderSetting().apply {
            arguments = Bundle().apply {putBoolean(EDITABLE, editable)}
        }

    }

    private lateinit var viewModel: TaskDetailsViewModel

    private var editable: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_task_reminder_picker, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[TaskDetailsViewModel::class.java]

        viewModel.exceptionMutable.observe(viewLifecycleOwner) {
            ErrorHandling.showExceptionDialog(requireActivity(), it)
        }

        arguments?.let {
            editable = it.getBoolean(EDITABLE, false)
        }

        val remindSwitch: SwitchCompat = view.findViewById(R.id.remindSwitch)
        val instantShowingTV = view.findViewById<TextView>(R.id.instantShowingTV)
        val timePickerButton: Button = view.findViewById(R.id.reminderTimePickerButton)
        val datePickerButton: Button = view.findViewById(R.id.reminderDatePickerButton)

        if (! editable) {
            remindSwitch.isEnabled = false
        }
        timePickerButton.isEnabled = false
        datePickerButton.isEnabled = false

        remindSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (! editable) return@setOnCheckedChangeListener
            if (isChecked) {
                timePickerButton.isEnabled = true
                datePickerButton.isEnabled = true
                if (viewModel.reminderShouldBeRemoved)
                    viewModel.reminderShouldBeRemoved = false
            }
            else {
                timePickerButton.isEnabled = false
                datePickerButton.isEnabled = false
                if (! viewModel.reminderShouldBeRemoved && viewModel.reminderExisted.value == true)
                    viewModel.reminderShouldBeRemoved = true
            }
        }

        viewModel.currentlyRemindOn.observe(viewLifecycleOwner) {
            var textToSet = getString(R.string.reminders_not_set)
            if (it != null) {
                remindSwitch.isChecked = true
                val locale = Locale(LocaleList.getDefault().get(0).language)
                // format - date ' ' time
                val formatter = DateTimeFormatterBuilder()
                    .appendLocalized(FormatStyle.FULL, null)
                    .appendLiteral(' ')
                    .appendLocalized(null, FormatStyle.MEDIUM)
                    .toFormatter(locale)
                val localDate = LocalDateTime.ofInstant(it, ZoneId.of("UTC"))
                textToSet = localDate.format(formatter)
            }
            else remindSwitch.isChecked = false
            instantShowingTV.text = textToSet
        }

        val timePicker = MaterialTimePicker.Builder().setTimeFormat(CLOCK_24H).build()
        timePicker.addOnPositiveButtonClickListener {
            if (! timePickerButton.isEnabled) return@addOnPositiveButtonClickListener
            if (! remindSwitch.isChecked) return@addOnPositiveButtonClickListener

            val hour = timePicker.hour
            val minute = timePicker.minute

            val newInstant : Instant = if (viewModel.currentlyRemindOn.value == null) {
                val localDate = LocalDate.now()
                val localTime = LocalTime.of(hour, minute, 0, 0)
                LocalDateTime.of(localDate, localTime).atZone(ZoneId.systemDefault())
                    .toInstant()
            } else {
                val curRemindOn = LocalDateTime.ofInstant(viewModel.currentlyRemindOn.value!!,
                    ZoneId.systemDefault())
                LocalDateTime.of(
                    curRemindOn.toLocalDate(),
                    LocalTime.of(hour, minute, 0, 0)
                ).atZone(ZoneId.systemDefault()).toInstant()
            }

            // user picked time in past
            if (newInstant < Instant.now()) {
                ErrorHandling.showThrowableAsToast(
                    requireActivity(),
                    Throwable(getString(R.string.reminders_cant_set_before))
                )
                return@addOnPositiveButtonClickListener
            }

            viewModel.assureOfExistingTaskHandleableMutable.value = OneTimeHandleable {
                lifecycleScope.launch(viewModel.exceptionHandler) {
                    val maybeInstant = viewModel.setReminder(newInstant) // returns a pair
                    // instant?, null because represents past
                    if (maybeInstant.first == null)
                        ErrorHandling.showThrowableAsToast(
                            requireActivity(),
                            Throwable(
                                if (maybeInstant.second)
                                    getString(R.string.reminders_cant_set_before)
                                else
                                    getString(R.string.reminders_unable_to_set_reminder)
                            )
                        )
                    else
                        viewModel.currentlyRemindOn.value = maybeInstant.first
                }
            }
        }
        timePickerButton.setOnClickListener {
            val tabIdx = viewModel.currentTabIdx
            timePicker.show(requireActivity().supportFragmentManager, null)
            viewModel.currentTabIdx = tabIdx
            viewModel.setTabEvent.value = OneTimeEvent()
        }
        // single date picker
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())
            .build()
        val datePicker: MaterialDatePicker<Long> = MaterialDatePicker.Builder.datePicker()
            .setCalendarConstraints(constraints)
            .build()
        datePicker.addOnPositiveButtonClickListener {
            if (! timePickerButton.isEnabled) return@addOnPositiveButtonClickListener
            if (! remindSwitch.isChecked) return@addOnPositiveButtonClickListener

            val selection: Long = datePicker.selection ?: return@addOnPositiveButtonClickListener

            val newInstant : Instant = if (viewModel.currentlyRemindOn.value == null) {
                val instant = Instant.ofEpochMilli(selection)
                var localDate = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate()
                val localTimeNow = LocalTime.now()
                if (localTimeNow.hour == 23) localDate = localDate.plusDays(1)
                val localTime = localTimeNow.plusHours(1)
                LocalDateTime.of(localDate, localTime)
                    .toInstant(ZoneOffset.UTC)
            } else {
                val curRemindOn = LocalDateTime.ofInstant(viewModel.currentlyRemindOn.value!!,
                    ZoneId.of("UTC"))
                val instant = Instant.ofEpochMilli(selection)
                LocalDateTime.of(
                    LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate(),
                    curRemindOn.toLocalTime()
                ).toInstant(ZoneOffset.UTC)
            }

            if (newInstant < Instant.now()) {
                ErrorHandling.showThrowableAsToast(
                    requireActivity(),
                    Throwable(getString(R.string.reminders_cant_set_before))
                )
                return@addOnPositiveButtonClickListener
            }

            viewModel.assureOfExistingTaskHandleableMutable.value = OneTimeHandleable {
                lifecycleScope.launch(viewModel.exceptionHandler) {
                    val maybeInstant : Pair<Instant?, Boolean> =
                        viewModel.setReminder(newInstant) // returns a pair
                    // instant?, will become null if wasn't set
                    if (maybeInstant.first == null)
                        ErrorHandling.showThrowableAsToast(
                            requireActivity(),
                            Throwable(
                                if (maybeInstant.second)
                                    getString(R.string.reminders_cant_set_before)
                                else
                                    getString(R.string.reminders_unable_to_set_reminder)
                            )
                        )
                    else
                        viewModel.currentlyRemindOn.value = maybeInstant.first
                }
            }
        }
        datePickerButton.setOnClickListener {
            val tabIdx = viewModel.currentTabIdx
            datePicker.show(requireActivity().supportFragmentManager, null)
            viewModel.currentTabIdx = tabIdx
            viewModel.setTabEvent.value = OneTimeEvent()
        }

        remindSwitch.isChecked = (viewModel.currentlyRemindOn.value != null)

    }


}