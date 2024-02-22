package com.example.towardsgoalsapp.etc

import java.util.concurrent.atomic.AtomicBoolean

// denies and ends work that is being requested while there is a work being done right now

abstract class WorkDenier<T>(private val onDenyReturn: T) {

    private val denyBoolean: AtomicBoolean = AtomicBoolean(false)
    suspend fun doWork(work: suspend () -> (T)): T {

        return if (denyBoolean.get()) {
            onDenyReturn
        } else {
            denyBoolean.set(true)
            val result: T = work()
            denyBoolean.set(false)
            result
        }
    }
}

class BooleanWorkDenier : WorkDenier<Boolean>(false)