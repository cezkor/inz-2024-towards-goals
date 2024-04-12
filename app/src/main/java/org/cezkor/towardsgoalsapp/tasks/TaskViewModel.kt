package org.cezkor.towardsgoalsapp.tasks

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import org.cezkor.towardsgoalsapp.etc.TextsViewModel
import org.cezkor.towardsgoalsapp.database.*
import org.cezkor.towardsgoalsapp.database.repositories.ImpIntRepository
import org.cezkor.towardsgoalsapp.database.repositories.ReminderRepository
import org.cezkor.towardsgoalsapp.database.repositories.StatsDataRepository
import org.cezkor.towardsgoalsapp.database.repositories.TaskRepository
import org.cezkor.towardsgoalsapp.etc.DescriptionFixer
import org.cezkor.towardsgoalsapp.database.userdata.ImpIntDataMutableArrayManager
import org.cezkor.towardsgoalsapp.database.userdata.MutablesArrayContentState
import org.cezkor.towardsgoalsapp.database.userdata.MarkedMultipleModifyUserDataSharer
import org.cezkor.towardsgoalsapp.etc.NameFixer
import org.cezkor.towardsgoalsapp.database.userdata.ViewModelWithImpIntsSharer
import org.cezkor.towardsgoalsapp.stats.StatsShowing
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

open class TaskViewModel(
    private val dbo: TGDatabase,
    private val taskId: Long,
    private val goalId: Long
): TextsViewModel(), ViewModelWithImpIntsSharer {

    protected val taskRepo: TaskRepository = TaskRepository(dbo)
    protected val impIntRepo: ImpIntRepository = ImpIntRepository(dbo)
    protected val statsRepo: StatsDataRepository = StatsDataRepository(dbo)
    protected val reminderRepo: ReminderRepository = ReminderRepository(dbo)

    protected val getMutex: Mutex = Mutex()

    val mutableTaskData = MutableLiveData<TaskData>()

    val arrayOfMutableImpIntData: ArrayList<MutableLiveData<ImpIntData>> =
        java.util.ArrayList()

    protected val arrayOfMutableImpIntDataManager: ImpIntDataMutableArrayManager
        = ImpIntDataMutableArrayManager(arrayOfMutableImpIntData)

    val canShowTasksStats: MutableLiveData<Boolean> = MutableLiveData(false)
    suspend fun checkIfCanShowTasksStats() = StatsShowing.canShowTaskGeneralStats(statsRepo, taskId)

    protected var addedImpIntsSet: Set<Long> = setOf()
    protected var removedImpIntsSet: Set<Long> = setOf()

    private val impIntsSharer: MarkedMultipleModifyUserDataSharer<ImpIntData> = object :
        MarkedMultipleModifyUserDataSharer<ImpIntData>() {

        override fun getArrayOfUserData(): ArrayList<MutableLiveData<ImpIntData>>?
            = arrayOfMutableImpIntData
        override fun signalAllMayHaveBeenChanged() {
            viewModelScope.launch {
                val manager = arrayOfMutableImpIntDataManager
                for (key in changeMap.keys) {
                    if (changeMap[key] == UserDataStates.DELETE) {
                        manager.deleteOneOldUserDataById(key)
                        changeMap.remove(key)
                        removedImpIntsSet = removedImpIntsSet.plus(key)
                    }
                    if (changeMap[key] == UserDataStates.ADD) {
                        val ii = impIntRepo.getOneById(key)
                        ii?.run { manager.insertOneUserData(ii) }
                        addedImpIntsSet = addedImpIntsSet.plus(key)
                        markChangeOf(key)
                    }
                }
                // change will be handled outside of sharer
            }
        }

        override val arrayState: MutableLiveData<MutablesArrayContentState>
            get() = arrayOfMutableImpIntDataManager.contentState
        override val addedCount: MutableLiveData<Int>
            get() = arrayOfMutableImpIntDataManager.addedCount

        override fun isArrayConsideredEmpty(): Boolean {
            return arrayOfMutableImpIntData.none { md -> md.value != null } ||
                    arrayOfMutableImpIntData.isEmpty()
        }


    }
    override fun getImpIntsSharer(): MarkedMultipleModifyUserDataSharer<ImpIntData>
        = impIntsSharer

    open suspend fun getEverything() {}

    override fun putTexts(newName: String?, newDescription: String?) {
        var name: String? = newName ?: mutableTaskData.value?.taskName
        var desc: String? = newDescription ?: mutableTaskData.value?.taskDescription
        if (name == null || desc == null) return
        name = NameFixer.fix(name)
        desc = DescriptionFixer.fix(desc)
        nameOfData.value = name
        descriptionOfData.value = desc
    }

    override suspend fun saveMainData(): Boolean {
        addedAnyData = true
        return true
    }

}