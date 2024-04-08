package com.example.towardsgoalsapp.database.repositories

import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.OwnerType
import com.example.towardsgoalsapp.database.ReminderData
import com.example.towardsgoalsapp.database.TGDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

class ReminderRepository(private val db: TGDatabase) : OwnedByTypedOwnerUserData {

    suspend fun getAll(): List<ReminderData> {
        return withContext(Dispatchers.IO) {
            db.reminderDataQueries.
            selectAll().executeAsList()
        }
    }
    // note: Tasks/Habits have 1-1 mapping with their Reminders
    override suspend fun getAllByOwnerTypeAndId(ownerType: OwnerType, ownerId: Long, allowUnfinished: Boolean): ReminderData? {
        return withContext(Dispatchers.IO) {
            db.reminderDataQueries.selectOf(ownerId, ownerType).executeAsOneOrNull()
        }
    }

    suspend fun getOneByOwnerTypeAndId(ownerType: OwnerType, ownerId: Long)
        : ReminderData? = getAllByOwnerTypeAndId(ownerType, ownerId, false)

    override suspend fun getOneById(id: Long, allowUnfinished: Boolean): ReminderData? {
        // status of finished is unused
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
                remindOn,
                ownerId,
                ownerType
            )
            db.reminderDataQueries.
            lastInsertRowId().executeAsOneOrNull()?: Constants.IGNORE_ID_AS_LONG
        }
    }

    suspend fun updateRemindOn(remId: Long, remindOn: Instant) {
        return withContext(Dispatchers.IO) {
            db.reminderDataQueries.updateRemindOn(remindOn, remId)
        }
    }

    suspend fun updateLastReminded(remId: Long, remindedOn: Instant) {
        return withContext(Dispatchers.IO) {
            db.reminderDataQueries.updateLastReminded(remindedOn, remId)
        }
    }

    override suspend fun markEditing(id:Long, isUnfinished: Boolean) {
        // not used
    }

    override suspend fun updateTexts(id:Long, firstText: String, secondText: String) {
        // not used
    }

}