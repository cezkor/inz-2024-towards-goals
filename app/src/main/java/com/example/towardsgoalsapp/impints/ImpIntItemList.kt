package com.example.towardsgoalsapp.impints

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.OwnerType
import com.example.towardsgoalsapp.R

class ImpIntItemListViewModel(val impIntDataList: ArrayList<MutableLiveData<ImpIntData_OLD>>): ViewModel() {
    fun putOneDataAt(
        ifText: String,
        thenText: String,
        pos: Int
    ) {
        val old = impIntDataList[pos].value
        old?.run {
            val new = ImpIntData_OLD(
                this.impIntId,
                ifText,
                thenText,
                this.ownerId,
                this.typeOfOwner
            )
            impIntDataList[pos].value = new
        }
    }

    fun updateOneDataAt(
        ifText: String,
        thenText: String,
        pos: Int
    ) {
        putOneDataAt(ifText, thenText, pos)
        // update in database
    }

}

class IntImpItemList : Fragment() {

    companion object {

        const val OWNER_ID_OF_IMPINTS = "oi_ii"
        const val OWNER_TYPE_OF_IMPINTS = "ot_ii"
        const val INHERIT_FROM_CLASS_NUMBER = "ihnclnum"
        const val READ_ONLY_IMPINTS = "roii"
        const val LOG_TAG = "IIIList"

        @JvmStatic
        fun newInstance(
            oid: Long,
            ownerType: OwnerType,
            inheritFromClass: Int,
            impIntsAreReadOnly: Boolean
        ) = IntImpItemList().apply {
                arguments = Bundle().apply {
                    putLong(OWNER_ID_OF_IMPINTS, oid)
                    putString(OWNER_TYPE_OF_IMPINTS, ownerType.typeString)
                    putInt(INHERIT_FROM_CLASS_NUMBER, inheritFromClass)
                    putBoolean(READ_ONLY_IMPINTS, impIntsAreReadOnly)
                }
            }
    }

    private var ownerId: Long = Constants.IGNORE_ID_AS_LONG
    private var ownerType: OwnerType = OwnerType.TYPE_NONE

    private var impIntsCount = Constants.IGNORE_COUNT_AS_INT // will be got from database

    private lateinit var viewModel: ImpIntItemListViewModel

    private var readOnly: Boolean = false

    private fun extractLiveDataArray(clazz: Class<*>?) : java.util.ArrayList<MutableLiveData<ImpIntData_OLD>>?{
        return when (clazz) {
            // to do
            else -> null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            ownerId = it.getLong(OWNER_ID_OF_IMPINTS)
            ownerType = OwnerType.valueOf(
                it.getString(OWNER_TYPE_OF_IMPINTS)?: OwnerType.TYPE_NONE.typeString
            )
            readOnly = it.getBoolean(READ_ONLY_IMPINTS)

            val classNumber = it.getInt(INHERIT_FROM_CLASS_NUMBER)
            var liveDataz = extractLiveDataArray(
                Constants.numberOfClassToViewModelClass[classNumber]
            )
            liveDataz?.run {
                viewModel = ImpIntItemListViewModel(this)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.impint_list_fragment, container, false)
        // Set the adapter
        if (view is RecyclerView) { with(view) {
            view.setItemViewCacheSize(Constants.RV_ITEM_CACHE_SIZE)

            val ater = if (readOnly) ReadOnlyImpIntItemListAdapter(viewModel)
            else EditableImpIntItemListAdapter(viewModel)
            ater.stateRestorationPolicy = RecyclerView.Adapter
                .StateRestorationPolicy.PREVENT_WHEN_EMPTY

            // adding a divider since i was not able to find a xml attribute/style for that
            view.addItemDecoration(
                DividerItemDecoration(view.context, RecyclerView.HORIZONTAL).apply {
                    setDrawable(
                        ContextCompat.getDrawable(view.context, R.drawable.recycler_view_divider)!!
                    )
                }
            )

            var i = 0
            while (i < impIntsCount) {
                val k = i
                viewModel.impIntDataList[k].observe( viewLifecycleOwner
                ) {
                    Log.i(LOG_TAG, "data changed on pos $k, notifying")
                    view.adapter?.notifyItemChanged(k)
                }
                i += 1
            }
        } }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

}