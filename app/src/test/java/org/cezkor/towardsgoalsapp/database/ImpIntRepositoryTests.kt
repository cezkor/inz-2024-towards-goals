package org.cezkor.towardsgoalsapp.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.cezkor.towardsgoalsapp.database.ImpIntData
import org.cezkor.towardsgoalsapp.database.TGDatabase
import org.cezkor.towardsgoalsapp.database.repositories.ImpIntRepository
import com.google.common.truth.Truth.*
import kotlinx.coroutines.runBlocking
import org.cezkor.towardsgoalsapp.Constants
import org.cezkor.towardsgoalsapp.OwnerType
import org.junit.Rule
import org.junit.Test

class ImpIntRepositoryTests {

    @Rule
    @JvmField
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).apply {
        TGDatabase.Schema.create(this)
        this.execute(null, "PRAGMA foreign_keys = ON;", 0)
    }

    private val db: TGDatabase = DatabaseObjectFactory.newDatabaseObject(driver)

    private val repo: ImpIntRepository = ImpIntRepository(db)

    private val arrayOfImpInts: ArrayList<ImpIntData>
        = java.util.ArrayList()

    @Test
    fun `basic database interaction`() {
        runBlocking {
            arrayOfImpInts.clear()

            val impId1 = repo.addImpInt(
                "if 1",
                "then 1",
                org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK,
                100
            )
            var iiList =  repo.getAllByOwnerTypeAndId(org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK, 100)
            assertThat(iiList.size).isEqualTo(1)
            assertThat(iiList[0].impIntId).isEqualTo(impId1)
            assertThat(iiList[0].impIntIfText).isEqualTo("if 1")
            assertThat(iiList[0].impIntThenText).isEqualTo("then 1")
            assertThat(iiList[0].ownerId).isEqualTo(100)
            assertThat(iiList[0].ownerType).isEqualTo(org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK)

            repo.updateTexts(
                impId1, "super text 1", "super descr 1"
            )
            val impOrNull = repo.getOneById(impId1)
            assertThat(impOrNull).isNotNull()
            iiList[0]=impOrNull!!
            assertThat(iiList[0].impIntId).isEqualTo(impId1)
            assertThat(iiList[0].impIntIfText).isEqualTo("super text 1")
            assertThat(iiList[0].impIntThenText).isEqualTo("super descr 1")
            assertThat(iiList[0].ownerId).isEqualTo(100)
            assertThat(iiList[0].ownerType).isEqualTo(org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK)

            repo.deleteById(impId1)
            iiList = repo.getAllByOwnerTypeAndId(org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK, 100)
            assertThat(iiList.size).isEqualTo(0)

            val impId2 = repo.addImpInt(
                "if 2",
                "then 2",
                org.cezkor.towardsgoalsapp.OwnerType.TYPE_HABIT,
                100
            )
            val imp2OrNull = repo.getOneById(impId2)
            assertThat(imp2OrNull).isNotNull()
            iiList = repo.getAllByOwnerTypeAndId(org.cezkor.towardsgoalsapp.OwnerType.TYPE_HABIT, 100)
            assertThat(iiList.size).isEqualTo(1)
            assertThat(iiList[0].impIntId).isEqualTo(impId2)
            assertThat(iiList[0].impIntIfText).isEqualTo("if 2")
            assertThat(iiList[0].impIntThenText).isEqualTo("then 2")
            assertThat(iiList[0].ownerId).isEqualTo(100)
            assertThat(iiList[0].ownerType).isEqualTo(org.cezkor.towardsgoalsapp.OwnerType.TYPE_HABIT)


            val impId3 = repo.addImpInt(
                "if 3",
                "then 3",
                org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK,
                100
            )
            val goal3OrNull = repo.getOneById(impId3)
            assertThat(goal3OrNull).isNotNull()

            arrayOfImpInts.addAll(
                arrayListOf<ImpIntData>(
                    ImpIntData(
                        Constants.IGNORE_ID_AS_LONG,
                        false,
                        "if 14",
                        "then 14",
                        100,
                        OwnerType.TYPE_HABIT
                    ),
                    ImpIntData(
                        Constants.IGNORE_ID_AS_LONG,
                        false,
                        "if 10",
                        "then 10",
                        100,
                        OwnerType.TYPE_TASK
                    ),
                    ImpIntData(
                        Constants.IGNORE_ID_AS_LONG,
                        false,
                        "if 11",
                        "then 11",
                        100,
                        OwnerType.TYPE_TASK
                    ),
                    ImpIntData(
                        Constants.IGNORE_ID_AS_LONG,
                        false,
                        "if 12",
                        "then 12",
                        100,
                        OwnerType.TYPE_HABIT
                    )
                )
            )
            arrayOfImpInts.map {
                repo.addImpInt(
                    it.impIntIfText,
                    it.impIntThenText,
                    it.ownerType,
                    it.ownerId
                )
            }

            iiList = repo.getAllByOwnerTypeAndId(org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK, 100)
            assertThat(iiList.size).isEqualTo(3)
            assertThat(iiList.map { it.impIntIfText }).containsExactly(
                "if 3", "if 10", "if 11"
            )
            assertThat(iiList.map { it.impIntThenText }).containsExactly(
                "then 3", "then 10", "then 11"
            )

            iiList = repo.getAllByOwnerTypeAndId(org.cezkor.towardsgoalsapp.OwnerType.TYPE_HABIT, 100)
            assertThat(iiList.size).isEqualTo(3)
            assertThat(iiList.map { it.impIntIfText }).containsExactly(
                "if 2", "if 12", "if 14"
            )
            assertThat(iiList.map { it.impIntThenText }).containsExactly(
                "then 2", "then 12", "then 14"
            )

            repo.deleteById(impId2)
            repo.deleteById(impId3)
            iiList = (repo.getAllByOwnerTypeAndId(org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK, 100) +
                    repo.getAllByOwnerTypeAndId(org.cezkor.towardsgoalsapp.OwnerType.TYPE_HABIT, 100))
                    as ArrayList<ImpIntData>
            for (el in iiList) {
                repo.deleteById(el.impIntId)
            }
            iiList = repo.getAllByOwnerTypeAndId(org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK, 100)
            assertThat(iiList.size).isEqualTo(0)
            iiList = repo.getAllByOwnerTypeAndId(org.cezkor.towardsgoalsapp.OwnerType.TYPE_HABIT, 100)
            assertThat(iiList.size).isEqualTo(0)
            iiList = repo.getAllByOwnerTypeAndId(org.cezkor.towardsgoalsapp.OwnerType.TYPE_NONE, 100)
            assertThat(iiList.size).isEqualTo(0)
        }

    }

    @Test
    fun `unfinished data handled properly`() { runBlocking {

        var iiList: ArrayList<ImpIntData>
            = repo.getAllByOwnerTypeAndId(org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK, 100)
        assertThat(iiList.size).isEqualTo(0)

        val impId1 = repo.addImpInt(
            "if 1",
            "then 1",
            org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK,
            100
        )
        val impId2 = repo.addImpInt(
            "if 2",
            "then 2",
            org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK,
            100
        )
        iiList = repo.getAllByOwnerTypeAndId(org.cezkor.towardsgoalsapp.OwnerType.TYPE_TASK, 100)
        assertThat(iiList.map { it.impIntEditUnfinished }).containsExactly(false, false)
        assertThat(iiList[0].impIntId).isEqualTo(impId1)

        val editedII = ImpIntData(
            impId1,
            false, // should be ignored
            "edited if 1",
            "edited then 1",
            100,
            OwnerType.TYPE_TASK
        )
        val ar1: ArrayList<MutableLiveData<ImpIntData>> = arrayListOf(MutableLiveData(editedII))
        repo.updateImpIntsBasedOn(ar1, true)
        val hopefullyUnfinishedImpInt = repo.getOneById(impId1)
        assertThat(hopefullyUnfinishedImpInt).isNotNull()
        assertThat(hopefullyUnfinishedImpInt!!).isInstanceOf(ImpIntData::class.java)
        assertThat(hopefullyUnfinishedImpInt.impIntEditUnfinished).isEqualTo(true)
        assertThat(hopefullyUnfinishedImpInt.impIntIfText).isEqualTo("edited if 1")
        assertThat(hopefullyUnfinishedImpInt.impIntThenText).isEqualTo("edited then 1")
        assertThat(hopefullyUnfinishedImpInt.impIntId).isNotEqualTo(impId2)
        assertThat(hopefullyUnfinishedImpInt.impIntId).isEqualTo(impId1)

        ar1[0].value = ImpIntData (
            impId1,
            true, // should be ignored
            "new edited if 1",
            "new edited then 1",
            100,
            OwnerType.TYPE_TASK
        )

        repo.updateImpIntsBasedOn(ar1, false)

        assertThat(
            db.impIntsDataQueries.selectGivenUnfinishedImpInt(impId1).executeAsOneOrNull()
        ).isNull()

        val hopefullyFinishedImpInt = repo.getOneById(impId1)
        assertThat(hopefullyFinishedImpInt).isNotNull()
        assertThat(hopefullyFinishedImpInt!!).isInstanceOf(ImpIntData::class.java)
        assertThat(hopefullyFinishedImpInt.impIntEditUnfinished).isEqualTo(false)
        assertThat(hopefullyFinishedImpInt.impIntIfText).isEqualTo("new edited if 1")
        assertThat(hopefullyFinishedImpInt.impIntThenText).isEqualTo("new edited then 1")

        repo.deleteById(impId2)
        repo.deleteById(impId1)
        assertThat(
            db.impIntsDataQueries.selectGivenUnfinishedImpInt(impId1).executeAsOneOrNull()
        ).isNull()
    } }

}