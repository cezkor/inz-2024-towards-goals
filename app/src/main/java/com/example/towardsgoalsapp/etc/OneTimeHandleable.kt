package com.example.towardsgoalsapp.etc

class OneTimeHandleable(private val func: () -> Unit)  {

    var handled = false
        private set

    fun handle() {
        if (! handled) {
            func()
            handled = true
        }
    }

}