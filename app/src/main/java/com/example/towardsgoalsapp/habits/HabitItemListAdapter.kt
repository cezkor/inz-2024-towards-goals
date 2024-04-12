package com.example.towardsgoalsapp.habits

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.database.*
import java.time.LocalDateTime
import java.time.ZoneId


class HabitItemListAdapter(
    private val viewModel: HabitItemListViewModel,
    private val habitMarkImage: Drawable?,
    private val completedHabitMarkImage: Drawable?,
    private val editUnfinishedImage: Drawable?,
    private val notMarkableHabitImage: Drawable?
) : RecyclerView.Adapter<HabitItemListAdapter.ViewHolder>() {

    private val habitsMutablesList: List<MutableLiveData<HabitData>> = viewModel.habitDataList

    companion object {
        const val LOG_TAG = "HILAdapter"
    }

    private var onItemClickListener: ((HabitData) -> Unit)? = null
    private var onHabitMarkButtonClickListener: ((HabitData) -> Unit)? = null

    fun setOnItemClickListener(listener: (HabitData) -> Unit) {
        this.onItemClickListener = listener
    }

    fun setOnHabitMarkButtonClickListener(listener: (HabitData) -> Unit) {
        this.onHabitMarkButtonClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.habit_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        Log.i(LOG_TAG, "bind view called, position $position")

        val habitData: HabitData? = habitsMutablesList[position].value
        habitData?.run { holder.bind(this) }
    }

    override fun getItemCount(): Int = habitsMutablesList.filter { p -> p.value != null }.size

    inner class ViewHolder(private val viewOfItem: View) : RecyclerView.ViewHolder(viewOfItem) {

        private val habitNameTextView: TextView = viewOfItem.findViewById(R.id.habitNameForItem)
        private val habitMarkButton: ImageButton = viewOfItem.findViewById(R.id.habitMarkButton)
        private val unfinishedImageView: ImageView = viewOfItem.findViewById(R.id.editUnfinishedImageView)

        fun bind(habitData: HabitData) {
            if (habitData.habitEditUnfinished)
                editUnfinishedImage?.run { unfinishedImageView.setImageDrawable(this) }

            if (habitData.habitTargetCompleted)
                completedHabitMarkImage?.run { habitMarkButton.setImageDrawable(this) }
            else
                habitMarkImage?.run { habitMarkButton.setImageDrawable(this) }

            viewOfItem.setOnClickListener { onItemClickListener?.invoke(habitData) }

            val canMarkHabit = HabitLogic.checkIfHabitIsMarkable(habitData.habitLastMarkedOn)
            if (canMarkHabit) {
                habitMarkButton.isEnabled = true
                habitMarkButton.setOnClickListener{
                    onHabitMarkButtonClickListener?.invoke(habitData) }
            }
            else {
                habitMarkButton.isEnabled = false
                notMarkableHabitImage?.run { habitMarkButton.setImageDrawable(this) }
            }

            habitNameTextView.text = habitData.habitName
        }

    }

}