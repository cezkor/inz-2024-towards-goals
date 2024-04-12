package org.cezkor.towardsgoalsapp.habits

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
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.database.DatabaseObjectFactory
import org.cezkor.towardsgoalsapp.database.HabitData
import org.cezkor.towardsgoalsapp.database.HabitStatsData
import org.cezkor.towardsgoalsapp.database.TGDatabase
import org.cezkor.towardsgoalsapp.database.repositories.HabitRepository
import org.cezkor.towardsgoalsapp.database.repositories.StatsDataRepository
import org.cezkor.towardsgoalsapp.etc.AndroidContextTranslation
import org.cezkor.towardsgoalsapp.etc.errors.ErrorHandling
import org.cezkor.towardsgoalsapp.etc.errors.ErrorHandlingViewModel
import org.cezkor.towardsgoalsapp.main.App
import org.cezkor.towardsgoalsapp.stats.EpochFormatter
import org.cezkor.towardsgoalsapp.stats.models.HabitGeneralStatsModelLogic
import org.cezkor.towardsgoalsapp.stats.models.HabitGeneralStatsTextAndExtraData
import org.cezkor.towardsgoalsapp.stats.models.ShowDataFrom
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CombinedData
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class HabitGeneralStatsViewModelFactory(private val dbo: TGDatabase,
                                 private val habitId: Long
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HabitGeneralStatsViewModel(dbo, habitId) as T
    }
}

class HabitGeneralStatsViewModel(private val dbo: TGDatabase,
                          private val habitId: Long
) : ErrorHandlingViewModel()
{
    private val statsRepo = StatsDataRepository(dbo)
    private val habitRepo = HabitRepository(dbo)

    var model: HabitGeneralStatsModelLogic? = null

    val dataToShow = MutableLiveData<ArrayList<HabitStatsData>>()

    var habitData: HabitData? = null
        private set
    suspend fun getEverything() {
        habitData = habitRepo.getOneById(habitId)
        dataToShow.value = statsRepo.getAllHabitStatsDataByHabit(habitId)
    }
}

class HabitGeneralStatsFragment : Fragment() {

    companion object {
        const val LOG_TAG = "HGSFr"

        const val HABIT_ID = "hgshid"

        private val halfADay : Float = Duration.ofDays(1).seconds/2f

        fun newInstance(habitId: Long) =
            HabitGeneralStatsFragment().apply {
                arguments = Bundle().apply {
                    putLong(HABIT_ID, habitId)
                }
            }
    }

    private lateinit var viewModel: HabitGeneralStatsViewModel

