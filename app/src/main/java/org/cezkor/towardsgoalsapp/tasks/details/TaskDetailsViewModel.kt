package org.cezkor.towardsgoalsapp.tasks.details

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import org.cezkor.towardsgoalsapp.database.ImpIntData
import org.cezkor.towardsgoalsapp.database.userdata.RecreatingTaskDataFactory
import org.cezkor.towardsgoalsapp.database.TGDatabase
import org.cezkor.towardsgoalsapp.database.TaskData
import org.cezkor.towardsgoalsapp.database.userdata.MarkedMultipleModifyUserDataSharer
import org.cezkor.towardsgoalsapp.database.userdata.MutablesArrayContentState
import org.cezkor.towardsgoalsapp.database.userdata.OneModifyUserDataSharer
import org.cezkor.towardsgoalsapp.database.userdata.TaskDataMutableArrayManager
import org.cezkor.towardsgoalsapp.database.userdata.ViewModelWithTasksSharer
import org.cezkor.towardsgoalsapp.etc.OneTimeEvent
import org.cezkor.towardsgoalsapp.etc.OneTimeHandleable
import org.cezkor.towardsgoalsapp.tasks.TaskViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.time.Instant

class TaskDetailsViewModel(private val dbo: TGDatabase, private var taskId: Long,
                           private var goalId: Long, private var ownerTaskId: Long):
    TaskViewModel(dbo, taskId, goalId), ViewModelWithTasksSharer {

    var currentTabIdx: Int = -1
    var setTabEvent: MutableLiveData<OneTimeEvent> = MutableLiveData()

    var priority: Int = 0
        set(v) {
            if (v < org.cezkor.towardsgoalsapp.Constants.MIN_PRIORITY) field = org.cezkor.towardsgoalsapp.Constants.MIN_PRIORITY
            if (v > org.cezkor.towardsgoalsapp.Constants.MAX_PRIORITY) field = org.cezkor.towardsgoalsapp.Constants.MAX_PRIORITY
            field = v
        }

    val assureOfExistingTaskHandleableMutable = MutableLiveData<OneTimeHandleable>()

    private val arrayOfMutableSubtasksTaskData: ArrayList<MutableLiveData<TaskData>> =
        java.util.ArrayList()

    private val arrayOfMutableSubtasksManager: TaskDataMutableArrayManager
            = TaskDataMutableArrayManager(arrayOfMutableSubtasksTaskData)

    private var addedSubtasksSet: Set<Long> = setOf()

    private val subtasksSharer: OneModifyUserDataSharer<TaskData> = object :
        OneModifyUserDataSharer<TaskData> {
        override fun getArrayOfUserData(): ArrayList<MutableLiveData<TaskData>>?
            = arrayOfMutableSubtasksTaskData
        override fun signalNeedOfChangeFor(userDataId: Long) {
            viewModelScope.launch(exceptionHandler) {
                if (userDataId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) return@launch
                val manager = arrayOfMutableSubtasksManager
                val list = getArrayOfUserData() ?: return@launch
                val hadSuchTask = manager.hasUserDataOfId(userDataId)
                val taskData = taskRepo.getOneById(userDataId)
                if (taskData == null) { if (hadSuchTask)
                    manager.deleteOneOldUserDataById(userDataId)
                }
                else {
                    if (! hadSuchTask) addedSubtasksSet = addedSubtasksSet.plus(userDataId)
                    manager.updateOneUserData(taskData)
                }
            }
        }
        override val arrayState: MutableLiveData<MutablesArrayContentState>
            get() = arrayOfMutableSubtasksManager.contentState
        override val addedCount: MutableLiveData<Int>
            get() = arrayOfMutableSubtasksManager.addedCount

        override fun isArrayConsideredEmpty(): Boolean {
            return arrayOfMutableSubtasksTaskData.none { md -> md.value != null } ||
                    arrayOfMutableSubtasksTaskData.isEmpty()
        }

    }
    override fun getTasksSharer(): OneModifyUserDataSharer<TaskData>? = subtasksSharer

    private var ownerTaskObject: TaskData? = null

    private fun getOwnerTaskDepth() : Int {
        return ownerTaskObject?.taskDepth ?: -1
    }

    fun getTaskDepth(): Int
        { return mutableTaskData.value?.taskDepth ?: 0 }

    suspend fun getCurrentSubtasksCount() : Long = taskRepo.getOneById(taskId)?.subtasksCount ?: 0L

    suspend fun addOneNewEmptyImpInt() : Boolean {
        // it is assumed there was an attempt at adding a new task
        if (taskId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) return false
        val iid = impIntRepo.addImpInt(
            org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING, org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING, org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK, taskId
        )

        val sharer = getImpIntsSharer()
        sharer.markAddOf(iid)
        sharer.signalAllMayHaveBeenChanged()

        return true
    }

    private suspend fun updateImpInts(asUnfinished: Boolean){
        fun decide(value: ImpIntData?) : Boolean {
            val sharer: MarkedMultipleModifyUserDataSharer<ImpIntData> = getImpIntsSharer()
            val valueNotNull = value != null
            if (! valueNotNull) return false
            val stateMatches = if (asUnfinished) {
                ( sharer.getStateOf(value!!.impIntId)
                        == MarkedMultipleModifyUserDataSharer.UserDataStates.CHANGE )
            } else {
                ((sharer.getStateOf(value!!.impIntId)
                        != MarkedMultipleModifyUserDataSharer.UserDataStates.DELETE)
                        || sharer.getStateOf(value.impIntId) == null)
            }
            return stateMatches
        }


        val toUpdate = arrayOfMutableImpIntData.filter { sth -> decide(sth.value) }
            .toCollection(ArrayList())
        impIntRepo.updateImpIntsBasedOn(toUpdate, asUnfinished)

        impIntRepo.deleteAllByIds(removedImpIntsSet)
        removedImpIntsSet = setOf()
    }

    var reminderExisted: MutableLiveData<Boolean> = MutableLiveData()
    private var addedReminder = false
    var reminderShouldBeRemoved = false
    val currentlyRemindOn : MutableLiveData<Instant?> = MutableLiveData()
    private var oldRemindOn : Instant? = null
    private var reminderId: Long = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG

    suspend fun setReminder(instant: Instant) : Pair<Instant?, Boolean> {
        // it is assumed that there was an attempt at adding task
        if (taskId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) return Pair(null, false)
        if (Instant.now() > instant) return Pair(null, true)
        if (reminderExisted.value == true) {
            reminderRepo.updateRemindOn(reminderId, instant)
        }
        else {
            // check if there isn't a reminder in database
            val reminder = reminderRepo.getOneByOwnerTypeAndId(org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK, taskId)
            if (reminder == null) {
                // add it if didn't exist
                val id = reminderRepo.addReminder(instant, org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK, taskId)
                reminderId = id
                addedReminder = true
            }
            else {
                reminderExisted.value = true
                reminderId = reminder.remId
                reminderRepo.updateRemindOn(reminderId, instant)
            }
        }
        return Pair(instant, false)
    }

    suspend fun deleteAddedData() {
        for (iid in addedImpIntsSet) impIntRepo.deleteById(iid)
        for (tid in addedSubtasksSet) taskRepo.deleteById(tid)
        if (addedReminder)
            reminderRepo.deleteById(reminderId)
        else if (reminderExisted.value == true && oldRemindOn != null) {
            reminderRepo.updateRemindOn(reminderId, oldRemindOn!!)
        }
    }

    suspend fun deleteWholeTask() { taskRepo.deleteById(taskId) }

    private suspend fun addMainTask() : Long {
        var retVal = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG

        val ownerIdOrNull
            = (if (ownerTaskId != org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG && getOwnerTaskDepth() >= 0)
                ownerTaskId
               else null)
        val newDepth = getOwnerTaskDepth() + 1

        val newTaskPair = taskRepo.addTask(
            nameOfData.value ?: org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING,
            descriptionOfData.value ?: org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING,
            goalId,
            ownerIdOrNull,
            newDepth,
            priority
        )
        // if not added because depth not ok
        if (!newTaskPair.second) return retVal
        if (newTaskPair.first == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) return retVal

        retVal = newTaskPair.first

        return retVal
    }

    private suspend fun getGoalIdOrNull() : Long? {
        if (goalId != org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) return goalId
        if (ownerTaskObject == null && ownerTaskId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) return null
        if (ownerTaskObject == null) ownerTaskObject = taskRepo.getOneById(ownerTaskId)
        return if (ownerTaskObject == null) null else ownerTaskObject?.goalId
    }

    suspend fun saveMainDataAsUnfinished() : Boolean {
        if (goalId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
            goalId = getGoalIdOrNull() ?: return false
        if (taskId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) {
            taskId = addMainTask()
            if (taskId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) return false
        }
        var task: TaskData? = taskRepo.getOneById(taskId) ?: return false
        taskRepo.markEditing(taskId, true)
        val newUnfData = RecreatingTaskDataFactory.createUserDataBasedOnTexts(
            task!!, nameOfData.value, descriptionOfData.value
        )
        taskRepo.putAsUnfinished(newUnfData)
        task = taskRepo.getOneById(taskId) ?: return false

        updateImpInts(true)

        mutableTaskData.value = task
        addedAnyData = true
        return true
    }
    override suspend fun saveMainData(): Boolean {

        if (goalId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
            goalId = getGoalIdOrNull() ?: return false
        if (taskId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) {
            taskId = addMainTask()
            if (taskId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) return false
        } else {
            taskRepo.updateTexts(  taskId,
                nameOfData.value?: org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING,
                descriptionOfData.value?: org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
            )
            taskRepo.updatePriority(taskId, priority)
        }
        var task: TaskData? = taskRepo.getOneById(taskId) ?: return false
        if (task!!.taskEditUnfinished) {
            taskRepo.markEditing(taskId, false)
        }
        task = taskRepo.getOneById(taskId) ?: return false

        updateImpInts(false)

        if (task.subtasksCount > 0)
            reminderShouldBeRemoved = true

        if (reminderShouldBeRemoved) {
            reminderRepo.deleteById(reminderId)
            reminderId = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG
            oldRemindOn = null
            reminderExisted.value = false
            addedReminder = false
            reminderShouldBeRemoved = false
            currentlyRemindOn.value = null
        }
        else {
            val remind = currentlyRemindOn.value ?: oldRemindOn
            remind?.run { setReminder(this) }
            reminderShouldBeRemoved = false
            oldRemindOn = null
            currentlyRemindOn.value = remind
        }

        mutableTaskData.value = task
        addedAnyData = true
        return true
    }

    override suspend fun getEverything() = getMutex.withLock {
        mutableTaskData.value = taskRepo.getOneById(taskId)
        if (mutableTaskData.value == null) return@withLock
        if (goalId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
            goalId = mutableTaskData.value!!.goalId
        if (ownerTaskId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
            ownerTaskId = mutableTaskData.value!!.taskOwnerId ?: ownerTaskId

        nameOfData.value = mutableTaskData.value?.taskName ?: nameOfData.value
        descriptionOfData.value =
            mutableTaskData.value?.taskDescription ?: descriptionOfData.value

        ownerTaskObject = taskRepo.getOneById(ownerTaskId)
        priority = mutableTaskData.value?.taskPriority ?: priority

        arrayOfMutableSubtasksManager.setUserDataArray(
            taskRepo.getAllByTaskOwnerId(taskId)
        )

        arrayOfMutableImpIntDataManager.setUserDataArray(
            impIntRepo.getAllByOwnerTypeAndId(org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK, taskId)
        )

        val remData = reminderRepo.getOneByOwnerTypeAndId(org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK, taskId)
        if (remData == null) {
            reminderExisted.value = false
        }
        else {
            reminderExisted.value = true
            currentlyRemindOn.value = remData.remindOn
            reminderId = remData.remId
            oldRemindOn = remData.remindOn
        }
    }

}

class TaskDetailsViewModelFactory(private val dbo: TGDatabase,
                                  private val taskId: Long,
                                  private val goalId: Long,
                                  private val ownerTaskId : Long
    ): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TaskDetailsViewModel(dbo, taskId, goalId, ownerTaskId) as T
    }
}