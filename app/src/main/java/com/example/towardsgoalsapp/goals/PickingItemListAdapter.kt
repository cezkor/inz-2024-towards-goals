package com.example.towardsgoalsapp.goals

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.database.*
import com.example.towardsgoalsapp.etc.NameFixer

class PickingItemListAdapter(
    private val list: ArrayList<HabitData>,
    private val markableImage: Drawable?,
    private val completedHabitMarkImage: Drawable?
) : RecyclerView.Adapter<PickingItemListAdapter.ViewHolder>() {

    companion object {
        const val LOG_TAG = "PILAdapter"
    }

    private var onItemClickListener: ((HabitData?) -> Unit)? = null

    fun setOnItemClickListener(listener: (HabitData?) -> Unit) {
        this.onItemClickListener = listener
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.picking_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        Log.i(LOG_TAG, "bind view called, position $position")

        val data = list[position]
        data?.run { holder.bind(this) }
    }

    override fun getItemCount(): Int = list.size

    inner class ViewHolder(private val viewOfItem: View) : RecyclerView.ViewHolder(viewOfItem) {

        private val nameTextView: TextView = viewOfItem.findViewById(R.id.nameItem)
        private val statusImageView: ImageView = viewOfItem.findViewById(R.id.statusImage)

        fun bind(data: HabitData) {
            nameTextView.text = NameFixer.fix(data.habitName)
            if (data.habitTargetCompleted)
                completedHabitMarkImage?.run { statusImageView.setImageDrawable(this) }
            else markableImage?.run { statusImageView.setImageDrawable(this) }
            viewOfItem.setOnClickListener {
                onItemClickListener?.invoke(data)
            }
        }

    }

}