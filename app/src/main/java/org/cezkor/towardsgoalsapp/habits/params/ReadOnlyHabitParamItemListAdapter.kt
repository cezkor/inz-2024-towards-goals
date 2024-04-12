package org.cezkor.towardsgoalsapp.habits.params

import android.graphics.Typeface
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.database.*


class ReadOnlyHabitParamItemListAdapter(
    private val viewModel: HabitParamItemListViewModel
) : RecyclerView.Adapter<ReadOnlyHabitParamItemListAdapter.ViewHolder>() {

    private val paramsMutablesList: List<MutableLiveData<HabitParameter>> = viewModel.habitParamList

    companion object {
        const val LOG_TAG = "ROHIPLA"
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.read_only_habit_parameter_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        Log.i(LOG_TAG, "bind view called, position $position")

        val habitParameter: HabitParameter? = paramsMutablesList[position].value
        habitParameter?.run { holder.bind(this) }
    }

    override fun getItemCount(): Int = paramsMutablesList.filter { p -> p.value != null }.size

    inner class ViewHolder(viewOfItem: View) : RecyclerView.ViewHolder(viewOfItem) {

        private val nameTextView: TextView = viewOfItem.findViewById(R.id.parameterNameTextView)
        private val unitTextView: TextView = viewOfItem.findViewById(R.id.parameterUnitTextView)
        private val targetValueTextView: TextView
            = viewOfItem.findViewById(R.id.parameterTargetValueTextView)


        fun bind(param: HabitParameter) {
            if (param.unit == null) {
                // by default there is text about having no unit
                unitTextView.setTypeface(null, Typeface.ITALIC)
            }
            else {
                unitTextView.text = param.unit
            }
            nameTextView.text = param.name
            targetValueTextView.text = param.targetVal.toString()
        }

    }

}