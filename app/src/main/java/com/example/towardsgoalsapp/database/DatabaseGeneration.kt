package com.example.towardsgoalsapp.database

import android.content.Context
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.OwnerType
import com.example.towardsgoalsapp.database.repositories.GoalRepository
import com.example.towardsgoalsapp.database.repositories.HabitParamsRepository
import com.example.towardsgoalsapp.database.repositories.HabitRepository
import com.example.towardsgoalsapp.database.repositories.ImpIntRepository
import com.example.towardsgoalsapp.database.repositories.ReminderRepository
import com.example.towardsgoalsapp.database.repositories.StatsDataRepository
import com.example.towardsgoalsapp.database.repositories.TaskRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.sin
import kotlin.random.Random

class DatabaseGeneration {
    companion object {

        const val LOG_TAG= "DataGen"

        private val putMutex = Mutex()

        fun getDriver(context: Context) : SqlDriver {
            val d = AndroidSqliteDriver(
                schema = TGDatabase.Schema,
                context = context,
                name = Constants.DATABASE_FILE_NAME,
                callback = object: AndroidSqliteDriver.Callback(TGDatabase.Schema) {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.setForeignKeyConstraintsEnabled(true)
                        super.onOpen(db)
                    }
                }
            )

            return d

        }

        suspend fun assureDatabaseHasTestData(db: TGDatabase) : Boolean {

            var testStatus = db.testDataPresentQueries
                .getTestStatus().executeAsOneOrNull()
            Log.i("DataGen", "test status: $testStatus")
            if (testStatus == null || testStatus == 0L) {
                db.testDataPresentQueries.setTestAsBeingGenerated()
                return fillDatabaseWithTestData(db)
            }
            else if (testStatus == 1L) {
                putMutex.withLock {
                    testStatus = db.testDataPresentQueries
                        .getTestStatus().executeAsOneOrNull()
                    return testStatus == 2L
                }
            }
            return false
        }

