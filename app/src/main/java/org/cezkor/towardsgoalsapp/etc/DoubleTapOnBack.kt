package org.cezkor.towardsgoalsapp.etc

import android.content.Context
import android.widget.Toast
import androidx.activity.OnBackPressedCallback

class DoubleTapOnBack(
    private val context: Context,
    private val onBackString: String,
    private val onBackRunnable: Runnable
) : OnBackPressedCallback(true){

    private var reallyLeave = false
    override fun handleOnBackPressed() {
        if (! isEnabled) return
        if (! reallyLeave) {
            Toast.
            makeText(
                context,
                onBackString,
                Toast.LENGTH_SHORT
            ).show()
            reallyLeave = true
        }
        else {
            isEnabled = false
            onBackRunnable.run()
        }
    }
}

