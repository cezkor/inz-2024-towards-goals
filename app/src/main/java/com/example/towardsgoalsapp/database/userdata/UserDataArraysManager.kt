package com.example.towardsgoalsapp.database.userdata

import androidx.lifecycle.MutableLiveData
import com.example.towardsgoalsapp.database.HabitData
import com.example.towardsgoalsapp.database.HabitParameter
import com.example.towardsgoalsapp.database.ImpIntData
import com.example.towardsgoalsapp.database.TaskData

class TaskDataMutableArrayManager(
    private val mtArray: ArrayList<MutableLiveData<TaskData>>,
    arrayList: ArrayList<TaskData>? = null
): UserDataManager<TaskData>(mtArray, arrayList) {

    init {
        reputter = TaskDataReputter(mtArray)
        arrayList?.run {
            val p = reputter.setWholeBasedOnArrayList(this)
            contentState.value = p.first
            addedCount.value = p.second
        }
        for (ud in arrayWithMutables)
            ud.value?.run { idToUserData[getIdOf(this)] = this }
    }

    override fun getIdOf(userData: TaskData): Long = userData.taskId

}

class HabitDataMutableArrayManager(
    private val mtArray: ArrayList<MutableLiveData<HabitData>>,
    arrayList: ArrayList<HabitData>? = null
): UserDataManager<HabitData>(mtArray, arrayList) {

    init {
        reputter = HabitDataReputter(mtArray)
        arrayList?.run {
            val p = reputter.setWholeBasedOnArrayList(this)
            contentState.value = p.first
            addedCount.value = p.second
        }
        for (ud in arrayWithMutables)
            ud.value?.run { idToUserData[getIdOf(this)] = this }
    }
    override fun getIdOf(userData: HabitData): Long = userData.habitId

}

class ImpIntDataMutableArrayManager(
    private val mtArray: ArrayList<MutableLiveData<ImpIntData>>,
    arrayList: ArrayList<ImpIntData>? = null
): UserDataManager<ImpIntData>(mtArray, arrayList) {

    init {
        reputter = ImpIntDataReputter(mtArray)
        arrayList?.run {
            val p = reputter.setWholeBasedOnArrayList(this)
            contentState.value = p.first
            addedCount.value = p.second
        }
        for (ud in arrayWithMutables)
            ud.value?.run { idToUserData[getIdOf(this)] = this }
    }

    override fun getIdOf(userData: ImpIntData): Long = userData.impIntId

}

class HabitParameterMutableArrayManager(
    private val mtArray: ArrayList<MutableLiveData<HabitParameter>>,
    arrayList: ArrayList<HabitParameter>? = null
): UserDataManager<HabitParameter>(mtArray, arrayList) {

    init {
        reputter = HabitParameterDataReputter(mtArray)
        arrayList?.run {
            val p = reputter.setWholeBasedOnArrayList(this)
            contentState.value = p.first
            addedCount.value = p.second
        }
        for (ud in arrayWithMutables)
            ud.value?.run { idToUserData[getIdOf(this)] = this }
    }

    override fun getIdOf(userData: HabitParameter): Long = userData.paramId

}

abstract class UserDataManager<UserData> (
    protected val arrayWithMutables: ArrayList<MutableLiveData<UserData>>,
    protected val arrayList: ArrayList<UserData>?
) {

    protected lateinit var reputter: UserDataReputter<UserData>

    protected val idToUserData = HashMap<Long, UserData>()

    val contentState: MutableLiveData<MutablesArrayContentState> = MutableLiveData()

    val addedCount: MutableLiveData<Int> = MutableLiveData(0)

    abstract fun getIdOf(userData: UserData): Long

    fun hasUserDataOfId(userDataId: Long) = idToUserData.containsKey(userDataId)

    private fun getCurrentUserDataById(userDataId: Long) : UserData?
         = idToUserData[userDataId]

    fun setUserDataArray(arrayList: ArrayList<UserData>) {
        val ret = reputter.setWholeBasedOnArrayList(arrayList)
        idToUserData.clear()
        for (ud in arrayWithMutables)
            ud.value?.run { idToUserData[getIdOf(this)] = this }
        addedCount.value = ret.second!!
        contentState.value = ret.first!!
    }

    fun insertWholeArray(arrayList: ArrayList<UserData>) {
        val ret = reputter.reputBasedOnInsertOfArrayList(arrayList)
        for (ud in arrayList)
            idToUserData[getIdOf(ud)] = ud
        addedCount.value = ret.second!!
        contentState.value = ret.first!!
    }

    fun updateOneUserData(userData: UserData)   {
        val ret = reputter.reputBasedOnUpdateOf(userData)
            idToUserData[getIdOf(userData)] = userData
        addedCount.value = ret.second!!
        contentState.value = ret.first!!
    }

    fun insertOneUserData(userData: UserData)  {
        val ret = reputter.reputBasedOnInsertOf(userData)
        idToUserData[getIdOf(userData)] = userData
        addedCount.value = ret.second!!
        contentState.value = ret.first!!
    }

    fun deleteOneOldUserDataById(userDataId : Long) {
        val ud = getCurrentUserDataById(userDataId)
        if (ud == null) {
            addedCount.value = 0
            contentState.value = MutablesArrayContentState.REPUTTED
            return
        }
        val ret = reputter.reputBasedOnDeleteOf(ud)
        idToUserData.remove(userDataId)
        addedCount.value = ret.second!!
        contentState.value = ret.first!!
    }

}