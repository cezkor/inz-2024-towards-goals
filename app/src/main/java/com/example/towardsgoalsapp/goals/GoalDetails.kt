package com.example.towardsgoalsapp.goals

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R

class GoalViewModel: ViewModel() {

    val mutableGoalData = MutableLiveData<GoalData>()

}

class MainActivityRefreshIntentContract: ActivityResultContract<Long, Long>() {

    companion object {
        const val GOAL_ID_INTENT_FROM_MAIN = "gid1"
        const val GOAL_ID_INTENT_TO_MAIN = "gid2"
        const val LOG_TAG = "RefreshMain"
    }
    override fun createIntent(context: Context, input: Long): Intent {
        Log.i(LOG_TAG, "creating intent for $context, input $input")

        return Intent(context, GoalDetails::class.java).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(GOAL_ID_INTENT_FROM_MAIN, input.toString())
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Long {
        val data = intent?.getStringExtra(GOAL_ID_INTENT_TO_MAIN)
        Log.i(LOG_TAG, "got data $data, is OK: ${resultCode == Activity.RESULT_OK}")

        return if (resultCode == Activity.RESULT_OK && data != null)
            data.toLong()
        else
            Constants.IGNORE_ID_AS_LONG
    }


}

class GoalDetails : AppCompatActivity() {

    companion object{
        const val LOG_TAG = "GoalDetails"
    }

    private lateinit var viewModel: GoalViewModel

    private var goalId = Constants.IGNORE_ID_AS_LONG

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_details)

        val id = intent.getStringExtra(MainActivityRefreshIntentContract.GOAL_ID_INTENT_FROM_MAIN)?.toLong()
        id?.run{ goalId = id }


        viewModel = GoalViewModel()
        // viewModel.mutableGoalData.value == get data from database

        title = viewModel.mutableGoalData.value?.goalName ?: R.string.default_title.toString()

    }

    override fun onStop() {
        val result = if (goalId == Constants.IGNORE_ID_AS_LONG) RESULT_CANCELED else RESULT_OK
        setResult(result, Intent().apply {
            putExtra(MainActivityRefreshIntentContract.GOAL_ID_INTENT_TO_MAIN, goalId)
        })
        super.onStop()
    }
    
}