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
class ImpIntRepository(private val db: TGDatabase) : OwnedByTypedOwnerUserData {
    override suspend fun getAllByOwnerTypeAndId(ownerType: OwnerType, ownerId: Long): ArrayList<ImpIntData> {
        return withContext(Dispatchers.IO) {
            db.impIntsDataQueries.selectAllOf(ownerId, ownerType).executeAsList()
                .toCollection(ArrayList())
        }
    }

    override suspend fun updateTexts(id:Long, firstText: String, secondText: String) {
        return withContext(Dispatchers.IO) {
            db.impIntsDataQueries.updateImpIntTexts(firstText, secondText, id)
        }
    }

    override suspend fun getOneById(id: Long): ImpIntData? {
        return withContext(Dispatchers.IO) {
            val unfinished = db.impIntsDataQueries.getImpIntUnfinished(id).executeAsList().last()
            if (unfinished) {
                val uii = db.impIntsDataQueries.selectGivenUnfinishedImpInt(id).executeAsOneOrNull()
                if (uii == null) null else {
                    ImpIntData(
                        uii.impIntId,
                        true,
                        uii.impIntIfText,
                        uii.impIntThenText,
                        uii.ownerId,
                        uii.ownerType
                    )
                }
            }
            else {
                db.impIntsDataQueries.selectGivenImpInt(id).executeAsOneOrNull()
            }
        }
    }

    override suspend fun deleteById(id: Long) {
        return withContext(Dispatchers.IO) {
            db.impIntsDataQueries.deleteImpInt(id)
        }
    }

    suspend fun addImpInt( impIntIfText: String, impIntThenText: String,
                           ownerType: OwnerType, ownerId: Long) : Long {
        return withContext(Dispatchers.IO) {
            db.impIntsDataQueries.insertOneImpInt(
                null,
                impIntIfText,
                impIntThenText,
                ownerId,
                ownerType
            )
            db.impIntsDataQueries.
            lastInsertRowId().executeAsOneOrNull()?: Constants.IGNORE_ID_AS_LONG
        }
    }

    suspend fun putAllImpInts(iis: ArrayList<MutableLiveData<ImpIntData>>) {
        return withContext(Dispatchers.IO) {
            db.impIntsDataQueries.transaction {
                for (ii in iis) ii.value?.run {
                    db.impIntsDataQueries.insertOneImpInt(
                        null,
                        this.impIntIfText,
                        this.impIntThenText,
                        this.ownerId,
                        this.ownerType
                    )
                }
            }
        }
    }


    override suspend fun markEditing(id:Long, isUnfinished: Boolean) {
        return withContext(Dispatchers.IO) {
            db.impIntsDataQueries.markImpIntEdit(isUnfinished, id)
        }
    }

    suspend fun putAsUnfinished(impInt: ImpIntData) {
        return withContext(Dispatchers.IO) {
            db.impIntsDataQueries.insertOneEditUnfinishedImpInt(
                impInt.impIntId,
                impInt.impIntIfText,
                impInt.impIntThenText,
                impInt.ownerId,
                impInt.ownerType
            )
        }
    }
}