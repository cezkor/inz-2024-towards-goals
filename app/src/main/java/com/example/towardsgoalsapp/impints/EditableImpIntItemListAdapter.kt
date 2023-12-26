package com.example.towardsgoalsapp.impints

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.lifecycle.MutableLiveData
import com.example.towardsgoalsapp.R


class EditableImpIntItemListAdapter(
    private val viewModel: ImpIntItemListViewModel
) : RecyclerView.Adapter<EditableImpIntItemListAdapter.ViewHolder>() {

    private val mutableImpIntsList: List<MutableLiveData<ImpIntData_OLD>> =
        viewModel.impIntDataList
    companion object {
        const val LOG_TAG = "EIIIAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.impint_editable_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        Log.i(LOG_TAG, "bind view called, position $position")

        val impIntData: ImpIntData_OLD? = mutableImpIntsList[position].value
        impIntData?.run { holder.bind(this) }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        val position = holder.bindingAdapterPosition
        val editedIfText = holder.itemView.findViewById<EditText>(R.id.ifEditText)
            .text.toString()
        val editedThenText = holder.itemView.findViewById<EditText>(R.id.thenEditText)
            .text.toString()

        viewModel.putOneDataAt(
            editedIfText,
            editedThenText,
            position
        )

        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int = mutableImpIntsList.size

    inner class ViewHolder(viewOfItem: View) : RecyclerView.ViewHolder(viewOfItem) {

        private val ifTextEditor: EditText = viewOfItem.findViewById(R.id.ifEditText)
        private val thenTextEditor: EditText = viewOfItem.findViewById(R.id.thenEditText)

        fun bind(impIntData: ImpIntData_OLD) {
            ifTextEditor.text.replace(0, 0, impIntData.ifText)
            thenTextEditor.text.replace(0, 0, impIntData.thenText)
        }

    }

}