    private var habitId: Long = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG


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
            habitId = this.getLong(HABIT_ID, habitId)
            if (habitId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) {
                Log.e(LOG_TAG,"no habit id")
                return
            }
        }

        val dbo = DatabaseObjectFactory
            .newDatabaseObject((requireActivity().application as App).driver)

        viewModel = ViewModelProvider(viewModelStore,
            HabitGeneralStatsViewModelFactory(
                dbo,
                habitId
            ))[HabitGeneralStatsViewModel::class.java]

        viewModel.exceptionMutable.observe(viewLifecycleOwner) {
            ErrorHandling.showExceptionDialog(requireActivity(), it)
        }
        
        // have to remake it in view model as it is context dependent 
        val translation = AndroidContextTranslation(requireContext())
        viewModel.model = HabitGeneralStatsModelLogic(translation)

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
        chart.setDrawBorders(false)
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.isHighlightFullBarEnabled = false;

        // offset for showing labels
        chart.setExtraOffsets(0f, 10f, 0f, 0f)
        // user can zoom to 1 day at least, 7 day at maximum
        chart.setVisibleXRangeMaximum((7 * Duration.ofDays(1).seconds).toFloat())
        chart.setVisibleXRangeMinimum(Duration.ofDays(1).seconds.toFloat())

        chart.drawOrder = arrayOf(CombinedChart.DrawOrder.BAR)
        
        val chartLegend = chart.legend
        chartLegend.setDrawInside(false)
        chartLegend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        chartLegend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        chartLegend.orientation = Legend.LegendOrientation.HORIZONTAL

        val xAxis: XAxis = chart.xAxis
        val rAxis: YAxis = chart.axisRight
        val lAxis: YAxis = chart.axisLeft

        // no need to be able to zoom in further than a day - as habit may not be marked more than
        // once a day
        xAxis.granularity = Duration.ofDays(1).seconds.toFloat()
        xAxis.labelRotationAngle = -90f
        xAxis.setAvoidFirstLastClipping(false)
        val valueFormatter = EpochFormatter()
        xAxis.valueFormatter = valueFormatter
        xAxis.setDrawLabels(true)

        // set ranges and zoom so bars are seen on whole y range
        chart.setVisibleYRange(1f, 1f, lAxis.axisDependency)
        chart.setVisibleYRange(1f, 1f, rAxis.axisDependency)
        
        // this will only show bars, no need to show that their values are all 1.0f
        rAxis.setDrawLabels(false)
        rAxis.setDrawZeroLine(false)
        rAxis.setDrawTopYLabelEntry(false)
        rAxis.gridColor = transparent
        lAxis.setDrawLabels(false)
        lAxis.setDrawZeroLine(false)
        lAxis.setDrawTopYLabelEntry(false)
        lAxis.gridColor = transparent
        
        suspend fun recalcChartContent(withObject: Any) {
            if (withObject !is Boolean) // if it is a boolean, then it means it is not data
                viewModel.model!!.setData(withObject)
            val modelData = viewModel.model!!.calculateModelData()
            if (modelData.first.size != 1) return
            val dataSets = modelData.first.first()

            if (dataSets.first.isEmpty() && dataSets.second.isEmpty()) return

            // https://github.com/PhilJay/MPAndroidChart/issues/718#issuecomment-112823075
            val wData = dataSets.first.map { e -> BarEntry(e.x, e.y) }.sortedWith { e1, e2 ->
                (e1.x - e2.x).toInt()
            }
            val wellDataSet = BarDataSet(
                wData,
                requireContext().getString(R.string.habits_marked_done)
            )
            wellDataSet.color = wellColor
            wellDataSet.setDrawValues(false)

            val nwData = dataSets.second.map { e -> BarEntry(e.x, e.y) }.sortedWith { e1, e2 ->
                (e1.x - e2.x).toInt()
            }
            val notWellDataSet = BarDataSet(
                nwData,
                requireContext().getString(R.string.habits_marked_done_not_well)
            )
            notWellDataSet.color = notWellColor
            notWellDataSet.setDrawValues(false)

            val extraData = (modelData.second as HabitGeneralStatsTextAndExtraData)
            // moving beginning by 2 days back so the oldest data can have shown labels
            val beginAtEpochSec = Instant
                                    .ofEpochSecond(extraData.periodBeginning.epochSecond)
                                    .minus(2, ChronoUnit.DAYS)
                                    .epochSecond
            val endAtEpocSec = extraData.periodEnd.epochSecond

            val diff = endAtEpocSec - beginAtEpochSec // so to keep range between [0, maximal seconds]
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = diff.toFloat() + Duration.ofHours(1).seconds // to show today
            rAxis.axisMaximum = 1f
            rAxis.axisMinimum = 0f
            lAxis.axisMaximum = 1f
            lAxis.axisMinimum = 0f
            valueFormatter.pushForwardBySeconds = beginAtEpochSec

            val nowDS = BarDataSet(
                listOf(BarEntry(diff.toFloat(), 1f)
                ), getString(R.string.stats_today))
            nowDS.setDrawValues(false)
            nowDS.color = blue

            val barData = BarData(wellDataSet, notWellDataSet, nowDS)
            barData.barWidth = Duration.ofDays(1).seconds/24f // one hour

            val combinedData = CombinedData()
            combinedData.setData(barData)
            chart.data = combinedData

            val t1 = viewModel.habitData?.habitTargetPeriod?.toString()
                ?: org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
            val t2 = viewModel.habitData?.habitTargetCount?.toString()
                ?: org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING

            extraTextView.text = "%s\n\n%s".format(
                requireContext().getString(R.string.habits_evaluation_info,
                    t1, t2
                ),
                modelData.second.getString(translation)
            )

            // zoom out
            chart.zoom(0.1f, 0.1f, 0f, 0f)
            chart.moveViewToX(diff.toFloat()) // it also invalidates chart
        }
        
        titleTextView.text = translation.getString(R.string.habits_marking_history)
        
        fun getOptionsArray() : Array<String> {
            return (viewModel.model!!.modelSettings[HabitGeneralStatsModelLogic.PERIOD] 
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
                    (viewModel.model!!.modelSettings[HabitGeneralStatsModelLogic.PERIOD]
                            as ShowDataFrom).setChoice(position)
                    lifecycleScope.launch(viewModel.exceptionHandler)
                    { recalcChartContent(false) }
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