package com.example.towardsgoalsapp.etc.errors

import android.content.Context
import android.widget.Toast
import com.example.towardsgoalsapp.R

class SaveSuccessToastLauncher private constructor() {
    companion object {
        fun launchToast(context: Context, success: Boolean) {
            val text = if (success) context.getText(R.string.edit_save_did_not_fail)
                else context.getText(R.string.edit_save_did_fail)
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

}