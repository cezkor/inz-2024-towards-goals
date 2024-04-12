package org.cezkor.towardsgoalsapp.tasks

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.database.DatabaseObjectFactory
import org.cezkor.towardsgoalsapp.database.TGDatabase
import org.cezkor.towardsgoalsapp.database.repositories.StatsDataRepository
import org.cezkor.towardsgoalsapp.etc.OneTextFragment
import org.cezkor.towardsgoalsapp.etc.errors.ErrorHandling
import org.cezkor.towardsgoalsapp.etc.errors.ErrorHandlingViewModel
import org.cezkor.towardsgoalsapp.main.App
import org.cezkor.towardsgoalsapp.stats.StatsShowing
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

class TaskStatsViewModelFactory(private val dbo: TGDatabase,
                                       private val goalId: Long
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TaskStatsViewModel(dbo, goalId) as T
    }
}

class TaskStatsViewModel(private val dbo: TGDatabase,
                         private val goalId: Long
) : ErrorHandlingViewModel()
{
    private val statsRepo = StatsDataRepository(dbo)

    val canShowTaskStats: MutableLiveData<Boolean> = MutableLiveData()

    suspend fun checkIfItIsPossibleToShowStats() {
        canShowTaskStats.value = StatsShowing.canShowTaskGeneralStats(statsRepo, goalId)
    }
}
class TaskStatsFragment : Fragment() {

    companion object {
        const val LOG_TAG = "TStatsFr"
        private const val FRAG_TAG = "TStatsFr_FRAG_TAG_98012301"

        const val GOAL_ID = "tstatgid"

        fun newInstance(goalId: Long) =
            TaskStatsFragment().apply {
                arguments = Bundle().apply {
                    putLong(GOAL_ID, goalId)
                }
            }
    }

    private var goalId: Long = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG

    private lateinit var viewModel: TaskStatsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_task_stats, container, false)
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
            goalId = this.getLong(GOAL_ID, goalId)
        }
        if (goalId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) {
            Log.e(LOG_TAG, "no goal id")
            return
        }

        val dbo = DatabaseObjectFactory
            .newDatabaseObject((requireActivity().application as App).driver)

        viewModel = ViewModelProvider(viewModelStore,
            TaskStatsViewModelFactory(dbo, goalId)
        )[TaskStatsViewModel::class.java]

        viewModel.exceptionMutable.observe(viewLifecycleOwner) {
            ErrorHandling.showExceptionDialog(requireActivity(), it)
        }

        val fragmentContainerId = R.id.taskStatsContainer

        viewModel.canShowTaskStats.observe(viewLifecycleOwner) {
            val fragment : Fragment = if (it) {
                TasksOfGoalGeneralStatsFragment.newInstance(goalId)
            }
            else {
                OneTextFragment.newInstance(getString(R.string.tasks_no_task_data))
            }
            childFragmentManager.beginTransaction()
                .replace(fragmentContainerId, fragment, FRAG_TAG)
                .setReorderingAllowed(true)
                .commit()
        }

        lifecycleScope.launch(viewModel.exceptionHandler)
        { viewModel.checkIfItIsPossibleToShowStats() }

    }

}