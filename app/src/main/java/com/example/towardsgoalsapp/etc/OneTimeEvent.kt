package com.example.towardsgoalsapp.etc

open class OneTimeEvent()  {

    var handled = false
        private set

    fun handleIfNotHandledWith(func : () -> Unit) {
        if (! handled) {
            func()
            handled = true
        }
    }

}

class OneTimeEventWithValue<T> (val value: T) : OneTimeEvent()