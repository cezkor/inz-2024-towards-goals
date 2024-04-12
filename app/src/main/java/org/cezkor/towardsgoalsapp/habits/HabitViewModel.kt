package org.cezkor.towardsgoalsapp.habits

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import org.cezkor.towardsgoalsapp.database.HabitData
import org.cezkor.towardsgoalsapp.database.HabitParameter
import org.cezkor.towardsgoalsapp.database.repositories.HabitRepository
import org.cezkor.towardsgoalsapp.database.ImpIntData
import org.cezkor.towardsgoalsapp.database.repositories.ImpIntRepository
import org.cezkor.towardsgoalsapp.database.userdata.RecreatingHabitDataFactory
import org.cezkor.towardsgoalsapp.database.TGDatabase
import org.cezkor.towardsgoalsapp.database.repositories.HabitParamsRepository
import org.cezkor.towardsgoalsapp.database.repositories.ReminderRepository
import org.cezkor.towardsgoalsapp.database.repositories.StatsDataRepository
import org.cezkor.towardsgoalsapp.database.userdata.HabitParameterMutableArrayManager
import org.cezkor.towardsgoalsapp.etc.DescriptionFixer
import org.cezkor.towardsgoalsapp.database.userdata.ImpIntDataMutableArrayManager
import org.cezkor.towardsgoalsapp.database.userdata.MarkedMultipleModifyUserDataSharer
import org.cezkor.towardsgoalsapp.database.userdata.MutablesArrayContentState
import org.cezkor.towardsgoalsapp.database.userdata.ViewModelWithHabitParamsSharer
import org.cezkor.towardsgoalsapp.etc.NameFixer
import org.cezkor.towardsgoalsapp.etc.TextsViewModel
import org.cezkor.towardsgoalsapp.database.userdata.ViewModelWithImpIntsSharer
import org.cezkor.towardsgoalsapp.etc.OneTimeEvent
import org.cezkor.towardsgoalsapp.etc.OneTimeHandleable
import org.cezkor.towardsgoalsapp.reminders.ReminderLogic
import org.cezkor.towardsgoalsapp.stats.StatsShowing
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

