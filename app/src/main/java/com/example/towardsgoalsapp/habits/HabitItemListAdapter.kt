package com.example.towardsgoalsapp.habits

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import com.example.towardsgoalsapp.R


class HabitItemListAdapter(
    private val viewModel: HabitItemListViewModel
) : RecyclerView.Adapter<HabitItemListAdapter.ViewHolder>() {

    private val habitsMutablesList: List<MutableLiveData<HabitData_OLD>> = viewModel.habitDataList

    companion object {
        const val LOG_TAG = "HILAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.habit_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        Log.i(LOG_TAG, "bind view called, position $position")

        val habitData: HabitData_OLD? = habitsMutablesList[position].value
        habitData?.run { holder.bind(this) }
    }

    override fun getItemCount(): Int = habitsMutablesList.size

    inner class ViewHolder(viewOfItem: View) : RecyclerView.ViewHolder(viewOfItem) {

        private val habitNameTextView: TextView = viewOfItem.findViewById(R.id.habitNameForItem)

        fun bind(habitData: HabitData_OLD) {
            habitNameTextView.text = habitData.habitName
        }

    }

}