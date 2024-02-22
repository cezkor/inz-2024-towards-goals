package com.example.towardsgoalsapp.habits

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.OwnerType
import com.example.towardsgoalsapp.database.HabitData
import com.example.towardsgoalsapp.database.repositories.HabitRepository
import com.example.towardsgoalsapp.database.ImpIntData
import com.example.towardsgoalsapp.database.repositories.ImpIntRepository
import com.example.towardsgoalsapp.database.userdata.RecreatingHabitDataFactory
import com.example.towardsgoalsapp.database.TGDatabase
import com.example.towardsgoalsapp.etc.DescriptionFixer
import com.example.towardsgoalsapp.database.userdata.ImpIntDataMutableArrayManager
import com.example.towardsgoalsapp.database.userdata.MarkedMultipleModifyUserDataSharer
import com.example.towardsgoalsapp.database.userdata.MutablesArrayContentState
import com.example.towardsgoalsapp.etc.NameFixer
import com.example.towardsgoalsapp.etc.TextsViewModel
import com.example.towardsgoalsapp.database.userdata.ViewModelWithImpIntsSharer
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

open class HabitViewModel(
    private val dbo: TGDatabase,
    private var habitId: Long,
    private var goalId: Long
): TextsViewModel(), ViewModelWithImpIntsSharer {

    protected val habitRepo: HabitRepository = HabitRepository(dbo)
    private val impIntRepo: ImpIntRepository = ImpIntRepository(dbo)

    private val getMutex: Mutex = Mutex()

    val mutableHabitData = MutableLiveData<HabitData>()

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
            viewModelScope.launch {
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

        if (habitId == Constants.IGNORE_ID_AS_LONG) return false
        val iid = impIntRepo.addImpInt(
            Constants.EMPTY_STRING, Constants.EMPTY_STRING, OwnerType.TYPE_HABIT, habitId
        )

        val sharer = getImpIntsSharer()
        sharer.markAddOf(iid)
        sharer.signalAllMayHaveBeenChanged()

        return true
    }

    suspend fun getEverything() = getMutex.withLock {
        mutableHabitData.value = habitRepo.getOneById(habitId)
        if (mutableHabitData.value == null) return@withLock

        if (goalId == Constants.IGNORE_ID_AS_LONG)
            goalId = mutableHabitData.value!!.goalId
        nameOfData.value = mutableHabitData.value!!.habitName
        descriptionOfData.value = mutableHabitData.value!!.habitDescription
        targetNumbers.value = Pair(mutableHabitData.value!!.habitTargetCount,
            mutableHabitData.value!!.habitTargetPeriod)

        arrayOfMutableImpIntDataManager.setUserDataArray(
            impIntRepo.getAllByOwnerTypeAndId(OwnerType.TYPE_HABIT, habitId)
        )
    }

    suspend fun deleteWholeHabit() { habitRepo.deleteById(habitId) }

    suspend fun deleteAddedData()
        { for (iid in addedImpIntsSet) impIntRepo.deleteById(iid) }

    private suspend fun addHabit(): Long {
        return habitRepo.addHabit(
            nameOfData.value ?: Constants.EMPTY_STRING,
            descriptionOfData.value ?: Constants.EMPTY_STRING,
            1,
            1,
            goalId
        )
    }

    val targetNumbers: MutableLiveData<Pair<Long, Long>> = MutableLiveData()

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

    suspend fun saveMainDataAsUnfinished() : Boolean {
        if (goalId == Constants.IGNORE_ID_AS_LONG) return false
        if (habitId == Constants.IGNORE_ID_AS_LONG) {
            habitId = addHabit()
            if (habitId == Constants.IGNORE_ID_AS_LONG) return false
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

        mutableHabitData.value = habit!!
        addedAnyData = true
        return true
    }

    override suspend fun saveMainData(): Boolean {
        if (goalId == Constants.IGNORE_ID_AS_LONG) return false
        if (habitId == Constants.IGNORE_ID_AS_LONG) {
            habitId = addHabit()
            if (habitId == Constants.IGNORE_ID_AS_LONG) return false
        } else habitRepo.updateTexts(  habitId,
            nameOfData.value?: Constants.EMPTY_STRING,
            descriptionOfData.value?: Constants.EMPTY_STRING
        )
        updateTargetPeriodAndCount()
        var habit: HabitData? = habitRepo.getOneById(habitId) ?: return false

        if (habit!!.habitEditUnfinished) {
            habitRepo.markEditing(habitId, false)
        }
        habit = habitRepo.getOneById(habitId)

        updateImpInts(false)

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