package com.example.towardsgoalsapp.tasks

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import com.example.towardsgoalsapp.R


class TaskItemListAdapter(
    private val viewModel: TaskItemListViewModel
) : RecyclerView.Adapter<TaskItemListAdapter.ViewHolder>() {

    private val tasksMutablesList: List<MutableLiveData<TaskData>>
        = viewModel.taskDataList

    companion object {
        const val LOG_TAG = "TILAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.task_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        Log.i(LOG_TAG, "bind view called, position $position")

        val habitData: TaskData? = tasksMutablesList[position].value
        habitData?.run { holder.bind(this) }
    }

    override fun getItemCount(): Int = tasksMutablesList.size

    inner class ViewHolder(private val viewOfItem: View) : RecyclerView.ViewHolder(viewOfItem) {

        private val taskNameTextView: TextView = viewOfItem.findViewById(R.id.taskNameForItem)
        private val taskSubtasksCountView: TextView = viewOfItem.findViewById(R.id.taskSubtasksTextview)


        fun bind(taskData: TaskData) {
            taskNameTextView.text = taskData.taskName
            taskSubtasksCountView.text =
                viewOfItem.context
                    .getString(R.string.tasks_subtasks_beginning_text, taskData.subtasksCount)
        }

    }

}