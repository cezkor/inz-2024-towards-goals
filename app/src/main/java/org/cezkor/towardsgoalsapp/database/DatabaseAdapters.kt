package org.cezkor.towardsgoalsapp.database

import app.cash.sqldelight.ColumnAdapter
import java.time.Instant

object TextAndInstantAdapter: ColumnAdapter<Instant, String> {
    override fun decode(databaseValue: String): Instant {
        return Instant.parse(databaseValue)
    }

    override fun encode(value: Instant): String {
        return value.toString()
    }

}