        private suspend fun fillDatabaseWithTestData(db: TGDatabase) : Boolean{
            val hRepo = HabitRepository(db)
            val tRepo = TaskRepository(db)
            val remRepo = ReminderRepository(db)
            val gRepo = GoalRepository(db)
            val statRepo = StatsDataRepository(db)
            val impIntRepo = ImpIntRepository(db)
            val hParRepo = HabitParamsRepository(db)

            val random = Random.Default

            var putData = false

            putMutex.withLock {

                try {

                    val g1 = gRepo.addOneGoal(
                        "Goal 1",
                        "Goal description of 1; it will contain in total" +
                                " 25 tasks and 4 habits",
                        1
                    )
                    val g2 = gRepo.addOneGoal(
                        "Goal 2",
                        "Goal description of 2; it will be empty",
                        4
                    )

                    val t1 = tRepo.addTask(
                        "task 1",
                        "directly doable task at goal depth; has 20 implementation intentions",
                        g1,
                        null,
                        0,
                        0
                    ).first
                    val t2 = tRepo.addTask(
                        "task 2",
                        "task at goal depth with subtasks; has 5 implementation intentions " +
                                "(imp ints for short)",
                        g1,
                        null,
                        0,
                        1
                    ).first
                    val t3 = tRepo.addTask(
                        "task 3",
                        "directly doable task at task depth 1; should be done well",
                        g1,
                        t2,
                        1,
                        3
                    ).first
                    val t4 = tRepo.addTask(
                        "task 4",
                        "task at task depth 1 with subtasks",
                        g1,
                        t2,
                        1,
                        3
                    ).first
                    val t5 = tRepo.addTask(
                        "task 5",
                        "directly doable task at depth 2; has reminder in 2025",
                        g1,
                        t4,
                        2,
                        2
                    ).first
                    val t6 = tRepo.addTask(
                        "task 6",
                        "directly doable task at depth 2; failed; priority 0",
                        g1,
                        t4,
                        2,
                        0
                    ).first
                    val t7 = tRepo.addTask(
                        "task 7",
                        "directly doable task at depth 2; failed; priority 1",
                        g1,
                        t4,
                        2,
                        1
                    ).first
                    val t8 = tRepo.addTask(
                        "task 8",
                        "directly doable task at depth 2; done well; priority 2",
                        g1,
                        t4,
                        2,
                        2
                    ).first

                    remRepo.addReminder(
                        LocalDateTime.of(2025, 10, 15, 15, 30)
                            .atZone(ZoneId.systemDefault()).toInstant(),
                        OwnerType.TYPE_TASK,
                        t5
                    )
                    for (i in 0..3) {
                        tRepo.addTask(
                            "1st task with priority $i",
                            "task at task depth 2; directly doable; priority ${1} " +
                                    "(${3 - i}th quadrant in eisenhower matrix}",
                            g1,
                            t4,
                            2,
                            i
                        )
                        tRepo.addTask(
                            "2nd task with priority $i",
                            "task at task depth 2; directly doable; priority ${1} " +
                                    "(${3 - i}th quadrant in eisenhower matrix}",
                            g1,
                            t4,
                            2,
                            i
                        )
                        tRepo.addTask(
                            "3rd task with priority $i",
                            "task at task depth 2; directly doable; priority ${1} " +
                                    "(${3 - i}th quadrant in eisenhower matrix}",
                            g1,
                            t4,
                            2,
                            i
                        )
                    }

                    val someArr: Array<Long> = Array(5) { -1 }
                    for (i in 1..5) {
                        val toFail = (i > 3)
                        someArr[i - 1] = tRepo.addTask(
                            "additional task $i for quadrant 3",
                            "task at task depth 2; directly doable; priority ${1}"
                                    + (if (toFail) "; will be failed" else "; will be done well"),
                            g1,
                            t4,
                            2,
                            0
                        ).first
                    }

                    var now = Instant.now()

                    tRepo.markTaskCompletion(t3, false)
                    statRepo.putNewMarkableTaskStatsData(
                        t3,
                        g1,
                        false,
                        now
                    )
                    tRepo.markTaskCompletion(t6, true)
                    statRepo.putNewMarkableTaskStatsData(
                        t6,
                        g1,
                        true,
                        now.minus(1, ChronoUnit.HOURS)
                    )
                    tRepo.markTaskCompletion(t7, true)
                    statRepo.putNewMarkableTaskStatsData(
                        t7,
                        g1,
                        true,
                        now.minus(2, ChronoUnit.HOURS)
                    )
                    tRepo.markTaskCompletion(t8, false)
                    statRepo.putNewMarkableTaskStatsData(
                        t8,
                        g1,
                        false,
                        now.minus(3, ChronoUnit.HOURS)
                    )

                    for (i in 1..5) {
                        val tid = someArr[i - 1]
                        if (i > 3) { // to fail
                            tRepo.markTaskCompletion(tid, true)
                            statRepo.putNewMarkableTaskStatsData(
                                tid,
                                g1,
                                true,
                                now.minus(10L * i, ChronoUnit.MINUTES)
                            )
                        } else {
                            tRepo.markTaskCompletion(tid, false)
                            statRepo.putNewMarkableTaskStatsData(
                                tid,
                                g1,
                                false,
                                now.minus(10L * i, ChronoUnit.MINUTES)
                            )
                        }
                    }

                    for (i in 1..5) {
                        impIntRepo.addImpInt(
                            "if $i",
                            "then $i",
                            OwnerType.TYPE_TASK,
                            t2
                        )
                    }
                    for (i in 1..3) {
                        impIntRepo.addImpInt(
                            "if $i",
                            "then $i",
                            OwnerType.TYPE_TASK,
                            t5
                        )
                    }

                    for (i in 1..20) {
                        impIntRepo.addImpInt(
                            "if $i",
                            "then $i",
                            OwnerType.TYPE_TASK,
                            t1
                        )
                    }

                    val h1 = hRepo.addHabit(
                        "habit 1",
                        "won't be touched, period of 14 days; a" +
                                "t least 10 days have to be marked well",
                        10,
                        Constants.periodsArray[2],
                        g1
                    )
                    val h2 = hRepo.addHabit(
                        "habit 2",
                        "done 50 times; no parameters; period of 28 days, " +
                                "all of them have to be marked well; not completed",
                        Constants.periodsArray[3],
                        Constants.periodsArray[3],
                        g1
                    )
                    val fourMonths = 4 * 28
                    val h3 = hRepo.addHabit(
                        "habit 3",
                        "done $fourMonths times; 3 parameters; period of 28 days, " +
                                "all of them have to be marked well; not completed; has 20 imp ints",
                        Constants.periodsArray[3],
                        Constants.periodsArray[3],
                        g1
                    )
                    val h4 = hRepo.addHabit(
                        "habit 4",
                        "done 32 times; 1 parameter; period of 28 days, " +
                                "all of them have to be marked well; completed; has 3 imp ints",
                        Constants.periodsArray[3],
                        Constants.periodsArray[3],
                        g1
                    )

                    for (i in 1..3) {
                        impIntRepo.addImpInt(
                            "if $i",
                            "then $i",
                            OwnerType.TYPE_HABIT,
                            h4
                        )
                    }
                    for (i in 1..20) {
                        impIntRepo.addImpInt(
                            "if $i",
                            "then $i",
                            OwnerType.TYPE_HABIT,
                            h3
                        )
                    }

                    val hPar1 = hParRepo.addHabitParam(
                        h4,
                        "linear",
                        5.0,
                        "coolu"
                    )
                    val hPar2 = hParRepo.addHabitParam(
                        h3,
                        "random",
                        10.0,
                        "km"
                    )
                    val hPar3 = hParRepo.addHabitParam(
                        h3,
                        "sin",
                        3.0,
                        null
                    )
                    val hPar4 = hParRepo.addHabitParam(
                        h3,
                        "WithMiss",
                        1.0,
                        "meters"
                    )

                    now = Instant.now()
                    now = now.minus(50, ChronoUnit.DAYS)
                    for (i in 0..<50) {
                        hRepo.markHabitDoneNotWell(h2, now)
                        statRepo.putNewHabitStatsData(
                            h2, g1,
                            false, true, now
                        )

                        val nowP1 = now.plus(1, ChronoUnit.HOURS)
                        if (i < 29) {
                            hRepo.markHabitDoneWell(h4, nowP1)
                            statRepo.putNewHabitStatsData(
                                h4, g1,
                                true, false, nowP1
                            )
                            hParRepo.putValueOfParam(
                                hPar1,
                                i / 10.0 + 0.1f,
                                nowP1
                            )
                        } else if (i < 31) {
                            hRepo.skipHabit(h4, nowP1)
                            statRepo.putNewHabitStatsData(
                                h4, g1,
                                false, false, nowP1
                            )
                        } else if (i == 31) {
                            hRepo.markHabitDoneNotWell(h4, nowP1)
                            statRepo.putNewHabitStatsData(
                                h4, g1,
                                false, true, nowP1
                            )
                            hParRepo.putValueOfParam(
                                hPar1,
                                i / 10.0 + 0.1f,
                                nowP1
                            )
                        }
                        now = now.plus(1, ChronoUnit.DAYS)
                    }

                    now = Instant.now()
                    now = now.minus(fourMonths.toLong(), ChronoUnit.DAYS)
                    for (i in 1..fourMonths) {
                        hRepo.markHabitDoneNotWell(h3, now)
                        statRepo.putNewHabitStatsData(
                            h3, g1,
                            false, true, now
                        )
                        hParRepo.putValueOfParam(
                            hPar2,
                            random.nextDouble(20.0),
                            now
                        )
                        hParRepo.putValueOfParam(
                            hPar3,
                            sin(i * Math.PI / 4.0),
                            now
                        )
                        hParRepo.putValueOfParam(
                            hPar4,
                            // 0.0 is default value; 12% chance for 1.0
                            if (random.nextDouble(1.0) < 0.12) 1.0 else 0.0,
                            now
                        )
                        now = now.plus(1, ChronoUnit.DAYS)
                    }

                    putData = true
                    db.testDataPresentQueries.setTestAsGenerated()
                    Log.i("DataGen", "test data was put")
                } catch (e: SQLiteException) {
                    if (putData) return true// assume we only broke onlyOne constraint
                    Log.e("DataGen", "exception", e)
                    db.testDataPresentQueries.setTestAsNotExistent()
                    return false
                }

                return true
            }
        }

        }
}
