package org.cezkor.towardsgoalsapp.etc.errors

import android.app.Activity
import android.app.AlertDialog
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.etc.OneTimeEventWithValue

class ErrorHandling {
    companion object {

        const val LOG_TAG = "ErrHDL"

        fun showExceptionDialog(activity: Activity,
                                throwEvent: OneTimeEventWithValue<Throwable>,
                                onDialogClosed: (() -> Unit)? = null) {

            if (throwEvent.handled) return

            val throwable = throwEvent.value
            val inflater = activity.layoutInflater
            val builder: AlertDialog.Builder = AlertDialog.Builder(activity)

            Log.e(LOG_TAG,
                "showing throwable:",
                throwable
            )

            builder.setView(inflater.inflate(R.layout.dialog_fragment_exception_showing, null))

            builder.setOnDismissListener {
                onDialogClosed?.invoke()
            }
            builder.setPositiveButton(R.string.ok) { dialog, which ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.create()

            val stackTTextView = dialog.findViewById<TextView>(R.id.stackTraceTV)

            stackTTextView.text = throwable.stackTraceToString()

            dialog.show()
        }

        fun showThrowableAsToast(activity: Activity, throwable: Throwable) {
            Toast.makeText(activity, throwable.message, Toast.LENGTH_LONG).show()
        }

    }

}