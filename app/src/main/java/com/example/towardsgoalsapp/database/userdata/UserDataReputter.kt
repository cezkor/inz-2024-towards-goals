package com.example.towardsgoalsapp.database.userdata

import androidx.lifecycle.MutableLiveData
import java.util.TreeMap
import com.example.towardsgoalsapp.database.*

enum class MutablesArrayContentState {
    ADDED_NEW, REPUTTED
}

class TaskDataReputter(
    private val mtArray: ArrayList<MutableLiveData<TaskData>>
): UserDataReputter<TaskData>(mtArray) {

    override fun getOrderNumber(userData: TaskData): Long {
        return userData.taskId
    }
}

class HabitDataReputter(
    private val mtArray: ArrayList<MutableLiveData<HabitData>>
): UserDataReputter<HabitData>(mtArray) {

    override fun getOrderNumber(userData: HabitData): Long {
        return userData.habitId
    }
}

class ImpIntDataReputter(
    private val mtArray: ArrayList<MutableLiveData<ImpIntData>>
): UserDataReputter<ImpIntData>(mtArray) {

    override fun getOrderNumber(userData: ImpIntData): Long {
        return userData.impIntId
    }
}

class HabitParameterDataReputter(
    private val mtArray: ArrayList<MutableLiveData<HabitParameter>>
): UserDataReputter<HabitParameter>(mtArray) {

    override fun getOrderNumber(userData: HabitParameter): Long {
        return userData.paramId
    }
}

// class "reputs"
// (that is, changes data in given MutableLiveData
// by either updating, inserting or setting it to null)
// data in MutableLiveData array
// notifies whether it had to extend the array with MutableLiveData objects with new ones
// or to just "reput"
// after each operation

abstract class UserDataReputter<UserData>(
    private val arrayWithMutables: ArrayList<MutableLiveData<UserData>>
) {

    // map of order number to position in array
    private var orderedSetOfUserDataOrder = TreeMap<Long, Int>()
    // index of first MutableLiveData with null (all MTDs after it are to be also null)
    private var firstWithNullIdx = 0

    // this method specifies how to acquire the order number; it should be a unique number
    // the number specifies order of UserData
    abstract fun getOrderNumber(userData: UserData) : Long

    fun setWholeBasedOnArrayList(arrayList: ArrayList<UserData>): Pair<MutablesArrayContentState, Int> {
        // it is assumed that giver array is already ordered by a unique number

        // indicates whether this method
        // has only put data into existing MutableLiveData objects in array (REPUTTED)
        // or also added new MutableLiveData objects to accommodate new UserData objects (ADDED_NEW)
        // it is required to pass information about what has happened as
        // views have to observe newly added MutableLiveData objects
        var retVal = MutablesArrayContentState.REPUTTED
        // how many MutableLiveData objects were added, if any
        var addNew = 0

        orderedSetOfUserDataOrder.clear() // reset order map

        if ( arrayWithMutables.size < arrayList.size ) {
            // add new MutableLiveData (MLD) objects, note that
            addNew = arrayList.size - arrayWithMutables.size
            arrayWithMutables += (1..addNew).map { MutableLiveData() }
            retVal = MutablesArrayContentState.ADDED_NEW
        }
        // add data from array
        for (i in (0..<arrayList.size) ){
            orderedSetOfUserDataOrder[getOrderNumber(arrayList[i])] = i
            arrayWithMutables[i].value = arrayList[i]
        }
        // set index of first MLD with null, or the size of array if there are no more MLD objects
        firstWithNullIdx = arrayList.lastIndex + 1

        return Pair(retVal, addNew)
    }

    fun reputBasedOnDeleteOf(userData: UserData): Pair<MutablesArrayContentState, Int>  {
        // deletion of UserData never adds new MutableLiveData objects, only sets at least
        // one to null if it contained data
        val position = orderedSetOfUserDataOrder[getOrderNumber(userData)]
            ?: return Pair(MutablesArrayContentState.REPUTTED, 0)

        orderedSetOfUserDataOrder.remove(getOrderNumber(userData))

        for (i in (position..<firstWithNullIdx) ){
            if (i != arrayWithMutables.lastIndex)
                arrayWithMutables[i].value = arrayWithMutables[i+1].value
            else
                arrayWithMutables[i].value = null
            arrayWithMutables[i].value?.run {
                orderedSetOfUserDataOrder[getOrderNumber(this)] = i
            }
        }
        firstWithNullIdx -= 1
        return Pair(MutablesArrayContentState.REPUTTED, 0)
    }

    fun reputBasedOnInsertOf(userData: UserData): Pair<MutablesArrayContentState, Int> {
        // see setWholeBasedOnArrayList()
        var retVal = MutablesArrayContentState.REPUTTED
        var addNew = 0

        // if there are no more MutableLiveData objects in array, add new one and note that
        if (firstWithNullIdx == arrayWithMutables.size) {
            arrayWithMutables.add(MutableLiveData())
            retVal = MutablesArrayContentState.ADDED_NEW
            addNew = 1
        }
        arrayWithMutables[firstWithNullIdx].value = userData

        firstWithNullIdx += 1 // move index

        // sort the array of mutables, nulls are treated as always bigger
        arrayWithMutables.sortWith { o1, o2 ->
            val v1 = o1.value
            val v2 = o2.value
            val f = v1?.let { getOrderNumber(it) }
            val s = v2?.let { getOrderNumber(it) }
            if (f == null && s == null) return@sortWith 0
            if (f == null) return@sortWith 1
            if (s == null) return@sortWith -1
            f.compareTo(s)
        }
        // note the possibly new positions in the array
        for (i in (0..<arrayWithMutables.size)) {
            arrayWithMutables[i].value?.run {
                orderedSetOfUserDataOrder[getOrderNumber(this)] = i
            }
        }

        return Pair(retVal, addNew)
    }

    fun reputBasedOnUpdateOf(userData: UserData) : Pair<MutablesArrayContentState, Int> {
        val ordNum = getOrderNumber(userData)
        return if (orderedSetOfUserDataOrder.containsKey(ordNum)) {
            // if it contains object, update it
            val pos = orderedSetOfUserDataOrder[ordNum]
            arrayWithMutables[pos!!].value = userData
            // it didn't change size of array
            Pair(MutablesArrayContentState.REPUTTED, 0)
        } else reputBasedOnInsertOf(userData) // insert as it didn't exist
    }

}