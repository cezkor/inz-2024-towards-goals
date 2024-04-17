package org.cezkor.towardsgoalsapp.etc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


// this receiver can be created by activities that are not for directly marking task or habit
// for them to update UI if their data has been changed outside
// of activity result launcher they could launch

enum class RefreshTypes(val s: String) {

    TASK("task"), HABIT("habit"), GOAL("goal")
}

class ShouldRefreshUIBroadcastReceiver(
    val matchSet: HashSet<Pair<Long, RefreshTypes>>?,
    val onMatch: (() -> Unit)?
) : BroadcastReceiver() {

    companion object {
        const val ID_TO_MATCH = "srubcwrritm"
        const val TYPE_TO_MATCH = "srubcwrrttm"
        const val INTENT_FILTER = "shouldrefreshuibecausechangedoutside"

        fun createIntent(id: Long, r: RefreshTypes) : Intent {
            return Intent(INTENT_FILTER).apply {
                putExtra(ID_TO_MATCH, id)
                putExtra(TYPE_TO_MATCH, r.s)
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        // check if valid
        if (intent == null ) return
        if (intent.action != INTENT_FILTER) return
        val id = intent.getLongExtra(ID_TO_MATCH, org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG)
        val type = when (intent.getStringExtra(TYPE_TO_MATCH)) {
            RefreshTypes.TASK.s -> RefreshTypes.TASK
            RefreshTypes.HABIT.s -> RefreshTypes.HABIT
            RefreshTypes.GOAL.s -> RefreshTypes.GOAL
            else -> null
        }
        if (id == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG || type == null) return
        // match
        if (matchSet == null) onMatch?.invoke() // null -- match everything
        else {
            val p = Pair(id, type) // check if matched given
            if (p in matchSet) onMatch?.invoke()
        }
    }

}