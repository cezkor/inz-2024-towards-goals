package org.cezkor.towardsgoalsapp.habits

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.goals.GoalSynopsisesViewModel
import org.cezkor.towardsgoalsapp.goals.GoalViewModel
import org.cezkor.towardsgoalsapp.database.*
import org.cezkor.towardsgoalsapp.database.userdata.MutablesArrayContentState
import org.cezkor.towardsgoalsapp.database.userdata.OneModifyUserDataSharer
import org.cezkor.towardsgoalsapp.database.userdata.ViewModelUserDataSharer
import org.cezkor.towardsgoalsapp.database.userdata.ViewModelWithHabitsSharer
import org.cezkor.towardsgoalsapp.database.userdata.ViewModelWithManyHabitsSharers
import org.cezkor.towardsgoalsapp.habits.questioning.HabitQuestioningContract
import org.cezkor.towardsgoalsapp.habits.questioning.HabitQuestioningLauncher

class HabitItemListViewModelFactory(private val sharer: OneModifyUserDataSharer<HabitData>): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HabitItemListViewModel(sharer) as T
    }
}

class HabitItemListViewModel(private val sharer: OneModifyUserDataSharer<HabitData>): ViewModel() {
    val habitDataList: ArrayList<MutableLiveData<HabitData>> = sharer.getArrayOfUserData() ?: ArrayList()

    val listState = sharer.arrayState

    val addedCount = sharer.addedCount
    fun notifyTaskUpdateOf(habitId : Long) = sharer.signalNeedOfChangeFor(habitId)
}

class HabitItemListFragment : Fragment() {

    companion object {

        const val GOAL_ID_OF_HABITS = "gidofh"
        const val INHERIT_FROM_CLASS_NUMBER = "ihnclnum"
        const val LOG_TAG = "HIList"

        @JvmStatic
        fun newInstance(gid: Long, inheritFromClass: Int) =
            HabitItemListFragment().apply {
                arguments = Bundle().apply {
                    putLong(GOAL_ID_OF_HABITS, gid)
                    putInt(INHERIT_FROM_CLASS_NUMBER, inheritFromClass)
                }
            }

        val expectedViewModelClasses = setOf(
            org.cezkor.towardsgoalsapp.Constants.viewModelClassToNumber[GoalSynopsisesViewModel::class.java]
                ?: org.cezkor.towardsgoalsapp.Constants.CLASS_NUMBER_NOT_RECOGNIZED,
            org.cezkor.towardsgoalsapp.Constants.viewModelClassToNumber[GoalViewModel::class.java]
                ?: org.cezkor.towardsgoalsapp.Constants.CLASS_NUMBER_NOT_RECOGNIZED
        )
    }

    private var goalId = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG

    private lateinit var viewModel: HabitItemListViewModel

    private lateinit var habitQuestioningLauncher: HabitQuestioningLauncher
    private lateinit var habitDetailsLauncher: HabitInfoLauncher

    private fun extractSharer(classnumber: Int) : OneModifyUserDataSharer<HabitData>?{
        if ((classnumber in expectedViewModelClasses) && classnumber != org.cezkor.towardsgoalsapp.Constants.CLASS_NUMBER_NOT_RECOGNIZED) {
            val clazz: Class<out ViewModel>? = org.cezkor.towardsgoalsapp.Constants.numberOfClassToViewModelClass[classnumber]
            if (clazz == null ) return null
            else {
                val inheritedViewModel = ViewModelProvider(requireActivity())[clazz]
                var sharer: ViewModelUserDataSharer<HabitData>? = null
                if (inheritedViewModel is ViewModelWithHabitsSharer) {
                    sharer = inheritedViewModel.getHabitsSharer()
                }
                if (inheritedViewModel is ViewModelWithManyHabitsSharers) {
                    sharer = inheritedViewModel.getHabitsSharer(goalId)
                }
                return if (sharer is OneModifyUserDataSharer) sharer else null
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
            goalId = it.getLong(GOAL_ID_OF_HABITS)
            val classNumber = it.getInt(INHERIT_FROM_CLASS_NUMBER)
            val sharer = extractSharer(classNumber)
            sharer?.run {
                viewModel = ViewModelProvider(viewModelStore,
                    HabitItemListViewModelFactory(this))[HabitItemListViewModel::class.java]
            }

        }

        val view = inflater.inflate(R.layout.question_list_fragment, container, false)
        if (view is RecyclerView) { with(view) {
            view.itemAnimator = null
            // set the adapter
            habitQuestioningLauncher = registerForActivityResult(HabitQuestioningContract()) {
                if (it != org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) viewModel.notifyTaskUpdateOf(it)
            }
            habitDetailsLauncher = registerForActivityResult(HabitInfoContract()) {
                if (it != org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) viewModel.notifyTaskUpdateOf(it)
            }

            view.setItemViewCacheSize(org.cezkor.towardsgoalsapp.Constants.RV_ITEM_CACHE_SIZE)
            view.setHasFixedSize(true)

            val completedHabitImage =
                AppCompatResources.getDrawable(context, R.drawable.full_oval_green)
            val habitImage = AppCompatResources.getDrawable(context, R.drawable.full_oval)
            val editUnfinishedImage = AppCompatResources.getDrawable(context, R.drawable.alert)
            val notMarkableImage = AppCompatResources.getDrawable(context, R.drawable.full_oval_gray)

            adapter = HabitItemListAdapter(viewModel, habitImage, completedHabitImage,
                editUnfinishedImage, notMarkableImage)
            .apply {
                stateRestorationPolicy = RecyclerView.Adapter
                    .StateRestorationPolicy.PREVENT_WHEN_EMPTY
                setOnItemClickListener {
                    habitDetailsLauncher.launch(
                        Triple(it.habitId, org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG, false)
                    )
                }
                setOnHabitMarkButtonClickListener {
                    habitQuestioningLauncher.launch(
                        it.habitId
                    )
                }
            }
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
                    viewModel.habitDataList?.run {
                        val size = this.size
                        adapter?.notifyItemRangeInserted(size - added, added)
                        for (i in size - added..<size) {
                            this[i].observe( viewLifecycleOwner
                            ) { hit ->
                                Log.i(LOG_TAG, "data changed on pos $i, notifying")
                                if (hit == null) {
                                    view.adapter?.notifyItemRemoved(i)
                                }
                                else {
                                    view.adapter?.notifyItemChanged(i)
                                }
                            }
                        }
                    }
                }
            }
        } }
        return view
    }

}