package com.example.towardsgoalsapp.habits.params

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.database.*
import com.example.towardsgoalsapp.etc.ParamUnitFixer
class EditableHabitParamItemListAdapter(
    private val viewModel: HabitParamItemListViewModel
) : RecyclerView.Adapter<EditableHabitParamItemListAdapter.ViewHolder>() {

    private val paramsMutablesList: List<MutableLiveData<HabitParameter>> = viewModel.habitParamList

    companion object {
        const val LOG_TAG = "EHIPLA"
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.editable_habit_parameter_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        Log.i(LOG_TAG, "bind view called, position $position")

        val habitParameter: HabitParameter? = paramsMutablesList[position].value
        habitParameter?.run { holder.bind(this, position) }
    }

    override fun getItemCount(): Int = paramsMutablesList.filter { p -> p.value != null }.size

    inner class ViewHolder(viewOfItem: View) : RecyclerView.ViewHolder(viewOfItem) {

        private val nameEditText: EditText = viewOfItem.findViewById(R.id.parameterNameEditText)
        private val unitEditText: EditText = viewOfItem.findViewById(R.id.parameterUnitEditText)
        private val targetValueEditText: EditText
            = viewOfItem.findViewById(R.id.parameterTargetValueEditText)
        private val deleteButton: ImageButton = viewOfItem.findViewById(R.id.deleteParameterItem)

        fun bind(param: HabitParameter, position: Int) {

            nameEditText.setText(param.name)
            param.unit?.run { unitEditText.setText(this) }
            targetValueEditText.setText(param.targetVal.toString())

            fun save() {
                val editedNameText = nameEditText.text.toString()
                val editedUnitText = unitEditText.text.toString()
                val editedNumberText = targetValueEditText.text.toString()
                viewModel.putOneHabitParamAt(
                    editedNameText,
                    editedNumberText,
                    editedUnitText,
                    position
                )
            }

            nameEditText.setOnFocusChangeListener { v, hasFocus ->
                if (! hasFocus) save()
            }
            unitEditText.setOnFocusChangeListener { v, hasFocus ->
                if (! hasFocus) save()
            }
            targetValueEditText.setOnFocusChangeListener { v, hasFocus ->
                if (! hasFocus) save()
            }
            nameEditText.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    save()
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }
            unitEditText.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    save()
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }
            targetValueEditText.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    save()
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }

            deleteButton.setOnClickListener{
                viewModel.deleteParam(param.paramId)
            }

        }

    }

}