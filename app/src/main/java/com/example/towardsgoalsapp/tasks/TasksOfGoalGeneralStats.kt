package com.example.towardsgoalsapp.tasks

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R
import com.example.towardsgoalsapp.database.DatabaseObjectFactory
import com.example.towardsgoalsapp.database.MarkableTaskStatsData
import com.example.towardsgoalsapp.database.TGDatabase
import com.example.towardsgoalsapp.database.repositories.StatsDataRepository
import com.example.towardsgoalsapp.etc.AndroidContextTranslation
import com.example.towardsgoalsapp.etc.Translation
import com.example.towardsgoalsapp.etc.errors.ErrorHandling
import com.example.towardsgoalsapp.etc.errors.ErrorHandlingViewModel
import com.example.towardsgoalsapp.main.App
import com.example.towardsgoalsapp.stats.EpochFormatter
import com.example.towardsgoalsapp.stats.models.MarkableTasksGeneralStatsModelLogic
import com.example.towardsgoalsapp.stats.models.MarkableTasksGeneralStatsTextAndExtraData
import com.example.towardsgoalsapp.stats.models.ShowDataFrom
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class TasksOfGoalGeneralStatsViewModelFactory(private val dbo: TGDatabase,
                                 private val goalId: Long
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TasksOfGoalGeneralStatsViewModel(dbo, goalId) as T
    }
}

class TasksOfGoalGeneralStatsViewModel(private val dbo: TGDatabase,
                          private val goalId: Long
) : ErrorHandlingViewModel()
{
    private val statsRepo = StatsDataRepository(dbo)


    var model: MarkableTasksGeneralStatsModelLogic? = null

    val dataToShow = MutableLiveData<ArrayList<MarkableTaskStatsData>>()

    suspend fun getEverything() {
        dataToShow.value = statsRepo.getAllMarkableTaskStatsDataByGoal(goalId)
    }
}

class TasksOfGoalGeneralStatsFragment : Fragment() {

    inner class PriorityValueFormatter(val t: Translation): ValueFormatter() {
        private val shiftBackBy = 1f

        override fun getFormattedValue(value: Float): String {
            val y = value + shiftBackBy
            if (y == 0f) return Constants.EMPTY_STRING
            if (y <= 1f) return t.getString(R.string.tasks_priority_least)
            if (y <= 2f) return t.getString(R.string.tasks_priority_quite)
            if (y <= 3f) return t.getString(R.string.tasks_priority_significant)
            return t.getString(R.string.tasks_priority_most)
        }
    }

    companion object {
        const val LOG_TAG = "GTStatsFr"

        const val GOAL_ID = "gtstatgid"

        fun newInstance(goalId: Long) =
            TasksOfGoalGeneralStatsFragment().apply {
                arguments = Bundle().apply {
                    putLong(GOAL_ID, goalId)
                }
            }
    }

    private lateinit var viewModel: TasksOfGoalGeneralStatsViewModel

