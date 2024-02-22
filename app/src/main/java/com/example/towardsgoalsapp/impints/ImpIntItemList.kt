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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.Constants.Companion.CLASS_NUMBER_NOT_RECOGNIZED
import com.example.towardsgoalsapp.OwnerType
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.database.*
import com.example.towardsgoalsapp.etc.DescriptionFixer
import com.example.towardsgoalsapp.database.userdata.MutablesArrayContentState
import com.example.towardsgoalsapp.database.userdata.MarkedMultipleModifyUserDataSharer
import com.example.towardsgoalsapp.database.userdata.ViewModelWithImpIntsSharer
import com.example.towardsgoalsapp.habits.questioning.HabitQuestionsViewModel
import com.example.towardsgoalsapp.habits.HabitViewModel
import com.example.towardsgoalsapp.tasks.TaskDetailsViewModel
import com.example.towardsgoalsapp.tasks.TaskItemList
import com.example.towardsgoalsapp.tasks.ongoing.TaskOngoingViewModel

class ImpIntItemListViewModelFactory(private val sharer: MarkedMultipleModifyUserDataSharer<ImpIntData>): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ImpIntItemListViewModel(sharer) as T
    }
}

class ImpIntItemListViewModel(
    private val sharer: MarkedMultipleModifyUserDataSharer<ImpIntData>
): ViewModel() {
    val impIntDataList: ArrayList<MutableLiveData<ImpIntData>> = sharer.getArrayOfUserData()
        ?: ArrayList()

    val addedCount = sharer.addedCount
    val listState = sharer.arrayState

    fun putOneImpIntAt(ifText: String, thenText: String, position: Int) {
        val old = impIntDataList[position].value
        old?.run {
            val sanitizedIfText = DescriptionFixer.fix(ifText)
            val sanitizedThenText = DescriptionFixer.fix(thenText)
            if ( sanitizedIfText != old.impIntIfText || sanitizedThenText != old.impIntThenText) {
                impIntDataList[position].value = ImpIntData(
                    this.impIntId,
                    this.impIntEditUnfinished,
                    ifText,
                    thenText,
                    this.ownerId,
                    this.ownerType
                )
                sharer.markChangeOf(this.impIntId)
            }
        }
    }

    fun deleteImpInt(impIntId: Long) {
        sharer.markDeleteOf(impIntId)
        sharer.signalAllMayHaveBeenChanged()
    }
}

class ImpIntItemList : Fragment() {

    companion object {

        const val OWNER_ID_OF_IMPINTS = "oi_ii"
        const val OWNER_TYPE_OF_IMPINTS = "ot_ii"
        const val INHERIT_FROM_CLASS_NUMBER = "ihnclnum"
        const val READ_ONLY_IMPINTS = "roii"
        const val LOG_TAG = "IIIList"

        val expectedViewModelClasses = setOf(
            Constants.viewModelClassToNumber[TaskDetailsViewModel::class.java]
                ?: CLASS_NUMBER_NOT_RECOGNIZED,
            Constants.viewModelClassToNumber[TaskOngoingViewModel::class.java]
                ?: CLASS_NUMBER_NOT_RECOGNIZED,
            Constants.viewModelClassToNumber[HabitViewModel::class.java]
                ?: CLASS_NUMBER_NOT_RECOGNIZED,
            Constants.viewModelClassToNumber[HabitQuestionsViewModel::class.java]
                ?: CLASS_NUMBER_NOT_RECOGNIZED,
        )

        @JvmStatic
        fun newInstance(
            oid: Long,
            ownerType: OwnerType,
            inheritFromClass: Int,
            impIntsAreReadOnly: Boolean
        ) = ImpIntItemList().apply {
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

    private lateinit var viewModel: ImpIntItemListViewModel

    private var readOnly: Boolean = false

    private fun extractSharer(classnumber: Int) : MarkedMultipleModifyUserDataSharer<ImpIntData>?{
        if ((classnumber in expectedViewModelClasses) && classnumber != CLASS_NUMBER_NOT_RECOGNIZED) {
            val clazz: Class<out ViewModel>? = Constants.numberOfClassToViewModelClass[classnumber]
            if (clazz == null ) return null
            else {
                val inheritedViewModel = ViewModelProvider(requireActivity())[clazz]
                if (inheritedViewModel is ViewModelWithImpIntsSharer) {
                    inheritedViewModel.getImpIntsSharer().run {
                        if (this is MarkedMultipleModifyUserDataSharer) return this
                        else null
                    }
                }
            }
        }
        return null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        try {
            requireActivity()
        }
        catch (e: IllegalStateException) {
            Log.e(LOG_TAG, "unable to get activity", e)
            return null
        }

        arguments?.let {
            ownerId = it.getLong(OWNER_ID_OF_IMPINTS)
            val otOrNull = OwnerType.from(it.getString(OWNER_TYPE_OF_IMPINTS) ?: OwnerType.TYPE_NONE.typeString)
            otOrNull?.run { ownerType = this }
            readOnly = it.getBoolean(READ_ONLY_IMPINTS)

            val classNumber = it.getInt(INHERIT_FROM_CLASS_NUMBER)
            val sharer = extractSharer(classNumber)
            sharer?.run {
                viewModel = ViewModelProvider(
                    viewModelStore,
                    ImpIntItemListViewModelFactory(this)
                )[ImpIntItemListViewModel::class.java]
            }
        }

        val view = inflater.inflate(R.layout.impint_list_fragment, container, false)
        if (view is RecyclerView) { with(view) {
            view.itemAnimator = null
            view.setItemViewCacheSize(Constants.RV_ITEM_CACHE_SIZE)

            // set the adapter
            val thisAdapter = if (readOnly)
                              ReadOnlyImpIntItemListAdapter(viewModel)
                              else
                              EditableImpIntItemListAdapter(viewModel)
            thisAdapter.stateRestorationPolicy = RecyclerView.Adapter
                .StateRestorationPolicy.PREVENT_WHEN_EMPTY
            adapter = thisAdapter

            // adding a divider since i was not able to find a xml attribute/style for that
            view.addItemDecoration(
                DividerItemDecoration(view.context, RecyclerView.HORIZONTAL).apply {
                    setDrawable(
                        ContextCompat.getDrawable(view.context, R.drawable.recycler_view_divider)!!
                    )
                }
            )

            viewModel.listState.observe(viewLifecycleOwner) {
                if (it == MutablesArrayContentState.ADDED_NEW) {
                    val added = viewModel.addedCount.value?: 0
                    viewModel.impIntDataList.run {
                        val size = this.size
                        thisAdapter.notifyItemRangeInserted(size - added, added)
                        for (i in size - added..<size) {
                            this[i].observe( viewLifecycleOwner
                            ) { iit ->
                                Log.i(LOG_TAG, "data changed on pos $i, notifying")
                                val posted = view.post {
                                    if (iit == null) {
                                        view.adapter?.notifyItemRemoved(i)
                                    }
                                    else {
                                        view.adapter?.notifyItemChanged(i)
                                    }
                                }
                                Log.i(LOG_TAG, "rv posted: $posted")
                            }
                        }
                    }
                }
            }

        } }
        return view
    }

    override fun onPause() {

        super.onPause()
    }

}