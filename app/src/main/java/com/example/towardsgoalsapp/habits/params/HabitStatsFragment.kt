package com.example.towardsgoalsapp.habits.params

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.database.DatabaseObjectFactory
import com.example.towardsgoalsapp.database.HabitParameter
import com.example.towardsgoalsapp.database.TGDatabase
import com.example.towardsgoalsapp.database.repositories.HabitParamsRepository
import com.example.towardsgoalsapp.database.repositories.StatsDataRepository
import com.example.towardsgoalsapp.etc.OneTextFragment
import com.example.towardsgoalsapp.etc.OneTimeEvent
import com.example.towardsgoalsapp.etc.errors.ErrorHandling
import com.example.towardsgoalsapp.etc.errors.ErrorHandlingViewModel
import com.example.towardsgoalsapp.habits.HabitGeneralStatsFragment
import com.example.towardsgoalsapp.habits.HabitGeneralStatsViewModel
import com.example.towardsgoalsapp.main.App
import com.example.towardsgoalsapp.stats.StatsShowing
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

class HabitStatsViewModelFactory(private val dbo: TGDatabase,
                                       private val habitId: Long
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HabitStatsViewModel(dbo, habitId) as T
    }
}

class HabitStatsViewModel(private val dbo: TGDatabase,
                          private val habitId: Long
) : ErrorHandlingViewModel()
{
    private val habitParamsRepo = HabitParamsRepository(dbo)
    private val statsRepo = StatsDataRepository(dbo)

    var canShowHabitStats = false
        private set
    var canShowParamStats = false
        private set

    val allReady: MutableLiveData<OneTimeEvent> = MutableLiveData()

    suspend fun checkIfItIsPossibleToShowStats() {
        canShowParamStats = StatsShowing.canShowHabitParamsStats(habitParamsRepo, habitId)
        canShowHabitStats = StatsShowing.canShowHabitGeneralStats(statsRepo, habitId)
        allReady.value = OneTimeEvent()
    }
}
class HabitStatsFragment : Fragment() {

    companion object {
        const val LOG_TAG = "HStatsFr"

        const val FRAG_TAG1 = "HStatsFr_FRAGTAG_2998409200"
        const val HABIT_ID = "hstathid"

        fun newInstance(habitId: Long) =
            HabitStatsFragment().apply {
                arguments = Bundle().apply {
                    putLong(HABIT_ID, habitId)
                }
            }
    }

    private var habitId: Long = Constants.IGNORE_ID_AS_LONG

    private lateinit var viewModel: HabitStatsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_habit_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            requireActivity()
        }
        catch (e : IllegalStateException) {
            Log.e(LOG_TAG,"no activity", e)
            return
        }

        arguments?.run {
            habitId = this.getLong(HABIT_ID, habitId)
        }
        if (habitId == Constants.IGNORE_ID_AS_LONG) {
            Log.e(LOG_TAG, "no habit id")
            return
        }

        val statsContainerId = R.id.statsContainer
        val radioButtonGroup: RadioGroup = view.findViewById(R.id.pickRadioGroup)

        val dbo = DatabaseObjectFactory
            .newDatabaseObject((requireActivity().application as App).driver)

        viewModel = ViewModelProvider(viewModelStore,
            HabitStatsViewModelFactory(dbo, habitId)
        )[HabitStatsViewModel::class.java]

        viewModel.exceptionMutable.observe(viewLifecycleOwner) {
            ErrorHandling.showExceptionDialog(requireActivity(), it)
        }

        lifecycleScope.launch(viewModel.exceptionHandler) { viewModel.checkIfItIsPossibleToShowStats() }

        viewModel.allReady.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            it.handleIfNotHandledWith {
                radioButtonGroup.setOnCheckedChangeListener { grp, buttonID ->
                    when (buttonID) {
                        R.id.generalStatsRadioButton -> {
                            val fragment: Fragment = if (viewModel.canShowHabitStats) {
                                HabitGeneralStatsFragment.newInstance(habitId)
                            } else {
                                OneTextFragment.newInstance(getString(R.string.habits_no_data))
                            }
                            // set fragment
                            childFragmentManager.beginTransaction()
                                .setReorderingAllowed(true)
                                .setCustomAnimations(R.anim.layout_accelerator, R.anim.layout_deccelerator)
                                .replace(statsContainerId, fragment, FRAG_TAG1)
                                .commitNow()
                        }
                        R.id.paramStatsRadioButton -> {
                            val fragment: Fragment = if (viewModel.canShowParamStats) {
                                HabitParamsStatsFragment.newInstance(habitId)
                            } else {
                                OneTextFragment.newInstance(getString(R.string.habits_no_params_or_no_values))
                            }
                            // set fragment
                            childFragmentManager.beginTransaction()
                                .setReorderingAllowed(true)
                                .setCustomAnimations(R.anim.layout_accelerator, R.anim.layout_deccelerator)
                                .replace(statsContainerId, fragment, FRAG_TAG1)
                                .commitNow()
                        }
                    }
                }
            }
        }



    }

}