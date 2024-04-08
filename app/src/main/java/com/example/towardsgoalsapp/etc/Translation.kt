package com.example.towardsgoalsapp.etc

import android.content.Context

abstract class Translation {

    abstract fun getString(resId: Int) : String

}

class AndroidContextTranslation(private val context : Context): Translation() {
    override fun getString(resId: Int): String = context.getString(resId)
}