    private var goalId: Long = Constants.IGNORE_ID_AS_LONG


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_general_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            requireActivity()
        }
        catch (e : IllegalStateException) {
            Log.e(LOG_TAG,"no activity", e)
            return
        }

        arguments?.run {
            goalId = this.getLong(GOAL_ID, goalId)
            if (goalId == Constants.IGNORE_ID_AS_LONG) {
                Log.e(LOG_TAG,"no goal id")
                return
            }
        }

        val dbo = DatabaseObjectFactory
            .newDatabaseObject((requireActivity().application as App).driver)

        viewModel = ViewModelProvider(viewModelStore,
            TasksOfGoalGeneralStatsViewModelFactory(
                dbo,
                goalId
            ))[TasksOfGoalGeneralStatsViewModel::class.java]

        viewModel.exceptionMutable.observe(viewLifecycleOwner) {
            ErrorHandling.showExceptionDialog(requireActivity(), it)
        }
        
        // have to remake it in view model as it is context dependent 
        val translation = AndroidContextTranslation(requireContext())
        viewModel.model = MarkableTasksGeneralStatsModelLogic(translation)

        val titleTextView : TextView = view.findViewById(R.id.titleTextView)
        val chart: CombinedChart = view.findViewById(R.id.chart)
        val dPSpinner: Spinner = view.findViewById(R.id.dataPeriodSpinner)
        val extraTextView: TextView = view.findViewById(R.id.extraTV)

        val zoomOutButton = view.findViewById<Button>(R.id.zoomOutButton)
        val zoomInButton = view.findViewById<Button>(R.id.zoomInButton)

        zoomInButton.setOnClickListener { chart.zoomIn() }
        zoomOutButton.setOnClickListener { chart.zoomOut() }

        val wellColor = ContextCompat.getColor(requireContext(), R.color.progress_green)
        val notWellColor = ContextCompat.getColor(requireContext(), R.color.progress_red)
        val blue = ContextCompat.getColor(requireContext(), R.color.light_blue_400)
        val transparent = ContextCompat.getColor(requireContext(), R.color.transparent)

        chart.description.isEnabled = true
        chart.description.text = getString(R.string.stats_chart_is_scrollable)
        chart.setDrawGridBackground(false)
        chart.setDrawBarShadow(false)
        chart.isHighlightFullBarEnabled = false;

        // offset for showing labels
        chart.setExtraOffsets(20f, 10f, 0f, 0f)
        // user can zoom to 1 minute at least, 7 day at maximum
        chart.setVisibleXRangeMaximum((7 * Duration.ofDays(1).seconds).toFloat())
        chart.setVisibleXRangeMinimum(Duration.ofMinutes(1).seconds.toFloat())

        // "Today" bar has to be visible
        chart.drawOrder = arrayOf(CombinedChart.DrawOrder.BAR ,CombinedChart.DrawOrder.SCATTER)
        
        val chartLegend = chart.legend
        chartLegend.setDrawInside(false)
        chartLegend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        chartLegend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        chartLegend.orientation = Legend.LegendOrientation.HORIZONTAL

        val xAxis: XAxis = chart.xAxis
        val rAxis: YAxis = chart.axisRight
        val lAxis: YAxis = chart.axisLeft

        xAxis.labelRotationAngle = -90f
        // as I format labels for each day, no need to duplicate labels
        val valueFormatter = EpochFormatter()
        xAxis.valueFormatter = valueFormatter
        xAxis.setDrawLabels(true)

        rAxis.setDrawLabels(false)
        rAxis.setDrawZeroLine(false)
        rAxis.setDrawTopYLabelEntry(false)
        rAxis.gridColor = transparent
        lAxis.setDrawLabels(true)
        lAxis.setDrawZeroLine(false)
        lAxis.setDrawTopYLabelEntry(false)
        
        suspend fun recalcChartContent(withObject: Any) {
            if (withObject !is Boolean)
                viewModel.model!!.setData(withObject)
            val modelData = viewModel.model!!.calculateModelData()
            if (modelData.first.size != 1) return
            val dataSets = modelData.first.first()

            if (dataSets.first.isEmpty() && dataSets.second.isEmpty()) return

            // https://github.com/PhilJay/MPAndroidChart/issues/718#issuecomment-112823075
            val wData = dataSets.first.sortedWith { e1, e2 ->
                (e1.x - e2.x).toInt()
            }
            val wellDataSet = ScatterDataSet(
                wData,
                requireContext().getString(R.string.habits_marked_done)
            )
            wellDataSet.color = wellColor
            wellDataSet.setDrawValues(false)
            wellDataSet.setScatterShape(ScatterChart.ScatterShape.X)

            val nwData = dataSets.second.sortedWith { e1, e2 ->
                (e1.x - e2.x).toInt()
            }
            val notWellDataSet = ScatterDataSet(
                nwData,
                requireContext().getString(R.string.habits_marked_done_not_well)
            )
            notWellDataSet.color = notWellColor
            notWellDataSet.setDrawValues(false)
            notWellDataSet.setScatterShape(ScatterChart.ScatterShape.CROSS)

            val extraData = (modelData.second as MarkableTasksGeneralStatsTextAndExtraData)
            val beginAtEpochSec = Instant
                .ofEpochSecond(extraData.periodBeginning.epochSecond)
                .minus(1, ChronoUnit.DAYS)
                .epochSecond
            val endAtEpocSec = extraData.periodEnd.epochSecond
            val diff = endAtEpocSec - beginAtEpochSec
            xAxis.axisMinimum = 0f
            // to make "Today" bar visible, extend chart by 1 hour ahead of
            xAxis.axisMaximum = diff.toFloat() + Duration.ofHours(1).seconds.toFloat()
            rAxis.axisMaximum = extraData.maxTaskPriorityPlotValue.toFloat() + 0.5f
            rAxis.axisMinimum = 0f
            lAxis.axisMaximum = extraData.maxTaskPriorityPlotValue.toFloat() + 0.5f
            lAxis.axisMinimum = 0f
            valueFormatter.pushForwardBySeconds = beginAtEpochSec

            // "Today" bar
            val nowDS = BarDataSet(
                listOf(BarEntry(diff.toFloat(), extraData.maxTaskPriorityPlotValue.toFloat())
                ), getString(R.string.stats_today))
            nowDS.setDrawValues(false)
            nowDS.color = blue

            val scatterData = ScatterData(wellDataSet, notWellDataSet)
            val barData = BarData(nowDS)
            barData.barWidth = Duration.ofHours(1).seconds.toFloat()

            val combinedData = CombinedData()
            combinedData.setData(scatterData)
            combinedData.setData(barData)
            chart.data = combinedData

            extraTextView.text = "%s\n\n%s".format(
                requireContext().getString(R.string.tasks_stats_info),
                modelData.second.getString(translation)
            )

            // set ranges so that only the biggest priority (+ 0.5f for aesthetics purposes)
            // will be seen
            val r = extraData.maxTaskPriorityPlotValue.toFloat() + 0.5f
            chart.setVisibleYRange(r,r,lAxis.axisDependency)
            chart.setVisibleYRange(r,r,rAxis.axisDependency)
            // zoom out
            chart.zoom(0.1f, 0.1f, 0f, 0f)
            chart.moveViewToX(diff.toFloat()) // it also invalidates chart
        }
        
        titleTextView.text = translation.getString(R.string.tasks_history_of_markable_tasks)
        
        fun getOptionsArray() : Array<String> {
            return (viewModel.model!!.modelSettings[MarkableTasksGeneralStatsModelLogic.PERIOD]
                    as ShowDataFrom).choicesNames
        }
        val adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, getOptionsArray())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dPSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                try {
                    (viewModel.model!!.modelSettings[MarkableTasksGeneralStatsModelLogic.PERIOD]
                            as ShowDataFrom).setChoice(position)
                    lifecycleScope.launch(viewModel.exceptionHandler) { recalcChartContent(false) }
                }
                catch (e : IndexOutOfBoundsException) {
                    // ignore
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // ignore
            }
        }
        dPSpinner.adapter = adapter
        
        viewModel.dataToShow.observe(viewLifecycleOwner) {
            if (it.isNullOrEmpty()) return@observe
            lifecycleScope.launch(viewModel.exceptionHandler) { recalcChartContent(it) }
        }

        lifecycleScope.launch {
            viewModel.getEverything()
            // one week
            dPSpinner.setSelection(1)
        }

    }

}