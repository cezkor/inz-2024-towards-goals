package org.cezkor.towardsgoalsapp.habits.params

import android.app.AlertDialog
import android.os.Bundle
import android.os.LocaleList
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
import androidx.recyclerview.widget.RecyclerView
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.database.DatabaseObjectFactory
import org.cezkor.towardsgoalsapp.database.HabitParameter
import org.cezkor.towardsgoalsapp.database.HabitParameterValue
import org.cezkor.towardsgoalsapp.database.TGDatabase
import org.cezkor.towardsgoalsapp.database.repositories.HabitParamsRepository
import org.cezkor.towardsgoalsapp.database.repositories.HabitRepository
import org.cezkor.towardsgoalsapp.etc.AndroidContextTranslation
import org.cezkor.towardsgoalsapp.etc.OneTimeEvent
import org.cezkor.towardsgoalsapp.etc.errors.ErrorHandling
import org.cezkor.towardsgoalsapp.etc.errors.ErrorHandlingViewModel
import org.cezkor.towardsgoalsapp.main.App
import org.cezkor.towardsgoalsapp.stats.DaysValueFormatter
import org.cezkor.towardsgoalsapp.stats.ModelSettingsAdapter
import org.cezkor.towardsgoalsapp.stats.NonScrollableLayoutManager
import org.cezkor.towardsgoalsapp.stats.models.HabitModelEnum
import org.cezkor.towardsgoalsapp.stats.models.HabitModelFactory.Companion.createModelFromEnum
import org.cezkor.towardsgoalsapp.stats.models.ModelLogic
import org.cezkor.towardsgoalsapp.stats.models.ModelLogicWithPrediction
import org.cezkor.towardsgoalsapp.stats.models.PredictionExtras
import org.cezkor.towardsgoalsapp.stats.models.PredictionExtrasWithConfidenceIntervals
import org.cezkor.towardsgoalsapp.stats.models.PredictionType
import org.cezkor.towardsgoalsapp.stats.models.WithExtraData
import org.cezkor.towardsgoalsapp.stats.UnitFormatter
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.cezkor.towardsgoalsapp.habits.HabitLogic
import org.cezkor.towardsgoalsapp.stats.models.WithExtraNamedData
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow


class HabitParamsStatsViewModelFactory(private val dbo: TGDatabase,
                            private val habitId: Long
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HabitParamsStatsViewModel(dbo, habitId) as T
    }
}

class HabitParamsStatsViewModel(private val dbo: TGDatabase, private val habitId: Long) :
    ErrorHandlingViewModel() {

    companion object {
        const val LOG_TAG = "HPSViewM"
    }

    private val habitParamsRepo = HabitParamsRepository(dbo)
    private val habitRepo = HabitRepository(dbo)

    val modelMutable: MutableLiveData<ModelLogic<*>?> = MutableLiveData(null)
    val modelSettingChanged: MutableLiveData<OneTimeEvent> = MutableLiveData()
    var currentParamId: Long = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG
    var currentParamObj: HabitParameter? = null

    var currentDayNumber: Long = 0
        private set

    var shiftPredictionLabel: Boolean = false
        private set

    val paramArrayMutable: MutableLiveData<ArrayList<HabitParameter>> = MutableLiveData()
    val currentParamDataMutable: MutableLiveData<ArrayList<HabitParameterValue>> = MutableLiveData()

    suspend fun getAndReadyEverything() {
        val params = habitParamsRepo.getAllByOwnerId(habitId)
        val toRemove = HashSet<HabitParameter>()
        // filter out parameters which don't have any values
        for (hp in params) {
            val paramValuesCount = habitParamsRepo.getParamValueCountOf(hp.paramId)
            if (paramValuesCount == null || paramValuesCount < org.cezkor.towardsgoalsapp.Constants.MINIMUM_SAMPLE)
                toRemove.add(hp)
        }
        params.removeAll(toRemove)
        if (params.isEmpty()) {
            Log.e(LOG_TAG, "habit has no showable parameters yet fragment for this VM is visible; returning")
            return
        }

        val habit = habitRepo.getOneById(habitId)
        habit?.run {
            val now = LocalDateTime.now()
            val lastMarked = LocalDateTime.ofInstant(this.habitLastMarkedOn, ZoneId.systemDefault())
            val lastHDC = this.habitMarkCount
            val dur = Duration.between(lastMarked, now).toDays()
            val daysDiff = if (dur > 0) dur else 0
            currentDayNumber = lastHDC + daysDiff
            shiftPredictionLabel = ! HabitLogic.checkIfHabitIsMarkable(this.habitLastMarkedOn)
        }

        currentParamId = params[0].paramId
        paramArrayMutable.value = params
        fetchValuesOfCurrentParam()
    }

    suspend fun fetchValuesOfCurrentParam() {
        val values = habitParamsRepo.getAllValuesOfParam(currentParamId)
        if (values.isEmpty()) return

        currentParamDataMutable.value = values
    }
}

