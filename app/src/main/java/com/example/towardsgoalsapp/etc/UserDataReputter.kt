package com.example.towardsgoalsapp.etc

import androidx.lifecycle.MutableLiveData
import java.util.TreeMap

enum class MutablesArrayContentState {
    ADDED_NEW, REPUTTED
}



abstract class UserDataReputter<UserData>(
    private val arrayWithMutables: ArrayList<MutableLiveData<UserData>>,
    arrayList: ArrayList<UserData>? = null
) {

    private var orderedSetOfUserDataOrder = TreeMap<Long, Int>()

    private var firstWithNullIdx = 0

    init {
        arrayList?.run {
            setWholeBasedOnArrayList(this)
        }
    }


    abstract fun getOrderNumber(userData: UserData) : Long

    fun setWholeBasedOnArrayList(arrayList: ArrayList<UserData>): MutablesArrayContentState {
        // assumption: database returns user data ordered by id
        var retVal = MutablesArrayContentState.REPUTTED

        orderedSetOfUserDataOrder.clear()
        if ( arrayWithMutables.size < arrayList.size ) {
            val addNew = arrayList.size - arrayWithMutables.size
            arrayWithMutables += (1..addNew).map { MutableLiveData() }
            retVal = MutablesArrayContentState.ADDED_NEW
        }

        for (i in (0..<arrayList.size) ){
            orderedSetOfUserDataOrder[getOrderNumber(arrayList[i])] = i
            arrayWithMutables[i].value = arrayList[i]
        }
        firstWithNullIdx = arrayList.lastIndex + 1

        return retVal
    }

    fun reputBasedOnInsertOfArrayList(arrayList: ArrayList<UserData>): MutablesArrayContentState {
        var retVal = MutablesArrayContentState.REPUTTED

        if ( arrayList.size > arrayWithMutables.size - firstWithNullIdx  ) {
            val addNew = arrayList.size + firstWithNullIdx - arrayWithMutables.size
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

        return retVal
    }

    fun reputBasedOnDeleteOf(userData: UserData): MutablesArrayContentState {
        val position = orderedSetOfUserDataOrder[getOrderNumber(userData)]
            ?: return MutablesArrayContentState.REPUTTED

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
        return MutablesArrayContentState.REPUTTED
    }

    fun reputBasedOnInsertOf(userData: UserData): MutablesArrayContentState {
        var retVal = MutablesArrayContentState.REPUTTED



        if (firstWithNullIdx == arrayWithMutables.size) {
            arrayWithMutables.add(MutableLiveData())
            retVal = MutablesArrayContentState.ADDED_NEW
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

        return retVal
    }

}