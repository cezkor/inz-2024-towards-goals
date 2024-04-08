package com.example.towardsgoalsapp.stats

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.stats.models.BooleanModelSetting
import com.example.towardsgoalsapp.stats.models.ModelSettingWithChoices
import com.example.towardsgoalsapp.stats.models.ModelSetting
import com.example.towardsgoalsapp.stats.models.WithHelpText


class NonScrollableLayoutManager(context: Context?) :
    LinearLayoutManager(context) {

    override fun canScrollVertically(): Boolean {
        return false
    }
}

class ModelSettingsAdapter(
    private val settingsSet: ArrayList<ModelSetting>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val LOG_TAG = "MSetAdapter"

        const val INVALID_TYPE: Int = -100
        const val CHOICE_TYPE: Int = 0
        const val BOOLEAN_TYPE: Int = 1
    }

    private var onSettingChanged: (() -> Unit)? = null

    fun setOnSettingChanged(func: () -> Unit) {
        onSettingChanged = func
    }

    private var onHelpRequested: ((String) -> Unit)? = null

    fun setOnHelpRequested(func: (String) -> Unit) {
        onHelpRequested = func
    }

    private fun handleHelpText(setting: ModelSetting, hButton: Button) {
        if (setting is WithHelpText) {
            val helpText = setting.getHelpText()
            if (helpText != null) {
                hButton.isEnabled = true
                hButton.text = "?"
                hButton.setOnClickListener { onHelpRequested?.invoke(
                    helpText
                ) }
                return
            }

        }
        // else
        hButton.isEnabled = false
        hButton.text = Constants.EMPTY_STRING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        Log.i(LOG_TAG, "view type: $viewType")
        return when(viewType) {
            CHOICE_TYPE -> ChoiceViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.model_setting_with_choices_item, parent, false))

            BOOLEAN_TYPE -> BooleanViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.model_setting_with_checkbox_item, parent, false))

            else -> throw IllegalArgumentException("No such viewType")
        }

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        Log.i(LOG_TAG, "bind view called, position $position")

        settingsSet.getOrNull(position)?.run {
            when(determineViewType(this)) {
                CHOICE_TYPE -> if (this is ModelSettingWithChoices<*>)
                                  (holder as ChoiceViewHolder).bind(this)
                BOOLEAN_TYPE -> if (this is BooleanModelSetting)
                                  (holder as BooleanViewHolder).bind(this)
                else -> { /* ignore */ }
            }
        }
    }

    override fun getItemCount(): Int = settingsSet.size

    override fun getItemViewType(position: Int): Int {
        val setting = settingsSet[position]
        return determineViewType(setting)
    }

    private fun determineViewType(setting: ModelSetting) : Int {
        if (setting is ModelSettingWithChoices<*>) return CHOICE_TYPE
        if (setting is BooleanModelSetting) return BOOLEAN_TYPE

        return INVALID_TYPE
    }

    inner class ChoiceViewHolder(viewOfItem: View) : ViewHolder(viewOfItem) {

        private val nameEditText: TextView = viewOfItem.findViewById(R.id.settingNameTextView)
        private val spinner: Spinner = viewOfItem.findViewById(R.id.choicesSpinner)
        private val helpButton: Button = viewOfItem.findViewById(R.id.showHelpButton)
        private val context = viewOfItem.context

        fun bind(setting: ModelSettingWithChoices<*>) {

            handleHelpText(setting, helpButton)

            nameEditText.text = setting.settingName

            // set label but do not trigger onSettingChanged
            var init = true
            spinner.setSelection(setting.choiceIdx)

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    Log.i(LOG_TAG, "before setting choice change: ${setting.choice}")
                    setting.setChoice(position)
                    Log.i(LOG_TAG, "after setting choice change: ${setting.choice}")
                    if (init)
                        init = false
                    else
                        onSettingChanged?.invoke()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // ignore
                }
            }

            val anAdapter = ArrayAdapter(context,
                android.R.layout.simple_spinner_item, setting.choicesNames)
            anAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = anAdapter

        }

    }

    inner class BooleanViewHolder(viewOfItem: View) : ViewHolder(viewOfItem) {

        private val nameEditText: TextView = viewOfItem.findViewById(R.id.settingNameTextView)
        private val helpButton: Button = viewOfItem.findViewById(R.id.showHelpButton)
        private val checkBox: CheckBox = viewOfItem.findViewById(R.id.checkbox)

        fun bind(setting: BooleanModelSetting) {

            handleHelpText(setting, helpButton)

            nameEditText.text = setting.settingName

            checkBox.setOnCheckedChangeListener { _, checked ->
                setting.setValue(checked)
                onSettingChanged?.invoke()
            }

        }

    }

}