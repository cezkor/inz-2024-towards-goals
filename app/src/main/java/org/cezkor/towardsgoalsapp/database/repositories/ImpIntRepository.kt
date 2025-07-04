package org.cezkor.towardsgoalsapp.database.repositories

import androidx.lifecycle.MutableLiveData
import org.cezkor.towardsgoalsapp.database.ImpIntData
import org.cezkor.towardsgoalsapp.database.TGDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImpIntRepository(private val db: TGDatabase) : OwnedByTypedOwnerUserData {
    override suspend fun getAllByOwnerTypeAndId(ownerType: org.cezkor.towardsgoalsapp.OwnerType,
                                                ownerId: Long,
                                                allowUnfinished: Boolean): ArrayList<ImpIntData> {
        return withContext(Dispatchers.IO) {
            val f = db.impIntsDataQueries.selectAllFinishedImpIntsOf(ownerId, ownerType).executeAsList()
                .toCollection(ArrayList())
            if (allowUnfinished) {
                val u = db.impIntsDataQueries.selectAllUnfinishedImpIntsOf(ownerId, ownerType)
                    .executeAsList().toCollection(java.util.ArrayList())
                f.addAll(u.map { selectObject ->
                    ImpIntData(
                        selectObject.impIntId,
                        selectObject.impIntEditUnfinished,
                        selectObject.impIntIfText,
                        selectObject.impIntThenText,
                        selectObject.ownerId,
                        selectObject.ownerType
                    )
                })
            }

            f.sortBy { it.impIntId }
            f
        }
    }

    override suspend fun updateTexts(id:Long, firstText: String, secondText: String) {
        return withContext(Dispatchers.IO) {
            db.impIntsDataQueries.updateImpIntTexts(firstText, secondText, id)
        }
    }

    override suspend fun getOneById(id: Long, allowUnfinished: Boolean): ImpIntData? {
        return withContext(Dispatchers.IO) {
            val unfinished = db.impIntsDataQueries.getImpIntUnfinished(id).executeAsOneOrNull()
                ?: return@withContext null
            if (unfinished) {
                if (!allowUnfinished) return@withContext null
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

    suspend fun deleteAllByIds(ids: Set<Long>) {
        for (id in ids) {
            deleteById(id)
        }
    }

    suspend fun addImpInt(impIntIfText: String, impIntThenText: String,
                          ownerType: org.cezkor.towardsgoalsapp.OwnerType, ownerId: Long) : Long {
        return withContext(Dispatchers.IO) {
            db.impIntsDataQueries.insertOneImpInt(
                null,
                impIntIfText,
                impIntThenText,
                ownerId,
                ownerType
            )
            db.impIntsDataQueries.
            lastInsertRowId().executeAsOneOrNull()?: org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG
        }
    }

    suspend fun updateImpIntsBasedOn(iis: ArrayList<MutableLiveData<ImpIntData>>, asUnfinished: Boolean) {
        return withContext(Dispatchers.IO) {
            db.impIntsDataQueries.transaction {
                for (ii in iis) ii.value?.run {
                    val isMarkedAsUnfinished =
                        db.impIntsDataQueries.getImpIntUnfinished(this.impIntId)
                            .executeAsOneOrNull() ?: rollback()

                    val isThereAnUnfinishedImpInt = db.impIntsDataQueries
                        .selectGivenUnfinishedImpInt(this.impIntId)
                        .executeAsOneOrNull() != null

                    if (asUnfinished) {

                        if (! isMarkedAsUnfinished)
                            db.impIntsDataQueries.markImpIntEdit(true, this.impIntId)
                        if (! isThereAnUnfinishedImpInt) {
                            db.impIntsDataQueries.insertOneEditUnfinishedImpInt(
                                this.impIntId,
                                this.impIntIfText,
                                this.impIntThenText,
                                this.ownerId,
                                this.ownerType
                            )
                        }
                        else {
                            db.impIntsDataQueries.updateUnfinishedImpIntTexts(
                                this.impIntIfText,
                                this.impIntThenText,
                                this.impIntId
                            )
                        }
                    }
                    else {
                        if (isThereAnUnfinishedImpInt) {
                            db.impIntsDataQueries.markImpIntEdit(false, this.impIntId)
                            db.impIntsDataQueries.deleteEditUnfinishedImpInt(this.impIntId)
                        }
                        db.impIntsDataQueries.updateImpIntTexts(
                            this.impIntIfText,
                            this.impIntThenText,
                            this.impIntId
                        )
                    }
                }
            }
        }
    }

    override suspend fun markEditing(id:Long, isUnfinished: Boolean) {
        // not used
    }

}