class HabitParamsStatsFragment : Fragment() {

    companion object {

        const val LOG_TAG = "HPSFr"

        const val HABIT_ID = "hpsid"

        fun newInstance(habitId: Long) =
            HabitParamsStatsFragment().apply {
                arguments = Bundle().apply {
                    putLong(HABIT_ID, habitId)
                }
            }
    }

    private lateinit var viewModel: HabitParamsStatsViewModel

    private var habitId: Long = org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG

    private val chartMutex = Mutex()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_habit_param_stats, container, false)
    }

    inner class HabitModelName (
        val enumValue: HabitModelEnum,
        val name: String
    ) {
        override fun toString(): String {
            return name
        }
    }

    private fun showHelpDialog(helpText: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(getString(R.string.help))
        builder.setPositiveButton(R.string.ok) { dialog, which ->
            dialog.dismiss()
        }
        builder.setMessage(helpText)
        builder.create().show()
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
        }
        if (habitId == org.cezkor.towardsgoalsapp.Constants.IGNORE_ID_AS_LONG) {
            Log.e(LOG_TAG, "no habit id")
            return
        }

        val chart: CombinedChart = view.findViewById(R.id.paramsChart)
        val paramsSpinner: Spinner = view.findViewById(R.id.parameterSpinner)
        val modelSpinner: Spinner = view.findViewById(R.id.modelSpinner)
        val modelDetailsTextView: TextView = view.findViewById(R.id.paramsStatsDetails)
        val modelSettingsRecyclerView: RecyclerView = view.findViewById(R.id.modelSettingsRecyclerView)
        val showExtraDataButton: Button = view.findViewById(R.id.showExtraButton)

        val zoomOutButton = view.findViewById<Button>(R.id.zoomOutButton)
        val zoomInButton = view.findViewById<Button>(R.id.zoomInButton)

        zoomInButton.setOnClickListener { chart.zoomIn() }
        zoomOutButton.setOnClickListener { chart.zoomOut() }

        val blueColor = ContextCompat.getColor(requireContext(), R.color.light_blue_600)
        val yellowColor = ContextCompat.getColor(requireContext(), R.color.prediction_yellow)
        val redColor = ContextCompat.getColor(requireContext(), R.color.progress_red)
        val greenColor = ContextCompat.getColor(requireContext(), R.color.progress_green)
        val purpleColor = ContextCompat.getColor(requireContext(), R.color.neon_purple)

        val translation = AndroidContextTranslation(requireContext())

        val modelArray: Array<HabitModelName> = arrayOf(
            HabitModelName(
                HabitModelEnum.LINEAR_REGRESSION,
                getString(R.string.stats_linear_reg_name)
            ),
            HabitModelName(
                HabitModelEnum.ARIMA,
                getString(R.string.stats_arima_name)
            )
        )

        fun prepareViewModel() {
            val driver = (requireActivity().application as App).driver

            viewModel = ViewModelProvider(viewModelStore,
                HabitParamsStatsViewModelFactory(
                    DatabaseObjectFactory.newDatabaseObject(driver), habitId
                )
            )[HabitParamsStatsViewModel::class.java]
        }

        fun prepareUI() {

            showExtraDataButton.isEnabled = false

            viewModel.exceptionMutable.observe(viewLifecycleOwner) {
                ErrorHandling.showExceptionDialog(requireActivity(), it)
            }

            // whole month has to be able to be seen
            chart.setVisibleXRangeMaximum(28f)
            chart.setVisibleXRangeMinimum(1f)
            // so user is able to see labels on top and on the left
            chart.setExtraOffsets(5f, 15f, 0f, 0f )

            chart.description.isEnabled = true
            chart.description.text =
                getString(R.string.stats_chart_is_scrollable)
            chart.setDrawGridBackground(true);
            chart.setDrawBarShadow(false);
            chart.isHighlightFullBarEnabled = false;

            val chartLegend = chart.legend
            chartLegend.setDrawInside(false)
            chartLegend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            chartLegend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            chartLegend.orientation = Legend.LegendOrientation.HORIZONTAL
            chartLegend.isWordWrapEnabled = true

            val xAxis: XAxis = chart.xAxis
            val lAxis: YAxis = chart.axisLeft
            val rAxis: YAxis = chart.axisRight

            xAxis.granularity = 1.0f // one day
            xAxis.setAvoidFirstLastClipping(true)
            xAxis.labelRotationAngle = -90f

            val xValueFormatter = DaysValueFormatter(translation)
            val yValueFormatter = UnitFormatter(LocaleList.getDefault().get(0).language)
            xAxis.valueFormatter = xValueFormatter
            xAxis.setDrawLabels(true)

            lAxis.setDrawLabels(true)
            lAxis.valueFormatter = yValueFormatter

            rAxis.setDrawLabels(false)
            rAxis.setDrawGridLines(false)

            // lines (expected value, linear regression etc.) have to be visible over
            // possibly many points
            chart.drawOrder = arrayOf(CombinedChart.DrawOrder.LINE,
                CombinedChart.DrawOrder.SCATTER)

            fun recreateChartAndDetailsContent() {
                lifecycleScope.launch(viewModel.exceptionHandler) { chartMutex.withLock {

                    val thisModel = viewModel.modelMutable.value ?: return@withLock
                    if (thisModel !is ModelLogicWithPrediction) return@withLock

                    val generalStatsText: String
                    var predictionExtraText: String = org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
                    var lowerCIntervalDataSet: ScatterDataSet? = null
                    var upperCIntervalDataSet: ScatterDataSet? = null
                    var lowerCIData: List<Entry>? = null
                    var upperCIData: List<Entry>? = null
                    val mainDataSet: ScatterDataSet
                    var predictionDataSet: ScatterDataSet? = null
                    var predictionLineDataSet: LineDataSet? = null

                    // get model data <=> get data to show
                    val dataToShow = thisModel.calculateModelData()
                    val data = dataToShow.first
                    if (data.firstOrNull() !is Entry?) {
                        ErrorHandling.showThrowableAsToast(
                            requireActivity(),
                            Throwable(getString(R.string.stats_error_loading_data))
                        )
                        return@launch
                    }
                    // set text about general statistics of the sample of data
                    generalStatsText = dataToShow.second.getString(translation)

                    val canShowPrediction = data.size >= thisModel.minimalPredictionSampleSize
                    val predictionData : ArrayList<*>
                    var predictionExtras: PredictionExtras? = null

                    if (canShowPrediction) {
                        // get predictions
                        val predictionToShow = thisModel.calculatePredictionData()
                        predictionData = predictionToShow.first
                        predictionExtras = predictionToShow.second
                        if (predictionData.firstOrNull() !is Entry?) {
                            ErrorHandling.showThrowableAsToast(
                                requireActivity(),
                                Throwable(getString(R.string.stats_error_loading_data))
                            )
                            return@launch
                        }
                        // set text about predictions for the sample of data
                        predictionExtraText = predictionToShow.second.extrasAsText

                        // set interval of confidence data to show for predictions if applicable
                        if (predictionToShow.second is PredictionExtrasWithConfidenceIntervals) {
                            lowerCIData = (predictionToShow.second as PredictionExtrasWithConfidenceIntervals)
                                .confidenceIntervals?.map { p -> p.first }
                            upperCIData = (predictionToShow.second as PredictionExtrasWithConfidenceIntervals)
                                .confidenceIntervals?.map { p -> p.second }
                            if (lowerCIData != null && upperCIData != null ) {
                                lowerCIntervalDataSet = ScatterDataSet(
                                    lowerCIData,
                                    getString(R.string.stats_lower_confidence_interval)
                                )
                                lowerCIntervalDataSet.setScatterShape(ScatterChart.ScatterShape.CHEVRON_DOWN)
                                lowerCIntervalDataSet.color = purpleColor

                                upperCIntervalDataSet = ScatterDataSet(
                                    upperCIData,
                                    getString(R.string.stats_upper_confidence_interval)
                                )
                                upperCIntervalDataSet.setScatterShape(ScatterChart.ScatterShape.CHEVRON_UP)
                                upperCIntervalDataSet.color = purpleColor
                            }
                            else {
                                lowerCIData = null
                                upperCIData = null
                            }
                        }
                    }
                    else {
                        // inform user that this model requires more samples
                        predictionData = ArrayList<Entry>()
                        predictionExtraText = getString(R.string.stats_this_model_requires_samples,
                            thisModel.minimalPredictionSampleSize,
                            data.size
                        )
                    }

                    // setting main data for showing
                    mainDataSet = ScatterDataSet(
                        data as ArrayList<Entry>,
                        getString(R.string.stats_measured_values)
                    )
                    mainDataSet.setScatterShape(ScatterChart.ScatterShape.X)
                    mainDataSet.color = blueColor

                    // setting prediction data
                    when(thisModel.predictionType) {
                        PredictionType.CURVE -> {

                            val lPDataSet = LineDataSet(
                                predictionData as ArrayList<Entry>,
                                getString(R.string.stats_predicted_line)
                            )
                            lPDataSet.color = redColor
                            lPDataSet.setDrawCircleHole(false)
                            lPDataSet.circleRadius = 1f
                            lPDataSet.lineWidth = 2f
                            lPDataSet.mode = LineDataSet.Mode.LINEAR
                            lPDataSet.valueTextSize = 1f
                            lPDataSet.axisDependency = YAxis.AxisDependency.LEFT

                            // first two points serve for sketching line
                            val predictionDataWithoutLinePoints = predictionData.filterIndexed {
                                i, _ -> i > 1
                            }

                            val pPDataSet = ScatterDataSet(
                                predictionDataWithoutLinePoints,
                                getString(R.string.stats_predicted_values)
                            )
                            pPDataSet.color = yellowColor
                            pPDataSet.setScatterShape(ScatterChart.ScatterShape.X)

                            predictionDataSet = pPDataSet
                            predictionLineDataSet = lPDataSet
                        }
                        PredictionType.POINTS -> {

                            val pDataSet = ScatterDataSet(
                                predictionData as ArrayList<Entry>,
                                getString(R.string.stats_predicted_values)
                            )
                            pDataSet.setScatterShape(ScatterChart.ScatterShape.CROSS)
                            pDataSet.color = yellowColor
                            predictionDataSet = pDataSet
                        }
                        else -> return@launch
                    }

                    // prepare button showing dialog with graph of extra data if applicable
                    if (predictionExtras != null && predictionExtras is WithExtraNamedData) {
                        showExtraDataButton.isEnabled = true
                        showExtraDataButton.setOnClickListener {
                            val builder: AlertDialog.Builder = AlertDialog.Builder(requireActivity())

                            builder.setView(
                                layoutInflater.inflate(R.layout.fragment_extra_data_plot, null)
                            )

                            builder.setPositiveButton(R.string.ok) { dialog, which ->
                                dialog.dismiss()
                            }

                            val dialog = builder.create()
                            dialog.create()

                            val titleTV = dialog.findViewById<TextView>(R.id.plotTitleTV)
                            val extraTV = dialog.findViewById<TextView>(R.id.plotExtraTV)

                            titleTV.text = predictionExtras.getTitle()
                            extraTV.text = predictionExtras.getExtraText()

                            val chartHere = dialog.findViewById<CombinedChart>(R.id.chart)
                            chartHere.drawOrder = arrayOf(CombinedChart.DrawOrder.BAR)
                            chartHere.description.isEnabled = true
                            chartHere.description.text =
                                getString(R.string.stats_chart_is_scrollable)
                            chartHere.setDrawGridBackground(true);
                            chartHere.setDrawBarShadow(false);
                            chartHere.isHighlightFullBarEnabled = false;

                            val xAxisHere: XAxis = chartHere.xAxis
                            xAxisHere.granularity = 1.0f
                            xAxisHere.setDrawLabels(false)
                            val lAxisHere = chartHere.axisLeft
                            val rAxisHere = chartHere.axisRight
                            rAxisHere.setDrawLabels(false)
                            rAxisHere.setDrawZeroLine(false)
                            lAxisHere.setDrawZeroLine(true)
                            lAxisHere.setDrawGridLines(true)
                            lAxisHere.setDrawGridLines(false)

                            val dataHere = predictionExtras.getExtraData().map { e ->
                                BarEntry(e.x + org.cezkor.towardsgoalsapp.Constants.X_BIAS_FLOAT, e.y) }

                            val dataSet = BarDataSet(
                                dataHere,
                                titleTV.text.toString()
                            )
                            dataSet.color = blueColor
                            dataSet.setDrawValues(false)
                            val dataObj = BarData(dataSet)
                            dataObj.barWidth = 2f
                            chartHere.data = CombinedData().apply { setData(dataObj) }

                            dialog.setOnShowListener {
                                chartHere.invalidate()
                            }

                            dialog.show()
                        }
                    }
                    else {
                        showExtraDataButton.setOnClickListener { /* do nothing */ }
                        showExtraDataButton.isEnabled = false
                    }

                    // calculate boundaries for labels and chart
                    val dataWithPredictions = data + predictionData
                    var minY : Float = if (dataWithPredictions.isEmpty())
                        0f
                    else dataWithPredictions.minOf{ e -> e.y }
                    if (! lowerCIData.isNullOrEmpty()) {
                        val minCIY = lowerCIData.minBy { e -> e.y }.y
                        if (minCIY < minY) minY = minCIY
                    }
                    var maxY : Float = if (dataWithPredictions.isEmpty())
                        0f
                    else dataWithPredictions.maxOf{ e -> e.y }
                    if (! upperCIData.isNullOrEmpty()) {
                        val maxCIY = upperCIData.maxBy { e -> e.y }.y
                        if (maxCIY > maxY) maxY = maxCIY
                    }
                    if (minY > maxY) {
                        // swap
                        val x = minY
                        minY = maxY
                        maxY = x
                    }

                    // days in data should be ordered by model - no need for swapping
                    val minX : Float = if (dataWithPredictions.isEmpty())
                        viewModel.currentDayNumber.toFloat()
                    else dataWithPredictions.minOf{ e -> e.x }
                    val maxXInPredicts = if (dataWithPredictions.isEmpty())
                                        viewModel.currentDayNumber.toFloat()
                                    else dataWithPredictions.maxOf{ e -> e.x }
                    val maxX = ( if (maxXInPredicts > viewModel.currentDayNumber) maxXInPredicts
                                else viewModel.currentDayNumber ).toFloat()

                    // setting formatter values for calculating labels
                    xValueFormatter.current = viewModel.currentDayNumber.toFloat()
                    xValueFormatter.nowIsPreviousDay = viewModel.shiftPredictionLabel
                    val lookOutXBy = 3
                    // extend yAxis range by 10 to the log_10 of range of Y values
                    val r = abs(maxY - minY)
                    val lookOutYBy = if ( ! r.isFinite() || r <= 0f) 0f
                                     else 10.0.pow(log10(r).toDouble()).toFloat()
                    xAxis.axisMinimum = minX - lookOutXBy
                    xAxis.axisMaximum = maxX + lookOutXBy
                    lAxis.axisMinimum = minY - lookOutYBy/2
                    rAxis.axisMinimum = minY - lookOutYBy/2
                    lAxis.axisMaximum = maxY + lookOutYBy/2
                    rAxis.axisMaximum = maxY + lookOutYBy/2

                    // setting unit for y labels
                    val unit = viewModel.currentParamObj?.unit ?: org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING
                    yValueFormatter.unit = unit

                    // extracting expected value for parameter for showing it as dashed line
                    // if possible
                    val exVal = viewModel.currentParamObj?.targetVal
                    var exValueDataSet: LineDataSet? = null
                    if (exVal != null) {

                        val beginning = Entry(minX, exVal.toFloat())
                        val end = Entry(maxX, exVal.toFloat())
                        exValueDataSet = LineDataSet(
                            arrayListOf(beginning, end),
                            getString(R.string.habits_params_expected_value_without_colon)
                        )
                        exValueDataSet.color = greenColor
                        exValueDataSet.setDrawCircleHole(false)
                        exValueDataSet.setDrawValues(true)
                        exValueDataSet.enableDashedLine(.1f, .1f, 0f)
                        exValueDataSet.setDrawIcons(false)
                        exValueDataSet.setDrawCircles(false)

                    }

                    val scatterAr = ArrayList<ScatterDataSet>()
                    val lineAr = ArrayList<LineDataSet>()

                    // adding scatter data to an arraylist
                    scatterAr.add(mainDataSet)
                    scatterAr.add(predictionDataSet)
                    if (lowerCIntervalDataSet != null && upperCIntervalDataSet != null) {
                        scatterAr.add(lowerCIntervalDataSet)
                        scatterAr.add(upperCIntervalDataSet)
                    }
                    if (predictionLineDataSet != null) lineAr.add(predictionLineDataSet)
                    if (exValueDataSet != null) lineAr.add(exValueDataSet)


                    // filling chart with datasets
                    val combinedData = CombinedData()
                    combinedData.setData(ScatterData(scatterAr as List<IScatterDataSet>?))
                    combinedData.setData(LineData(lineAr as List<ILineDataSet>))
                    chart.data = combinedData

                    // setting text about statistics of model data and given model
                    modelDetailsTextView.text = "%s\n%s".format(
                        generalStatsText,
                        predictionExtraText
                    )

                    // zoom out
                    chart.zoom(0.1f, 0.1f, 0f, 0f)
                    // this method will invalidate chart and move it to the
                    // newest X value (day number)
                    chart.moveViewToX(maxX)
                } }

            }

            // setting recycler view to be nonscrollable, assume fixed size items
            // (all of them will have 50 dp)
            modelSettingsRecyclerView.layoutManager = NonScrollableLayoutManager(context)
            modelSettingsRecyclerView.setHasFixedSize(true)

            val modelAdapter = ArrayAdapter(requireContext(),
                android.R.layout.simple_spinner_item, modelArray)
            modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            modelSpinner.adapter = modelAdapter
            modelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    try {
                        // create new model object based on picked element position
                        val modelName = modelArray[position]
                        val model =
                            createModelFromEnum(modelName.enumValue, translation) ?: return
                        lifecycleScope.launch { chartMutex.withLock {
                            viewModel.modelMutable.value = model
                            // set data to model if possible
                            val values = viewModel.currentParamDataMutable.value ?: return@withLock
                            model.setData(values)
                        } }

                    }
                    catch (e : IndexOutOfBoundsException) {
                        // ignore
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // ignore
                }
            }
            paramsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    try {
                        // set parameter data based on picked parameters
                        val param = viewModel.paramArrayMutable.value?.get(position) ?: return
                        viewModel.currentParamId = param.paramId
                        viewModel.currentParamObj = param
                        lifecycleScope.launch { viewModel.fetchValuesOfCurrentParam() }
                    }
                    catch (e : IndexOutOfBoundsException) {
                        // ignore
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // ignore
                }
            }

            viewModel.modelMutable.observe(viewLifecycleOwner) {
                if (it == null) return@observe
                showExtraDataButton.isEnabled = false
                val settings = it.modelSettings.values.toCollection(ArrayList())

                modelSettingsRecyclerView.adapter = ModelSettingsAdapter(
                    settings,
                    lifecycleScope,
                    chartMutex
                ).apply {
                    stateRestorationPolicy = RecyclerView.Adapter
                        .StateRestorationPolicy.PREVENT_WHEN_EMPTY
                    this.setOnSettingChanged { recreateChartAndDetailsContent() }
                    this.setOnHelpRequested { s ->
                        showHelpDialog(s)
                    }
                }

                recreateChartAndDetailsContent()
            }

            viewModel.modelSettingChanged.observe(viewLifecycleOwner) {
                it?.handleIfNotHandledWith {
                    showExtraDataButton.isEnabled = false
                    recreateChartAndDetailsContent()
                }
            }

            viewModel.paramArrayMutable.observe(viewLifecycleOwner) {
                if (it == null || it.isEmpty()) return@observe
                showExtraDataButton.isEnabled = false
                val paramNames = it.map { hp -> hp.name }.toCollection(ArrayList()).toArray()
                val paramsAdapter = ArrayAdapter(requireContext(),
                    android.R.layout.simple_spinner_item, paramNames)
                paramsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                paramsSpinner.adapter = paramsAdapter
            }

            viewModel.currentParamDataMutable.observe(viewLifecycleOwner) {
                if (it != null && it.isNotEmpty()) {
                    showExtraDataButton.isEnabled = false
                    lifecycleScope.launch {
                        // process data if possible
                        var doShow = false
                        var modelAt = modelSpinner.selectedItemPosition
                        if (modelAt == -1) modelAt = 0
                        val modelName = modelArray[modelAt]
                        chartMutex.withLock {
                            val model = viewModel.modelMutable.value
                                ?: createModelFromEnum(modelName.enumValue, translation)
                                ?: return@withLock
                            model.setData(it)
                            // if there is no model - set it
                            if (viewModel.modelMutable.value == null) {
                                viewModel.modelMutable.value = model
                                doShow = false
                            } else doShow = true
                        }
                        if (doShow) recreateChartAndDetailsContent()
                    }
                }
            }

        }

        fun doInThisOrder() {

            prepareViewModel()

            prepareUI()

            // by default there is no extra data to show
            showExtraDataButton.isEnabled = false

            lifecycleScope.launch(viewModel.exceptionHandler) {
                viewModel.getAndReadyEverything()

                // pick first element if possible
                modelSpinner.setSelection(0)
                paramsSpinner.setSelection(0)
            }

        }; doInThisOrder()

    }

}