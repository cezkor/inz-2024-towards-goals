package com.example.towardsgoalsapp.reminders

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.OwnerType
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.goals.GoalSynopsisViewModel
import com.example.towardsgoalsapp.goals.GoalViewModel

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
            val typeStr: String = it.getString(TYPE)?: OwnerType.TYPE_NONE.typeString
            val id: Long = it.getLong(OWNER_ID)
            viewModel = ReminderViewModel(OwnerType.valueOf(typeStr), id)
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