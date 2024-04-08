package com.example.towardsgoalsapp.etc.errors

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.towardsgoalsapp.etc.OneTimeEvent
import com.example.towardsgoalsapp.etc.OneTimeEventWithValue
import kotlinx.coroutines.CoroutineExceptionHandler

open class ErrorHandlingViewModel : ViewModel() {

    val exceptionMutable: MutableLiveData<OneTimeEventWithValue<Throwable>> = MutableLiveData()

    val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        exceptionMutable.value = OneTimeEventWithValue(throwable)
    }

}