package com.example.towardsgoalsapp.database.repositories

import androidx.lifecycle.MutableLiveData
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.database.HabitParameter
import com.example.towardsgoalsapp.database.HabitParameterValue
import com.example.towardsgoalsapp.database.TGDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

class HabitParamsRepository(private val db: TGDatabase) : OwnedByOneTypeOnlyOwnerUserData {

   suspend fun getParamCountOf(habitId: Long) : Long? {
        return withContext(Dispatchers.IO) {
            db.habitParametersQueries.getCountOfParamsOf(habitId).executeAsOneOrNull()
        }
    }

    suspend fun getParamValueCountOf(paramId: Long) : Long? {
        return withContext(Dispatchers.IO) {
            db.habitParametersQueries.getCountOfValuesOf(paramId).executeAsOneOrNull()
        }
    }

    override suspend fun getAllByOwnerId(ownerId: Long, allowUnfinished: Boolean): ArrayList<HabitParameter> {
        return withContext(Dispatchers.IO) {
            val f = db.habitParametersQueries.selectAllFinishedHabitParamsOf(ownerId).executeAsList()
                .toCollection(ArrayList())
            if (allowUnfinished) {
                val u = db.habitParametersQueries.selectAllUnfinishedHabitParamsOf(ownerId)
                    .executeAsList().toCollection(java.util.ArrayList())
                f.addAll(u.map { selectObject ->
                    HabitParameter(
                        selectObject.paramId,
                        true,
                        selectObject.habitId,
                        selectObject.name,
                        selectObject.targetVal,
                        selectObject.unit
                    )
                })
            }

            f.sortBy { it.paramId }
            f
        }
    }

    override suspend fun updateTexts(id:Long, firstText: String, secondText: String) {
        // not used
    }

    override suspend fun markEditing(id: Long, isUnfinished: Boolean) {
        // not used
    }

    suspend fun putValueOfParam(paramId: Long, value: Double, addedOn: Instant) {
        return withContext(Dispatchers.IO) {
            db.habitParametersQueries.putNewValueOfParameter(paramId, value, addedOn)
        }
    }

    suspend fun getAllValuesOfParam(paramId: Long) : ArrayList<HabitParameterValue> {
        return withContext(Dispatchers.IO) {
            db.habitParametersQueries.selectAllValuesOfParameter(paramId)
                .executeAsList().toCollection(ArrayList())
        }
    }

    override suspend fun getOneById(id: Long, allowUnfinished: Boolean): HabitParameter? {
        return withContext(Dispatchers.IO) {
            val unfinished = db.habitParametersQueries.getHabitParamUnfinished(id).executeAsOneOrNull()
                ?: return@withContext null
            if (unfinished) {
                if (!allowUnfinished) return@withContext null
                val uhp = db.habitParametersQueries.selectGivenUnfinishedHabitParam(id).executeAsOneOrNull()
                if (uhp == null) null else {
                    HabitParameter(
                        uhp.paramId,
                        true,
                        uhp.habitId,
                        uhp.name,
                        uhp.targetVal,
                        uhp.unit
                    )
                }
            }
            else {
                db.habitParametersQueries.selectGivenHabitParameter(id).executeAsOneOrNull()
            }
        }
    }

    override suspend fun deleteById(id: Long) {
        return withContext(Dispatchers.IO) {
            db.habitParametersQueries.deleteHabitParam(id)
        }
    }

    suspend fun deleteAllByIds(ids: Set<Long>) {
        for (id in ids) {
            deleteById(id)
        }
    }

    suspend fun addHabitParam( ownerId: Long, name: String, value: Double, unit: String?) : Long {
        return withContext(Dispatchers.IO) {
            db.habitParametersQueries.insertOneHabitParam(
                ownerId,
                name,
                unit,
                value
            )
            db.habitParametersQueries.
            lastInsertRowId().executeAsOneOrNull()?: Constants.IGNORE_ID_AS_LONG
        }
    }

    suspend fun updateHabitParamsBasedOn(hps: ArrayList<MutableLiveData<HabitParameter>>, asUnfinished: Boolean) {
        return withContext(Dispatchers.IO) {
            db.habitParametersQueries.transaction {
                for (hp in hps) hp.value?.run {

                    val isMarkedAsUnfinished =
                        db.habitParametersQueries.getHabitParamUnfinished(this.paramId)
                            .executeAsOneOrNull() ?: rollback()

                    val hasUnfinishedParam = db.habitParametersQueries
                        .selectGivenUnfinishedHabitParam(this.paramId).executeAsOneOrNull() != null

                    if (asUnfinished) {
                        if (! isMarkedAsUnfinished)
                            db.habitParametersQueries.markHabitParamEdit(true,
                                this.paramId)

                        if (hasUnfinishedParam) {
                            db.habitParametersQueries.updateUnfinishedHabitParam(
                                this.name,
                                this.unit,
                                this.targetVal,
                                this.paramId
                            )
                        }
                        else {
                            db.habitParametersQueries.insertOneEditUnfinishedHabitParam(
                                this.paramId,
                                this.name,
                                this.unit,
                                this.targetVal,
                            )
                        }
                    }
                    else {
                        if (hasUnfinishedParam) {
                            db.habitParametersQueries.markHabitParamEdit(false, this.paramId)
                            db.habitParametersQueries.deleteEditUnfinishedHabitParam(this.paramId)
                        }
                        db.habitParametersQueries.updateHabitParamTexts(
                            this.name,
                            this.unit,
                            this.paramId
                        )
                        db.habitParametersQueries
                            .updateHabitParamTargetValue(this.targetVal, this.paramId)
                    }
                }

            }
        }
    }


}