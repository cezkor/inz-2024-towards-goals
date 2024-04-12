package org.cezkor.towardsgoalsapp.habits.params

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
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.database.*
import org.cezkor.towardsgoalsapp.database.userdata.MarkedMultipleModifyUserDataSharer
import org.cezkor.towardsgoalsapp.database.userdata.MutablesArrayContentState
import org.cezkor.towardsgoalsapp.database.userdata.ViewModelUserDataSharer
import org.cezkor.towardsgoalsapp.database.userdata.ViewModelWithHabitParamsSharer
import org.cezkor.towardsgoalsapp.etc.ParamUnitFixer
import org.cezkor.towardsgoalsapp.etc.VeryShortNameFixer
import org.cezkor.towardsgoalsapp.habits.HabitViewModel

class HabitParamItemListViewModelFactory(private val sharer: MarkedMultipleModifyUserDataSharer<HabitParameter>): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HabitParamItemListViewModel(sharer) as T
    }
}

class HabitParamItemListViewModel(private val sharer: MarkedMultipleModifyUserDataSharer<HabitParameter>): ViewModel() {
    val habitParamList: ArrayList<MutableLiveData<HabitParameter>> = sharer.getArrayOfUserData() ?: ArrayList()

    val listState = sharer.arrayState

    val addedCount = sharer.addedCount

    // this method assumes MutableLiveData at given position has HabitParameter object
    fun putOneHabitParamAt(name: String, targetValueText: String, unit: String?, position: Int) {
        val old = habitParamList[position].value
        old?.run {
            var sanitizedUnitText: String? = ParamUnitFixer.fix(unit)
            if (sanitizedUnitText.isNullOrEmpty()) sanitizedUnitText = null
            val sanitizedNameText = VeryShortNameFixer.fix(name)
            var targetValue = old.targetVal
            try {
                targetValue = targetValueText.toDouble()
            }
            catch (e : NumberFormatException) {
                // skip
            }

            habitParamList[position].value = HabitParameter(
                old.paramId,
                old.hParEditUnfinished,
                old.habitId,
                sanitizedNameText,
                targetValue,
                sanitizedUnitText
            )

            sharer.markChangeOf(this.paramId)
        }
    }

    fun deleteParam(paramId: Long) {
        sharer.markDeleteOf(paramId)
        sharer.signalAllMayHaveBeenChanged()
    }
}

class HabitParamItemList : Fragment() {

    companion object {

        const val INHERIT_FROM_CLASS_NUMBER = "ihnclnum"
        const val READ_ONLY_LIST = "hpilro"
        const val LOG_TAG = "HPIList"

        val expectedViewModelClasses = setOf(
            org.cezkor.towardsgoalsapp.Constants.viewModelClassToNumber[HabitViewModel::class.java]
                ?: org.cezkor.towardsgoalsapp.Constants.CLASS_NUMBER_NOT_RECOGNIZED,
        )

        @JvmStatic
        fun newInstance(readOnly: Boolean, inheritFromClass: Int) =
            HabitParamItemList().apply {
                arguments = Bundle().apply {
                    putInt(INHERIT_FROM_CLASS_NUMBER, inheritFromClass)
                    putBoolean(READ_ONLY_LIST, readOnly)
                }
            }

    }

    private lateinit var viewModel: HabitParamItemListViewModel

    private var readOnly: Boolean = false

    private fun extractSharer(classnumber: Int) : MarkedMultipleModifyUserDataSharer<HabitParameter>?{
        if ((classnumber in expectedViewModelClasses) && classnumber != org.cezkor.towardsgoalsapp.Constants.CLASS_NUMBER_NOT_RECOGNIZED) {
            val clazz: Class<out ViewModel>? = org.cezkor.towardsgoalsapp.Constants.numberOfClassToViewModelClass[classnumber]
            if (clazz == null ) return null
            else {
                val inheritedViewModel = ViewModelProvider(requireActivity())[clazz]
                var sharer: ViewModelUserDataSharer<HabitParameter>? = null
                if (inheritedViewModel is ViewModelWithHabitParamsSharer) {
                    sharer = inheritedViewModel.getHabitParamsSharer()
                    return if (sharer is MarkedMultipleModifyUserDataSharer) sharer else null
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
            readOnly = it.getBoolean(READ_ONLY_LIST)
            val classNumber = it.getInt(INHERIT_FROM_CLASS_NUMBER)
            val sharer = extractSharer(classNumber)
            sharer?.run {
                viewModel = ViewModelProvider(viewModelStore,
                    HabitParamItemListViewModelFactory(this))[HabitParamItemListViewModel::class.java]
            }

        }

        val view = inflater.inflate(R.layout.habit_parameter_list_fragment, container, false)
        if (view is RecyclerView) { with(view) {
            view.itemAnimator = null

            view.setItemViewCacheSize(org.cezkor.towardsgoalsapp.Constants.RV_ITEM_CACHE_SIZE)
            view.setHasFixedSize(true)

            val thisAdapter = if (readOnly)
                ReadOnlyHabitParamItemListAdapter(viewModel)
            else
                EditableHabitParamItemListAdapter(viewModel)
            thisAdapter.apply {
                stateRestorationPolicy = RecyclerView.Adapter
                    .StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
            adapter = thisAdapter

            // set recycler view item divider
            view.addItemDecoration(
                DividerItemDecoration(view.context, RecyclerView.HORIZONTAL).apply {
                    setDrawable(
                        ContextCompat.getDrawable(view.context, R.drawable.recycler_view_divider)!!
                    )
                }
            )

            fun addListenersToNewParams(addedParamsCount: Int) {
                viewModel.habitParamList?.run {
                    val size = this.size
                    // notify about added MutableLiveDatas
                    adapter?.
                      notifyItemRangeInserted(size - addedParamsCount, addedParamsCount)
                    // add observer for new MutableLiveData in list
                    for (i in size - addedParamsCount..<size) {
                        this[i].observe( viewLifecycleOwner
                        ) { hit ->
                            Log.i(LOG_TAG, "data changed on pos $i, notifying")
                            val posted = view.post {
                                if (hit == null) {
                                    view.adapter?.notifyItemRemoved(i)
                                } else {
                                    view.adapter?.notifyItemChanged(i)
                                }
                            }
                            Log.i(LOG_TAG, "rv posted: $posted")
                        }
                    }
                }
            }

            viewModel.listState.observe(viewLifecycleOwner) {
                if (it != MutablesArrayContentState.ADDED_NEW) return@observe
                val added = viewModel.addedCount.value?: 0
                addListenersToNewParams(added)
            }
        } }
        return view
    }

}