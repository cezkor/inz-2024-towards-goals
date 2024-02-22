package com.example.towardsgoalsapp.goals

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.database.GoalData
import com.example.towardsgoalsapp.database.repositories.GoalRepository
import com.example.towardsgoalsapp.database.HabitData
import com.example.towardsgoalsapp.database.repositories.HabitRepository
import com.example.towardsgoalsapp.database.TGDatabase
import com.example.towardsgoalsapp.database.TaskData
import com.example.towardsgoalsapp.database.repositories.TaskRepository
import com.example.towardsgoalsapp.database.userdata.HabitDataMutableArrayManager
import com.example.towardsgoalsapp.database.userdata.MutablesArrayContentState
import com.example.towardsgoalsapp.database.userdata.OneModifyUserDataSharer
import com.example.towardsgoalsapp.database.userdata.TaskDataMutableArrayManager
import com.example.towardsgoalsapp.database.userdata.ViewModelUserDataSharer
import com.example.towardsgoalsapp.database.userdata.ViewModelWithManyHabitsSharers
import com.example.towardsgoalsapp.database.userdata.ViewModelWithManyTasksSharers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GoalSynopsisesViewModelFactory(private val dbo: TGDatabase): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GoalSynopsisesViewModel(dbo) as T
    }
}

class GoalSynopsisesViewModel(private val dbo: TGDatabase):
    ViewModel(), ViewModelWithManyHabitsSharers, ViewModelWithManyTasksSharers {

    private val goalRepo = GoalRepository(dbo)

    private val taskRepo = TaskRepository(dbo)

    private val habitRepo = HabitRepository(dbo)

    // meanings of enum entries in order:
    // not ready to show
    // readied when activity view created: with goal data, without it
    // data changed (in case of page with GoalSynopsis)
    // populated with GoalData (to make GoalSynopsis page)
    // populated with AddGoalSuggestion
    enum class MutableGoalDataStates {
        NOT_READY, INITIALIZED_POPULATED, INITIALIZED_EMPTY , REFRESHED, POPULATED, EMPTIED
    }

    val arrayOfGoalData: ArrayList<MutableLiveData<GoalData?>> =
        List(Constants.MAX_GOALS_AMOUNT) { MutableLiveData<GoalData?>() }.toCollection(ArrayList())

    val habitDataArraysPerGoal: HashMap<Long,ArrayList<MutableLiveData<HabitData>>> =
        HashMap(Constants.MAX_GOALS_AMOUNT)

    val taskDataArraysPerGoal: HashMap<Long,ArrayList<MutableLiveData<TaskData>>> =
        HashMap(Constants.MAX_GOALS_AMOUNT)

    private val habitDataArrayManagerPerGoal: HashMap<Long, HabitDataMutableArrayManager> =
        HashMap(Constants.MAX_GOALS_AMOUNT)

    private val taskDataArrayManagerPerGoal: HashMap<Long, TaskDataMutableArrayManager> =
        HashMap(Constants.MAX_GOALS_AMOUNT)

    val arrayOfGoalDataStates: Array<MutableLiveData<MutableGoalDataStates>> =
        Array(Constants.MAX_GOALS_AMOUNT) { MutableLiveData(MutableGoalDataStates.NOT_READY) }

    val allReady: MutableLiveData<Boolean> = MutableLiveData(false)
    private val getMutex = Mutex()

    private val gidToPosition = HashMap<Long, Int>()

    val lastTabIndexes: Array<MutableLiveData<Int>> =
        Array(Constants.MAX_GOALS_AMOUNT) { MutableLiveData(Constants.IGNORE_ID_AS_INT) }

    private suspend fun setReputtersAndMutablesArrays(goal: GoalData) {
        val id = goal.goalId
        val habits = habitRepo.getAllByGoalId(id)
        val tasks = taskRepo.getAllByGoalId(id)
        if (! habitDataArraysPerGoal.containsKey(id)){
            habitDataArraysPerGoal[id] = ArrayList()
        }
        if (! habitDataArrayManagerPerGoal.containsKey(id) ){

            habitDataArraysPerGoal[id]?.run {
                habitDataArrayManagerPerGoal[id] = HabitDataMutableArrayManager(
                    this, habits
                )
            }
        }
        if (! taskDataArraysPerGoal.containsKey(id)){
            taskDataArraysPerGoal[id] = ArrayList()
        }
        if (! taskDataArrayManagerPerGoal.containsKey(id) ){
            taskDataArraysPerGoal[id]?.run {
                taskDataArrayManagerPerGoal[id] = TaskDataMutableArrayManager(
                    this, tasks
                )
            }
        }
    }

    suspend fun getEverything() = getMutex.withLock {
            // for every goal up to max amount, based on its page number put it in the array
        allReady.value = false

        val goals = goalRepo.getAllGoals()

        val pNumToNullOrGoal = goals.associateBy { it.pageNumber }.toSortedMap()

        for (pnum: Int in 0..<Constants.MAX_GOALS_AMOUNT) {
            val goal = pNumToNullOrGoal[pnum]
            goal?.run {
                gidToPosition[this.goalId] = pnum
                arrayOfGoalData[pnum].value = this
                setReputtersAndMutablesArrays(this)
                arrayOfGoalDataStates[pnum].value = MutableGoalDataStates.INITIALIZED_POPULATED
            }
            if (goal == null)
                arrayOfGoalDataStates[pnum].value = MutableGoalDataStates.INITIALIZED_EMPTY
        }

        allReady.value = true
    }

    private fun createSharerForTasksOf(goalId: Long): ViewModelUserDataSharer<TaskData> = object :
        OneModifyUserDataSharer<TaskData> {
        override fun getArrayOfUserData(): ArrayList<MutableLiveData<TaskData>>?
                = taskDataArraysPerGoal[goalId]
        override fun signalNeedOfChangeFor(userDataId: Long) { viewModelScope.launch {
            if (userDataId == Constants.IGNORE_ID_AS_LONG) return@launch
            val manager = taskDataArrayManagerPerGoal[goalId]
            val list = taskDataArraysPerGoal[goalId]
            if (list == null || manager == null) return@launch
            val hadSuchTask = manager.hasUserDataOfId(userDataId)
            val taskData = taskRepo.getOneById(userDataId)
            val position = gidToPosition[goalId] ?: return@launch
            if (taskData == null) {
                // it was probably removed
                if (hadSuchTask) {
                    manager.deleteOneOldUserDataById(userDataId)
                }
            }
            else {
                manager.updateOneUserData(taskData)
            }
            arrayOfGoalDataStates[position].value = MutableGoalDataStates.REFRESHED
        } }
        override val arrayState: MutableLiveData<MutablesArrayContentState>
            = taskDataArrayManagerPerGoal[goalId]?.contentState ?: MutableLiveData()
        override val addedCount
            = taskDataArrayManagerPerGoal[goalId]?.addedCount?: MutableLiveData(0)
        override fun isArrayConsideredEmpty(): Boolean {
            val arr = getArrayOfUserData() ?: return true
            return arr.none { md -> md.value != null } ||
                    arr.isEmpty()
        }
    }

    private fun createSharerForHabitsOf(goalId: Long): ViewModelUserDataSharer<HabitData> = object :
        OneModifyUserDataSharer<HabitData> {
        override fun getArrayOfUserData(): ArrayList<MutableLiveData<HabitData>>?
                = habitDataArraysPerGoal[goalId]
        override fun signalNeedOfChangeFor(userDataId: Long) { viewModelScope.launch {
            if (userDataId == Constants.IGNORE_ID_AS_LONG) return@launch
            val manager = habitDataArrayManagerPerGoal[goalId]
            val list = habitDataArraysPerGoal[goalId]
            if (list == null || manager == null) return@launch
            val hadSuchHabit = manager.hasUserDataOfId(userDataId)
            val habitData = habitRepo.getOneById(userDataId)
            if (habitData == null) {
                // it was probably removed
                if (hadSuchHabit)
                    manager.deleteOneOldUserDataById(userDataId)
            }
            else {
                manager.updateOneUserData(habitData)
            }
        } }
        override val arrayState: MutableLiveData<MutablesArrayContentState>
            = habitDataArrayManagerPerGoal[goalId]?.contentState ?: MutableLiveData()
        override val addedCount
            = habitDataArrayManagerPerGoal[goalId]?.addedCount ?: MutableLiveData(0)

        override fun isArrayConsideredEmpty(): Boolean {
            val arr = getArrayOfUserData() ?: return true
            return arr.none { md -> md.value != null } ||
                    arr.isEmpty()
        }
    }

    fun getOrUpdateOneGoal(goalId: Long) {
        // get it to view model - it will be updated via GoalDetails so all this vm has to do is
        // to fetch it
        val pos = gidToPosition[goalId]

        viewModelScope.launch { getMutex.withLock {
            if (goalId == Constants.IGNORE_ID_AS_LONG) return@withLock
            val goal = goalRepo.getOneById(goalId)
            if (goal == null) { // it was probably removed
                if (pos == null) return@withLock
                arrayOfGoalDataStates[pos].value = MutableGoalDataStates.EMPTIED
                arrayOfGoalData[pos].value = null
                habitDataArrayManagerPerGoal.remove(goalId)
                habitDataArraysPerGoal.remove(goalId)
                taskDataArrayManagerPerGoal.remove(goalId)
                taskDataArraysPerGoal.remove(goalId)
            }
            else {
                if (goal.pageNumber >= Constants.MAX_GOALS_AMOUNT) return@withLock
                var thePos = pos
                if (thePos == null) {
                    thePos = goal.pageNumber
                    arrayOfGoalData[thePos].value = goal
                    setReputtersAndMutablesArrays(goal)
                    arrayOfGoalDataStates[thePos].value = MutableGoalDataStates.POPULATED
                    gidToPosition[goalId] = thePos
                } else {
                    arrayOfGoalData[thePos].value = goal
                    habitDataArrayManagerPerGoal[goalId]!!.setUserDataArray(
                        habitRepo.getAllByGoalId(goalId, false)
                    )
                    taskDataArrayManagerPerGoal[goalId]!!.setUserDataArray(
                        taskRepo.getAllByGoalId(goalId, false)
                    )
                    arrayOfGoalDataStates[thePos].value = MutableGoalDataStates.REFRESHED
                }
            }
        } }
    }

    override fun getTasksSharer(byOwnerId: Long): ViewModelUserDataSharer<TaskData>?
        =   if (byOwnerId in taskDataArraysPerGoal.keys) createSharerForTasksOf(byOwnerId)
            else null



    override fun getHabitsSharer(byOwnerId: Long): ViewModelUserDataSharer<HabitData>?
        =   if (byOwnerId in habitDataArraysPerGoal.keys) createSharerForHabitsOf(byOwnerId)
            else null
}
