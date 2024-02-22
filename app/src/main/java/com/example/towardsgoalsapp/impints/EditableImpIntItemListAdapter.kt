package com.example.towardsgoalsapp.impints

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import androidx.lifecycle.MutableLiveData
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.database.*


class EditableImpIntItemListAdapter(
    private val viewModel: ImpIntItemListViewModel
) : RecyclerView.Adapter<EditableImpIntItemListAdapter.ViewHolder>() {

    private val mutableImpIntsList: List<MutableLiveData<ImpIntData>> =
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

        val impIntData: ImpIntData? = mutableImpIntsList[position].value
        impIntData?.run { holder.bind(this, position) }
    }

    override fun getItemCount(): Int
        = mutableImpIntsList.filter { p -> p.value != null }.size

    inner class ViewHolder(viewOfItem: View) : RecyclerView.ViewHolder(viewOfItem) {

        private val ifTextEditor: EditText = viewOfItem.findViewById(R.id.triggerEditText)
        private val thenTextEditor: EditText = viewOfItem.findViewById(R.id.reactionEditText)
        private val deleteButton: ImageButton = viewOfItem.findViewById(R.id.deleteImpIntItem)

        fun bind(impIntData: ImpIntData, position: Int) {

            ifTextEditor.setText(impIntData.impIntIfText)
            thenTextEditor.setText(impIntData.impIntThenText)

            fun saveText() {
                val editedIfText = ifTextEditor.text.toString()
                val editedThenText = thenTextEditor.text.toString()
                viewModel.putOneImpIntAt(
                    editedIfText,
                    editedThenText,
                    position
                )
            }

            ifTextEditor.setOnFocusChangeListener { v, hasFocus ->
                if (! hasFocus) saveText()
            }
            thenTextEditor.setOnFocusChangeListener { v, hasFocus ->
                if (! hasFocus) saveText()
            }
            ifTextEditor.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    saveText()
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }
            thenTextEditor.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    saveText()
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }

            deleteButton.setOnClickListener{
                viewModel.deleteImpInt(impIntData.impIntId)
            }
        }

    }

}