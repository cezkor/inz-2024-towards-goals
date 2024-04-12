package org.cezkor.towardsgoalsapp.tasks.details

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.database.TaskData
import org.cezkor.towardsgoalsapp.etc.EisenhowerTaskNameFixer

class EisenhowerMatrixListAdapter(
    private val viewModel: TaskEisenhowerMatrixViewModel,
    private val idxOfList : Int,
    private val taskDoneImage: Drawable?,
    private val taskFailedImage : Drawable?,
    private val taskWithSubtasksImage: Drawable?,
    private val doableTaskImage: Drawable?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val LOG_TAG = "EMLAdapter"

    }

    private var onItemClickListener: ((TaskData) -> Unit)? = null

    fun setOnItemClickListener(func: (TaskData) -> Unit) {
        onItemClickListener = func
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return TaskViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.task_in_matrix_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        Log.i(LOG_TAG, "bind view called, position $position")

        if (holder is TaskViewHolder) {
            val taskData = viewModel.taskMtArrays.getOrNull(idxOfList)?.getOrNull(position)?.value
            taskData?.run { holder.bind(this) }
        }
    }

    override fun getItemCount(): Int =
        viewModel.taskMtArrays.getOrNull(idxOfList)?.filter { p -> p.value != null }?.size ?: 0

    inner class TaskViewHolder(private val viewOfItem: View) : ViewHolder(viewOfItem) {

        private val nameTextView = viewOfItem.findViewById<TextView>(R.id.taskNameForItem)
        private val taskImageView = viewOfItem.findViewById<ImageView>(R.id.taskStatusImage)

        fun bind(taskData: TaskData) {
            val shortName = EisenhowerTaskNameFixer.fix(taskData.taskName)
            nameTextView.text = shortName
            if (taskData.subtasksCount > 0) {
                taskImageView.setImageDrawable(taskWithSubtasksImage)
            }
            else {
                if (taskData.taskDone) {
                    if (taskData.taskFailed)
                        taskImageView.setImageDrawable(taskFailedImage)
                    else
                        taskImageView.setImageDrawable(taskDoneImage)
                }
                else {
                    taskImageView.setImageDrawable(doableTaskImage)
                }
            }

            viewOfItem.setOnClickListener {
                onItemClickListener?.invoke(taskData)
            }
        }

    }

}