open class HabitViewModel(
    private val dbo: TGDatabase,
    private var habitId: Long,
    private var goalId: Long
): TextsViewModel(), ViewModelWithImpIntsSharer, ViewModelWithHabitParamsSharer {

    protected val habitRepo: HabitRepository = HabitRepository(dbo)
    protected val habitParamsRepo: HabitParamsRepository = HabitParamsRepository(dbo)
    protected val statsDataRepo: StatsDataRepository = StatsDataRepository(dbo)
    protected val impIntRepo: ImpIntRepository = ImpIntRepository(dbo)
    protected val reminderRepo: ReminderRepository = ReminderRepository(dbo)

    private val getMutex: Mutex = Mutex()

    val mutableHabitData = MutableLiveData<HabitData>()

    var currentTabIdx: Int = -1
    var setTabEvent: MutableLiveData<OneTimeEvent> = MutableLiveData()

    suspend fun getEverything() = getMutex.withLock {
        mutableHabitData.value = habitRepo.getOneById(habitId)
        if (mutableHabitData.value == null) return@withLock

        if (goalId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
            goalId = mutableHabitData.value!!.goalId
        nameOfData.value = mutableHabitData.value!!.habitName
        descriptionOfData.value = mutableHabitData.value!!.habitDescription
        targetNumbers.value = Pair(mutableHabitData.value!!.habitTargetCount,
            mutableHabitData.value!!.habitTargetPeriod)

        arrayOfMutableImpIntDataManager.setUserDataArray(
            impIntRepo.getAllByOwnerTypeAndId(org.cezkor.towardsgoalsapp.OwnerType.TYPE_HABIT, habitId)
        )

        arrayOfMutableHabitParamsManager.setUserDataArray(
            habitParamsRepo.getAllByOwnerId(habitId)
        )

        checkIfCanShowHabitStats()

        val remData = reminderRepo.getOneByOwnerTypeAndId(org.cezkor.towardsgoalsapp.OwnerType.TYPE_HABIT, habitId)
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

    val canShowHabitStatsAtAll: MutableLiveData<Boolean> = MutableLiveData(false)
    suspend fun checkIfCanShowHabitStats() {
        canShowHabitStatsAtAll.value =
            StatsShowing.canShowHabitGeneralStats(statsDataRepo, habitId) &&
            StatsShowing.canShowHabitParamsStats(habitParamsRepo, habitId)
    }

    val arrayOfMutableImpIntData: ArrayList<MutableLiveData<ImpIntData>> =
        java.util.ArrayList()

    private var addedImpIntsSet: Set<Long> = setOf()
    private var removedImpIntsSet: Set<Long> = setOf()

    private val arrayOfMutableImpIntDataManager: ImpIntDataMutableArrayManager
            = ImpIntDataMutableArrayManager(arrayOfMutableImpIntData)

    private val impIntsSharer: MarkedMultipleModifyUserDataSharer<ImpIntData> = object :
        MarkedMultipleModifyUserDataSharer<ImpIntData>() {

        override fun getArrayOfUserData(): ArrayList<MutableLiveData<ImpIntData>>?
                = arrayOfMutableImpIntData
        override fun signalAllMayHaveBeenChanged() {
            viewModelScope.launch(exceptionHandler) {
                val manager = arrayOfMutableImpIntDataManager
                for (key in changeMap.keys) {
                    if (changeMap[key] == UserDataStates.DELETE) {
                        manager.deleteOneOldUserDataById(key)
                        removedImpIntsSet = removedImpIntsSet.plus(key)
                        changeMap.remove(key)
                    }
                    if (changeMap[key] == UserDataStates.ADD) {
                        val ii = impIntRepo.getOneById(key)
                        ii?.run { manager.insertOneUserData(ii) }
                        addedImpIntsSet = addedImpIntsSet.plus(key)
                        markChangeOf(key)
                    }
                }
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

    suspend fun addOneNewEmptyImpInt() : Boolean {

        if (habitId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) return false
        val iid = impIntRepo.addImpInt(
            org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING, org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING, org.cezkor.towardsgoalsapp.OwnerType.TYPE_HABIT, habitId
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

    val arrayOfMutableHabitParams: ArrayList<MutableLiveData<HabitParameter>> =
        java.util.ArrayList()

    private var addedHabitParamsSet: Set<Long> = setOf()
    private var removedHabitParamsSet: Set<Long> = setOf()

    private val arrayOfMutableHabitParamsManager: HabitParameterMutableArrayManager
            = HabitParameterMutableArrayManager(arrayOfMutableHabitParams)

    private val habitParamsSharer: MarkedMultipleModifyUserDataSharer<HabitParameter> = object :
        MarkedMultipleModifyUserDataSharer<HabitParameter>() {

        override fun getArrayOfUserData(): ArrayList<MutableLiveData<HabitParameter>>?
                = arrayOfMutableHabitParams
        override fun signalAllMayHaveBeenChanged() {
            viewModelScope.launch(exceptionHandler) {
                val manager = arrayOfMutableHabitParamsManager
                for (key in changeMap.keys) {
                    if (changeMap[key] == UserDataStates.DELETE) {
                        manager.deleteOneOldUserDataById(key)
                        removedHabitParamsSet = removedHabitParamsSet.plus(key)
                        changeMap.remove(key)
                    }
                    if (changeMap[key] == UserDataStates.ADD) {
                        val ii = habitParamsRepo.getOneById(key)
                        ii?.run { manager.insertOneUserData(ii) }
                        addedHabitParamsSet = addedHabitParamsSet.plus(key)
                        markChangeOf(key)
                    }
                }
            }
        }

        override val arrayState: MutableLiveData<MutablesArrayContentState>
            get() = arrayOfMutableHabitParamsManager.contentState
        override val addedCount: MutableLiveData<Int>
            get() = arrayOfMutableHabitParamsManager.addedCount

        override fun isArrayConsideredEmpty(): Boolean {
            return arrayOfMutableHabitParams.none { md -> md.value != null } ||
                    arrayOfMutableHabitParams.isEmpty()
        }


    }
    override fun getHabitParamsSharer(): MarkedMultipleModifyUserDataSharer<HabitParameter>
        = habitParamsSharer

    suspend fun addOneNewEmptyHabitParam() : Boolean {

        if (habitId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) return false
        val iid = habitParamsRepo.addHabitParam(
            habitId, org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING, 0.0, null
        )

        val sharer = getHabitParamsSharer()
        sharer.markAddOf(iid)
        sharer.signalAllMayHaveBeenChanged()

        return true
    }

    private suspend fun updateHabitParams(asUnfinished: Boolean){
        fun decide(value: HabitParameter?) : Boolean {
            val sharer: MarkedMultipleModifyUserDataSharer<HabitParameter> = getHabitParamsSharer()
            val valueNotNull = value != null
            if (! valueNotNull) return false
            val stateMatches = if (asUnfinished) {
                ( sharer.getStateOf(value!!.paramId)
                        == MarkedMultipleModifyUserDataSharer.UserDataStates.CHANGE )
            } else {
                ((sharer.getStateOf(value!!.paramId)
                        != MarkedMultipleModifyUserDataSharer.UserDataStates.DELETE)
                        || sharer.getStateOf(value.paramId) == null)
            }
            return stateMatches
        }


        val toUpdate = arrayOfMutableHabitParams.filter { sth -> decide(sth.value) }
            .toCollection(ArrayList())
        habitParamsRepo.updateHabitParamsBasedOn(toUpdate, asUnfinished)

        habitParamsRepo.deleteAllByIds(removedHabitParamsSet)
        removedHabitParamsSet = setOf()
    }

    var reminderExisted: MutableLiveData<Boolean> = MutableLiveData()
    private var addedReminder = false
    var reminderShouldBeRemoved = false
    val currentlyRemindOn : MutableLiveData<Instant> = MutableLiveData()
    private var oldRemindOn : Instant? = null

    var reminderId: Long = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG

    suspend fun setReminder(time: LocalTime) : Instant? {
        if (habitId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) return null
        val reminderWillFireAt = ReminderLogic.prepareInstantForNextDayBasedOnHourAndToday(time)
        if (reminderExisted.value == true) {
            reminderRepo.updateRemindOn(reminderId, reminderWillFireAt)
        }
        else {
            val reminder = reminderRepo.getOneByOwnerTypeAndId(org.cezkor.towardsgoalsapp.OwnerType.TYPE_HABIT, habitId)
            if (reminder == null) {
                // add it if didn't exist
                val id = reminderRepo.addReminder(reminderWillFireAt, org.cezkor.towardsgoalsapp.OwnerType.TYPE_HABIT, habitId)
                reminderId = id
                addedReminder = true
            }
            else {
                reminderExisted.value = true
                reminderId = reminder.remId
                reminderRepo.updateRemindOn(reminderId, reminderWillFireAt)
            }
        }
        return reminderWillFireAt
    }

    val assureOfExistingHabitHandleableMutable = MutableLiveData<OneTimeHandleable>()

    suspend fun deleteWholeHabit() { habitRepo.deleteById(habitId) }

    suspend fun deleteAddedData() {
        for (iid in addedImpIntsSet) impIntRepo.deleteById(iid)
        for (pid in addedHabitParamsSet) habitParamsRepo.deleteById(pid)
        if (addedReminder)
            reminderRepo.deleteById(reminderId)
        else if (reminderExisted.value == true && oldRemindOn != null) {
            reminderRepo.updateRemindOn(reminderId, oldRemindOn!!)
        }
    }

    private suspend fun addHabit(): Long {
        return habitRepo.addHabit(
            nameOfData.value ?: org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING,
            descriptionOfData.value ?: org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING,
            1,
            1,
            goalId
        )
    }

    val targetNumbers: MutableLiveData<Pair<Long, Long>> = MutableLiveData(Pair(1L, 1L))

    private suspend fun updateTargetPeriodAndCount() : Boolean{
        val tCount = targetNumbers.value?.first ?: mutableHabitData.value?.habitTargetCount
        val tPeriod = targetNumbers.value?.second ?: mutableHabitData.value?.habitTargetPeriod
        if (tCount == null || tPeriod == null) return false
        habitRepo.updateTargets(habitId, tCount, tPeriod)
        return true
    }

    fun putTargets(targetCount: Long, targetPeriod: Long) {
        targetNumbers.value = Pair(targetCount, targetPeriod)
    }

    override fun putTexts(newName: String?, newDescription: String?) {
        var name: String? = newName ?: mutableHabitData.value?.habitName
        var desc: String? = newDescription ?: mutableHabitData.value?.habitDescription
        if (name == null || desc == null) return
        name = NameFixer.fix(name)
        desc = DescriptionFixer.fix(desc)
        nameOfData.value = name
        descriptionOfData.value = desc
    }

    suspend fun saveMainDataAsUnfinished() : Boolean {
        if (goalId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) return false
        if (habitId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) {
            habitId = addHabit()
            if (habitId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) return false
        }
        var habit: HabitData? = habitRepo.getOneById(habitId) ?: return false
        habitRepo.markEditing(habitId, true)
        var newUnfData = RecreatingHabitDataFactory.createUserDataBasedOnTexts(
            habit!!, nameOfData.value, descriptionOfData.value
        )
        newUnfData = RecreatingHabitDataFactory.createUserDataBasedOnTargets(
            newUnfData, targetNumbers.value?.first, targetNumbers.value?.second
        )
        habitRepo.putAsUnfinished(newUnfData)
        habit = habitRepo.getOneById(goalId) ?: return false

        updateImpInts(true)

        updateHabitParams(true)

        mutableHabitData.value = habit!!
        addedAnyData = true
        return true
    }

    override suspend fun saveMainData(): Boolean {
        if (goalId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) return false
        if (habitId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) {
            habitId = addHabit()
            if (habitId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) return false
        } else habitRepo.updateTexts(  habitId,
            nameOfData.value?: org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING,
            descriptionOfData.value?: org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
        )
        updateTargetPeriodAndCount()
        var habit: HabitData? = habitRepo.getOneById(habitId) ?: return false

        if (habit!!.habitEditUnfinished) {
            habitRepo.markEditing(habitId, false)
        }
        habit = habitRepo.getOneById(habitId)

        updateImpInts(false)

        updateHabitParams(false)

        if (reminderShouldBeRemoved) {
            reminderRepo.deleteById(reminderId)
            reminderId = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG
            oldRemindOn = null
            reminderExisted.value = false
            addedReminder = false
            reminderShouldBeRemoved = false
        }
        else {
            val remind = currentlyRemindOn.value ?: oldRemindOn
            remind?.run {
                val lt = this.atZone(ZoneId.systemDefault()).toLocalTime()
                setReminder(lt)
            }
        }

        mutableHabitData.value = habit!!

        addedAnyData = true
        return true
    }

}

class HabitViewModelFactory(private val dbo: TGDatabase,
                            private val habitId: Long,
                            private val goalId: Long
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HabitViewModel(dbo, habitId, goalId) as T
    }
}