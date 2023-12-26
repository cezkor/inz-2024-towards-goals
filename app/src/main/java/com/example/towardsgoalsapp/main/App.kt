package com.example.towardsgoalsapp.main

import android.app.Application
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.database.TGDatabase

class App: Application() {

    val driver: SqlDriver = AndroidSqliteDriver(
        schema = TGDatabase.Schema,
        context = this,
        name = Constants.DATABASE_FILE_NAME,
        callback = object: AndroidSqliteDriver.Callback(TGDatabase.Schema) {
            override fun onOpen(db: SupportSQLiteDatabase) {
                db.setForeignKeyConstraintsEnabled(true)
                super.onOpen(db)
            }
        }
    )

}