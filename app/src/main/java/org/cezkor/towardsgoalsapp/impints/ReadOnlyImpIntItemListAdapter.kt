package org.cezkor.towardsgoalsapp.impints

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.database.*


class ReadOnlyImpIntItemListAdapter(
    viewModel: ImpIntItemListViewModel
) : RecyclerView.Adapter<ReadOnlyImpIntItemListAdapter.ViewHolder>() {

    private val mutableImpIntsList: List<MutableLiveData<ImpIntData>> =
        viewModel.impIntDataList

    companion object {
        const val LOG_TAG = "ROIIIAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.impint_read_only_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        Log.i(LOG_TAG, "bind view called, position $position")

        val impIntData: ImpIntData? = mutableImpIntsList[position].value
        impIntData?.run { holder.bind(this) }
    }

    override fun getItemCount(): Int
        = mutableImpIntsList.filter { p -> p.value != null }.size

    inner class ViewHolder(viewOfItem: View) : RecyclerView.ViewHolder(viewOfItem) {

        private val ifTextView: TextView = viewOfItem.findViewById(R.id.triggerTextView)
        private val thenTextView: TextView = viewOfItem.findViewById(R.id.reactionTextView)

        fun bind(impIntData: ImpIntData) {
            ifTextView.text = impIntData.impIntIfText
            thenTextView.text = impIntData.impIntThenText
        }

    }

}