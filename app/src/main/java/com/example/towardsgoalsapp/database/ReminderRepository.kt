package com.example.towardsgoalsapp.database

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.OwnerType
import com.example.towardsgoalsapp.database.TaskData
import com.example.towardsgoalsapp.database.HabitData
import com.example.towardsgoalsapp.database.TGDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.IllegalStateException
import java.time.Instant

object FromOldestToNewestComparator: Comparator<ReminderData> {
    override fun compare(o1: ReminderData?, o2: ReminderData?): Int {
        if (o1 == null && o2 == null) return 0
        if (o1 == null) return -1
        if (o2 == null) return 1
        return o1.remindOn.compareTo(o2.remindOn)
    }

}

class ReminderRepository(private val db: TGDatabase) : OwnedByTypedOwnerUserData {

    suspend fun getAll(): List<ReminderData> {
        return withContext(Dispatchers.IO) {
            db.reminderDataQueries.
            selectAll().executeAsList().sortedWith(FromOldestToNewestComparator)
        }
    }
    override suspend fun getAllByOwnerTypeAndId(ownerType: OwnerType, ownerId: Long): ReminderData? {
        return withContext(Dispatchers.IO) {
            db.reminderDataQueries.selectOf(ownerId, ownerType).executeAsOneOrNull()
        }
    }

    override suspend fun getOneById(id: Long): ReminderData? {
        return withContext(Dispatchers.IO) {
            db.reminderDataQueries.selectReminderById(id).executeAsOneOrNull()
        }
    }

    override suspend fun deleteById(id: Long) {
        return withContext(Dispatchers.IO) {
            db.reminderDataQueries.deleteReminder(id)
        }
    }

    suspend fun addReminder(remindOn: Instant,
                            ownerType: OwnerType, ownerId: Long) : Long {
        return withContext(Dispatchers.IO) {
            db.reminderDataQueries.insertOne(
                null,
                remindOn,
                ownerId,
                ownerType
            )
            db.reminderDataQueries.
            lastInsertRowId().executeAsOneOrNull()?: Constants.IGNORE_ID_AS_LONG
        }
    }

    override suspend fun markEditing(id:Long, isUnfinished: Boolean) {
        // not used
    }

    override suspend fun updateTexts(id:Long, firstText: String, secondText: String) {
        // not used
    }

}