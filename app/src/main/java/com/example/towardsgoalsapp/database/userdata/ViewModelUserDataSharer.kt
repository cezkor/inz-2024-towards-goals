package com.example.towardsgoalsapp.database.userdata

import androidx.lifecycle.MutableLiveData
import com.example.towardsgoalsapp.database.HabitData
import com.example.towardsgoalsapp.database.HabitParameter
import com.example.towardsgoalsapp.database.ImpIntData
import com.example.towardsgoalsapp.database.TaskData

interface ViewModelUserDataSharer<UserData> {

    fun getArrayOfUserData(): ArrayList<MutableLiveData<UserData>>?

    val arrayState: MutableLiveData<MutablesArrayContentState>

    val addedCount: MutableLiveData<Int>

    fun isArrayConsideredEmpty() : Boolean

}

interface OneModifyUserDataSharer<UserData> : ViewModelUserDataSharer<UserData> {
    // this class should decide if given object was changed
    fun signalNeedOfChangeFor(userDataId: Long)

}

abstract class MarkedMultipleModifyUserDataSharer<UserData> : ViewModelUserDataSharer<UserData> {
    // this class lets mark modification and will do accordingly to UserDataState
    enum class UserDataStates {
        DELETE, CHANGE, ADD
    }

    protected val changeMap: HashMap<Long, UserDataStates> = HashMap()

    fun getStateOf(userDataId: Long) : UserDataStates? {
        return changeMap[userDataId]
    }

    // changes should NOT be brought to database
    // - it will be decided by whoever creates this sharer
    abstract fun signalAllMayHaveBeenChanged()

    fun markDeleteOf(userDataId: Long) { changeMap[userDataId] = UserDataStates.DELETE
    }

    fun markChangeOf(userDataId: Long) { changeMap[userDataId] = UserDataStates.CHANGE
    }

    fun markAddOf(userDataId: Long) { changeMap[userDataId] = UserDataStates.ADD
    }
}

interface ViewModelWithTasksSharer {
    fun getTasksSharer() : ViewModelUserDataSharer<TaskData>?
}

interface ViewModelWithManyTasksSharers {
    fun getTasksSharer(byOwnerId: Long) : ViewModelUserDataSharer<TaskData>?
}

interface ViewModelWithManyHabitsSharers {
    fun getHabitsSharer(byOwnerId: Long) : ViewModelUserDataSharer<HabitData>?
}


interface ViewModelWithHabitsSharer {
    fun getHabitsSharer() : ViewModelUserDataSharer<HabitData>?
}

interface ViewModelWithImpIntsSharer {
    fun getImpIntsSharer() : ViewModelUserDataSharer<ImpIntData>?
}

interface ViewModelWithHabitParamsSharer {
    fun getHabitParamsSharer() : ViewModelUserDataSharer<HabitParameter>?
}