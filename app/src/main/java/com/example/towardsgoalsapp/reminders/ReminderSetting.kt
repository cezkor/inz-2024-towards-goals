package com.example.towardsgoalsapp.reminders

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import com.example.towardsgoalsapp.OwnerType
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.database.TGDatabase
import com.example.towardsgoalsapp.impints.ImpIntItemList

class ReminderViewModel(ownerType: OwnerType, ownerId: Long): ViewModel() {

    // val reminderData ...

    fun putReminder() {}

}

class ReminderSetting : Fragment() {

    companion object {

        const val TYPE = "rstype"
        const val OWNER_ID = "oid_rs"
        const val EDITABLE = "rsedit"
        const val LOG_TAG = "RemSet"

        @JvmStatic
        fun newInstance(ownerType: OwnerType, id: Long, editable: Boolean) = ReminderSetting().apply {
            arguments?.putString(TYPE, ownerType.typeString)
            arguments?.putLong(OWNER_ID, id)
            arguments?.putBoolean(EDITABLE, editable)
        }

    }

    private lateinit var viewModel: ReminderViewModel

    private var editable: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            var otOrNull = OwnerType.from(it.getString(ImpIntItemList.OWNER_TYPE_OF_IMPINTS) ?: OwnerType.TYPE_NONE.typeString)
            if (otOrNull == null) otOrNull = OwnerType.TYPE_NONE
            val id: Long = it.getLong(OWNER_ID)
            viewModel = ReminderViewModel(otOrNull, id)
            editable = it.getBoolean(EDITABLE, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reminder_picker, container, false)
        // to do: logic for switch
        // especially: add observable event (live data event?) when activity ends
        ;
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

}