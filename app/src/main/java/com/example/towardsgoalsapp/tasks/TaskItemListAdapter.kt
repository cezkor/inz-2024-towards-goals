package com.example.towardsgoalsapp.tasks

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.database.*


class TaskItemListAdapter(
    private val viewModel: TaskItemListViewModel,
    private val taskDoneImage: Drawable?,
    private val taskFailedImage : Drawable?,
    private val taskWithSubtasksImage: Drawable?,
    private val editUnfinishedImage: Drawable?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val LOG_TAG = "TILAdapter"
        private const val DOABLE_TASK_ID: Int = 0
        private const val TASK_WITH_SUBTASKS_ID: Int = 1
    }

    private var onItemClickListener: ((TaskData) -> Unit)? = null
    private var onDoTaskButtonClickListener: ((TaskData) -> Unit)? = null

    fun setOnItemClickListener(listener: (TaskData) -> Unit) {
        this.onItemClickListener = listener
    }

    fun setOnDoTaskButtonClickListener(listener: (TaskData) -> Unit) {
        this.onDoTaskButtonClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when(viewType) {
            TASK_WITH_SUBTASKS_ID -> TaskWithSubtasksViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.task_with_subtasks_list_item, parent, false)
            )
            else -> DoableTaskViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.directly_doable_task_list_item, parent, false)
            )
        }
    }

    private fun determineViewType(taskData: TaskData?): Int {
        return if (taskData == null || taskData.subtasksCount == 0L) DOABLE_TASK_ID
        else TASK_WITH_SUBTASKS_ID
    }

    override fun getItemViewType(position: Int): Int
        = determineViewType(viewModel.taskDataList?.get(position)?.value)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        Log.i(LOG_TAG, "bind view called, position $position")

        val taskData: TaskData? = viewModel.taskDataList?.get(position)?.value
        taskData?.run {  when (determineViewType(this)) {
            TASK_WITH_SUBTASKS_ID ->
                (holder as TaskWithSubtasksViewHolder).bind(this)
            else -> (holder as DoableTaskViewHolder).bind(this)
        } }
    }

    override fun getItemCount(): Int
        = viewModel.taskDataList?.filter { p -> p.value != null }?.size ?: 0

    inner class DoableTaskViewHolder(private val viewOfItem: View) : RecyclerView.ViewHolder(viewOfItem) {

        private val taskNameTextView: TextView = viewOfItem.findViewById(R.id.taskNameForItem)
        private val taskProgress: ProgressBar = viewOfItem.findViewById(R.id.taskItemProgress)
        private val doButton: ImageButton = viewOfItem.findViewById(R.id.taskDoButton)
        private val editUnfinishedImageView: ImageView = viewOfItem.findViewById(R.id.editUnfinishedImageView)

        fun bind(taskData: TaskData) {
            if (taskData.taskEditUnfinished) {
                editUnfinishedImage?.run { editUnfinishedImageView.setImageDrawable(this) }
            }
            viewOfItem.setOnClickListener {
                onItemClickListener?.invoke(taskData)
            }
            if (taskData.taskDone) {
                doButton.isEnabled = false

                if (taskData.taskFailed)
                    taskFailedImage?.run { doButton.setImageDrawable(this) }
                else
                    taskDoneImage?.run { doButton.setImageDrawable(this) }

            } else {
                doButton.setOnClickListener {
                    onDoTaskButtonClickListener?.invoke(taskData)
                }
            }
            taskNameTextView.text = taskData.taskName
            taskProgress.progress = (100 * taskData.taskProgress).toInt()
        }

    }

    inner class TaskWithSubtasksViewHolder(private val viewOfItem: View) : RecyclerView.ViewHolder(viewOfItem) {

        private val taskNameTextView: TextView = viewOfItem.findViewById(R.id.taskNameForItem)
        private val taskSubtasksCountView: TextView = viewOfItem.findViewById(R.id.taskSubtasksTextview)
        private val taskProgress: ProgressBar = viewOfItem.findViewById(R.id.taskItemProgress)
        private val taskDoneImageView: ImageView = viewOfItem.findViewById(R.id.taskStatusImage)
        private val editUnfinishedImageView: ImageView = viewOfItem.findViewById(R.id.editUnfinishedImageView)

        fun bind(taskData: TaskData) {

            if (taskData.taskEditUnfinished) {
                editUnfinishedImage?.run { editUnfinishedImageView.setImageDrawable(this) }
            }

            if (taskData.taskDone) {
                if (taskData.taskFailed)
                    taskFailedImage?.run { taskDoneImageView.setImageDrawable(this) }
                else
                    taskDoneImage?.run { taskDoneImageView.setImageDrawable(this) }
            }
            else {
                taskWithSubtasksImage?.run { taskDoneImageView.setImageDrawable(this) }
            }

            viewOfItem.setOnClickListener {
                onItemClickListener?.invoke(taskData)
            }
            taskNameTextView.text = taskData.taskName
            taskSubtasksCountView.text =
                viewOfItem.context
                    .getString(R.string.tasks_subtasks_beginning_text, taskData.subtasksCount)
            taskProgress.progress = (100 * taskData.taskProgress).toInt()
        }

    }

}