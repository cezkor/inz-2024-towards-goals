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

abstract class UserDataReputter<UserData>(
    private val arrayWithMutables: ArrayList<MutableLiveData<UserData>>
) {

    private var orderedSetOfUserDataOrder = TreeMap<Long, Int>()

    private var firstWithNullIdx = 0

    abstract fun getOrderNumber(userData: UserData) : Long

    fun setWholeBasedOnArrayList(arrayList: ArrayList<UserData>): Pair<MutablesArrayContentState, Int> {
        // assumption: database returns user data ordered by unique number
        var retVal = MutablesArrayContentState.REPUTTED
        var addNew = 0

        orderedSetOfUserDataOrder.clear()
        if ( arrayWithMutables.size < arrayList.size ) {
            addNew = arrayList.size - arrayWithMutables.size
            arrayWithMutables += (1..addNew).map { MutableLiveData() }
            retVal = MutablesArrayContentState.ADDED_NEW
        }

        for (i in (0..<arrayList.size) ){
            orderedSetOfUserDataOrder[getOrderNumber(arrayList[i])] = i
            arrayWithMutables[i].value = arrayList[i]
        }
        firstWithNullIdx = arrayList.lastIndex + 1

        return Pair(retVal, addNew)
    }

    fun reputBasedOnInsertOfArrayList(arrayList: ArrayList<UserData>): Pair<MutablesArrayContentState, Int>  {
        var retVal = MutablesArrayContentState.REPUTTED
        var addNew = 0

        if ( arrayList.size > arrayWithMutables.size - firstWithNullIdx  ) {
            addNew = arrayList.size + firstWithNullIdx - arrayWithMutables.size
            arrayWithMutables += (1..addNew).map { MutableLiveData() }
            retVal = MutablesArrayContentState.ADDED_NEW
        }

        for (i in (firstWithNullIdx..<firstWithNullIdx + arrayList.size) ){
            arrayWithMutables[i].value = arrayList[i - firstWithNullIdx]
        }
        firstWithNullIdx += arrayList.size

        arrayWithMutables.sortWith { o1, o2 ->
            val v1 = o1.value
            val v2 = o2.value
            val f = v1?.let { getOrderNumber(it) } ?: -1L
            val s = v2?.let { getOrderNumber(it) } ?: -1L
            f.compareTo(s)
        }
        for (i in (0..<arrayWithMutables.size)) {
            arrayWithMutables[i].value?.run {
                orderedSetOfUserDataOrder[getOrderNumber(this)] = i
            }
        }

        return Pair(retVal, addNew)
    }

    fun reputBasedOnDeleteOf(userData: UserData): Pair<MutablesArrayContentState, Int>  {
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
        var retVal = MutablesArrayContentState.REPUTTED
        var addNew = 0

        if (firstWithNullIdx == arrayWithMutables.size) {
            arrayWithMutables.add(MutableLiveData())
            retVal = MutablesArrayContentState.ADDED_NEW
            addNew = 1
        }
        arrayWithMutables[firstWithNullIdx].value = userData

        firstWithNullIdx += 1

        arrayWithMutables.sortWith { o1, o2 ->
            val v1 = o1.value
            val v2 = o2.value
            val f = v1?.let { getOrderNumber(it) } ?: -1L
            val s = v2?.let { getOrderNumber(it) } ?: -1L
            f.compareTo(s)
        }
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
            val pos = orderedSetOfUserDataOrder[ordNum]
            arrayWithMutables[pos!!].value = userData
            Pair(MutablesArrayContentState.REPUTTED, 0)
        } else reputBasedOnInsertOf(userData)
    }

}