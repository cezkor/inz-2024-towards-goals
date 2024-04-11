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
        onInit()
    }

    override fun getIdOf(userData: TaskData): Long = userData.taskId

    override fun getReputter(mtArray: ArrayList<MutableLiveData<TaskData>>)
    : UserDataReputter<TaskData> {
        return TaskDataReputter(mtArray)
    }

}

class HabitDataMutableArrayManager(
    private val mtArray: ArrayList<MutableLiveData<HabitData>>,
    arrayList: ArrayList<HabitData>? = null
): UserDataManager<HabitData>(mtArray, arrayList) {

    init {
        onInit()
    }
    override fun getIdOf(userData: HabitData): Long = userData.habitId

    override fun getReputter(mtArray: ArrayList<MutableLiveData<HabitData>>)
            : UserDataReputter<HabitData> {
        return HabitDataReputter(mtArray)
    }

}

class ImpIntDataMutableArrayManager(
    private val mtArray: ArrayList<MutableLiveData<ImpIntData>>,
    arrayList: ArrayList<ImpIntData>? = null
): UserDataManager<ImpIntData>(mtArray, arrayList) {

    init {
        onInit()
    }

    override fun getIdOf(userData: ImpIntData): Long = userData.impIntId

    override fun getReputter(mtArray: ArrayList<MutableLiveData<ImpIntData>>)
    : UserDataReputter<ImpIntData> {
        return ImpIntDataReputter(mtArray)
    }
}

class HabitParameterMutableArrayManager(
    private val mtArray: ArrayList<MutableLiveData<HabitParameter>>,
    arrayList: ArrayList<HabitParameter>? = null
): UserDataManager<HabitParameter>(mtArray, arrayList) {

    init {
        onInit()
    }

    override fun getIdOf(userData: HabitParameter): Long = userData.paramId
    override fun getReputter(mtArray: ArrayList<MutableLiveData<HabitParameter>>)
    : UserDataReputter<HabitParameter> {
        return HabitParameterDataReputter(mtArray)
    }

}

abstract class UserDataManager<UserData> (
    protected val arrayWithMutables: ArrayList<MutableLiveData<UserData>>,
    protected val arrayList: ArrayList<UserData>?
) {

    // this method as to be called for every non-abstract class of this class on init
    protected fun onInit() {
        this.reputter = getReputter(arrayWithMutables)
        arrayList?.run {
            // set data for reputter, pass its state
            val p = reputter.setWholeBasedOnArrayList(this)
            contentState.value = p.first!!
            addedCount.value = p.second!!
        }
        // put user data to id map
        for (ud in arrayWithMutables)
            ud.value?.run { idToUserData[getIdOf(this)] = this }
    }

    protected lateinit var reputter: UserDataReputter<UserData>

    protected val idToUserData = HashMap<Long, UserData>()

    val contentState: MutableLiveData<MutablesArrayContentState> = MutableLiveData()

    val addedCount: MutableLiveData<Int> = MutableLiveData(0)

    // this method specifies objects of this class how to get id of given UserData object
    abstract fun getIdOf(userData: UserData): Long

    // this method specifies how to acquire UserDataReputter for UserData class
    protected abstract fun getReputter(mtArray: ArrayList<MutableLiveData<UserData>>) :
        UserDataReputter<UserData>

    fun hasUserDataOfId(userDataId: Long) = idToUserData.containsKey(userDataId)

    // returns UserData of given id if it existed in the array
    private fun getCurrentUserDataById(userDataId: Long) : UserData?
         = idToUserData[userDataId]

    fun setUserDataArray(arrayList: ArrayList<UserData>) {
        val ret = reputter.setWholeBasedOnArrayList(arrayList)
        // reset id to data map
        idToUserData.clear()
        // add user data to map
        for (ud in arrayWithMutables)
            ud.value?.run { idToUserData[getIdOf(this)] = this }
        // note: it is preferable to listen to contentState as it gets updated later than addedCount
        addedCount.value = ret.second!!
        contentState.value = ret.first!!
    }

    fun updateOneUserData(userData: UserData)   {
        // reput with reputter, update map, pass state
        val ret = reputter.reputBasedOnUpdateOf(userData)
        idToUserData[getIdOf(userData)] = userData
        addedCount.value = ret.second!!
        contentState.value = ret.first!!
    }

    fun insertOneUserData(userData: UserData)  {
        // insert with reputter, insert to map, pass state
        val ret = reputter.reputBasedOnInsertOf(userData)
        idToUserData[getIdOf(userData)] = userData
        addedCount.value = ret.second!!
        contentState.value = ret.first!!
    }

    fun deleteOneOldUserDataById(userDataId : Long) {
        // delete with reputter if given user data existed
        val ud = getCurrentUserDataById(userDataId)
        if (ud == null) {
            // treat it as all data in array was only reputted (no change of size of array)
            addedCount.value = 0
            contentState.value = MutablesArrayContentState.REPUTTED
            return
        }
        // otherwise, remove with reputter and from map, pass state
        val ret = reputter.reputBasedOnDeleteOf(ud)
        idToUserData.remove(userDataId)
        addedCount.value = ret.second!!
        contentState.value = ret.first!!
    }

}