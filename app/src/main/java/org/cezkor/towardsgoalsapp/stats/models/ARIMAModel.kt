package org.cezkor.towardsgoalsapp.stats.models

import org.cezkor.towardsgoalsapp.database.HabitParameterValue
import com.github.mikephil.charting.data.Entry
import org.cezkor.towardsgoalsapp.Constants.Companion.X_BIAS_FLOAT
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.etc.Translation
import com.github.signaflo.timeseries.TimePeriod
import com.github.signaflo.timeseries.TimeSeries
import com.github.signaflo.timeseries.TimeUnit
import com.github.signaflo.timeseries.model.Model
import com.github.signaflo.timeseries.model.arima.Arima
import com.github.signaflo.timeseries.model.arima.ArimaOrder
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode

class SeasonSize(translation: Translation, defaultChoiceIdx: Int = 0)
    : ModelSettingWithChoices<TimePeriod?>(translation, defaultChoiceIdx) {

    override val settingName: String = translation.getString(R.string.stats_season_size)
    companion object {
    }

    override val choices = arrayOf(
        null,
        TimePeriod.oneWeek(),
        TimePeriod(TimeUnit.WEEK, 2),
        TimePeriod(TimeUnit.WEEK, 4),
        TimePeriod(TimeUnit.WEEK, 3*4)
    )
    override val choicesNames: Array<String>
            = arrayOf(
                translation.getString(R.string.stats_no_seasonality),
                translation.getString(R.string.stats_one_week),
                translation.getString(R.string.stats_two_week),
                translation.getString(R.string.stats_one_month),
                translation.getString(R.string.stats_one_quarter)
            )

    override var choice: TimePeriod? = choices[defaultChoiceIdx]

}

class ARRank(translation: Translation, defaultChoiceIdx: Int = 0)
    : ModelSettingWithChoices<Int>(translation, defaultChoiceIdx), WithHelpText {

    override fun getHelpText(): String? = translation.getString(R.string.stats_ar_rank)

    override val settingName: String = translation.getString(R.string.stats_ar_rank_short)
    companion object {
    }

    override val choices = arrayOf( 0, 1, 2, 3 )
    override val choicesNames: Array<String>
            = arrayOf( translation.getString(R.string.stats_no_autoregression),
        choices[1].toString(), choices[2].toString(), choices[3].toString())

    override var choice: Int? = choices[defaultChoiceIdx]

}

class MARank(translation: Translation, defaultChoiceIdx: Int = 0)
    : ModelSettingWithChoices<Int>(translation, defaultChoiceIdx), WithHelpText {

    override fun getHelpText() : String? = translation.getString(R.string.stats_ma_rank)

    override val settingName: String = translation.getString(R.string.stats_ma_rank_short)
    companion object {
    }

    override val choices = arrayOf( 0 ,1, 2, 3 )
    override val choicesNames: Array<String>
            = arrayOf(translation.getString(R.string.stats_no_moving_average),
        choices[1].toString(), choices[2].toString(), choices[3].toString())

    override var choice: Int? = choices[defaultChoiceIdx]

}

class ARIMASampleDaySize(translation: Translation, defaultChoiceIdx: Int = 0)
    : ModelSettingWithChoices<Int>(translation, defaultChoiceIdx), WithHelpText{

    override fun getHelpText(): String? =
        translation.getString(R.string.stats_sample_size)

    override val settingName: String = translation.getString(R.string.stats_sample_size_short)

    override val choices = arrayOf(28, 3 * 28, 6 * 28)
    override val choicesNames: Array<String> = arrayOf(
        choices[0].toString(),
        choices[1].toString(),
        "${choices[2]} (${translation.getString(R.string.stats_half_a_year)})"
    )

    override var choice: Int? = choices[defaultChoiceIdx]

}

class IntegrationRank(translation: Translation, defaultChoiceIdx: Int = 0)
    : ModelSettingWithChoices<Int>(translation, defaultChoiceIdx), WithHelpText {

    override fun getHelpText(): String? {
        return translation.getString(R.string.stats_integration_long)
    }

    override val settingName: String = translation.getString(R.string.stats_integration)

    override val choices = arrayOf( 0, 1, 2, 3 )
    override val choicesNames: Array<String>
            = arrayOf( translation.getString(R.string.stats_stationary),
        translation.getString(R.string.stats_linear_trend),
        choices[2].toString(), choices[3].toString())

    override var choice: Int? = choices[defaultChoiceIdx]

}

class ARIMAModelPredictionExtras(
    private val translation: Translation,
    val sampleSize: Int,
    val confidenceLevel: Double,
    private val cIntervals: ArrayList<Pair<Entry, Entry>>,
    private val extraData: ArrayList<Entry>
): PredictionExtrasWithConfidenceIntervals(cIntervals), WithExtraNamedData {

    companion object {
        const val SAMPLE_SIZE_TO_BE_ACCURATE = 100
    }

    override fun getTitle(): String = translation.getString(R.string.stats_residues)
    override fun getExtraData(): ArrayList<Entry> = extraData

    override fun getExtraText(): String = translation.getString(R.string.stats_residues_explained)

    override fun setString(translation: Translation) {
        extrasAsText = translation.getString(R.string.stats_arima_regression_text).format(
            sampleSize,
            if (sampleSize < SAMPLE_SIZE_TO_BE_ACCURATE)
                translation.getString(R.string.stats_may_not_be_accurate_for_sample).format(
                    SAMPLE_SIZE_TO_BE_ACCURATE.toString()
                )
            else org.cezkor.towardsgoalsapp.Constants.EMPTY_STRING,
            BigDecimal(confidenceLevel).setScale(2, RoundingMode.HALF_EVEN).toString()
        )
    }
}

class OneVariableARIMAModel(translation: Translation): ModelLogicWithPrediction<Entry>(translation) {

    companion object {
        private const val FILL_MISSING_WITH_AVERAGE = 0
        private const val I_RANK = 1
        private const val SEASONALITY = 2
        private const val AR_RANK = 3
        private const val MA_RANK = 4
        private const val INCLUDE_CONSTANT_IF_POSSIBLE = 5
        private const val SAMPLE_SIZE = 6
        private const val PREDICT_SIZE = 7

        private const val ALPHA = 0.05
    }

    private var seriesOfData: TimeSeries = TimeSeries.from(0.0)
    private var samplesForData: ArrayList<HabitParameterValue> = arrayListOf()
    private var trueData: ArrayList<HabitParameterValue> = arrayListOf()

    private var oldFill = false
    private var oldSampleSize: Int = 28
    private var newData = false

    init {
        // SampleSize, by default 7
        modelSettings[SAMPLE_SIZE] = ARIMASampleDaySize(translation).apply { this.setChoice(0) }
        modelSettings[PREDICT_SIZE] = PredictSize(translation)
        modelSettings[SEASONALITY] = SeasonSize(translation).apply { this.setChoice(0) }
        // Autoregression of rank 1 (one value in past is included in calculation of next)
        modelSettings[AR_RANK] = ARRank(translation).apply { this.setChoice(1) }
        // no moving average
        modelSettings[MA_RANK] = MARank(translation).apply { this.setChoice(0) }
        modelSettings[FILL_MISSING_WITH_AVERAGE] =
            BooleanModelSettingWithHelp(translation,
            R.string.stats_fill_missing_with_average_short,
            R.string.stats_fill_missing_with_average).apply { setValue(false) }
        // assume stationary process - no need of differentiation
        modelSettings[I_RANK] = IntegrationRank(translation).apply { this.setChoice(0) }
        modelSettings[INCLUDE_CONSTANT_IF_POSSIBLE] = BooleanModelSettingWithHelp(translation,
            R.string.stats_include_constant,
            R.string.stats_include_constant_if_rank_is_low
        ).apply { setValue(false) }
    }

    override val predictionType: PredictionType = PredictionType.POINTS

    override val minimalPredictionSampleSize: Int = 50

    private fun getSampleCount() : Int {
        val all = trueData.size
        val sampleSize = modelSettings[SAMPLE_SIZE] ?: return all
        if (sampleSize !is ModelSettingWithChoices<*>) return all
        if (sampleSize.choice == null) return all
        if (sampleSize.choice == SampleSize.ALL_SAMPLES) return all
        return sampleSize.choice as Int
    }

    private fun recalcDataIfNeeded() {

        val fillSett = modelSettings[FILL_MISSING_WITH_AVERAGE] as BooleanModelSetting?
        val fill = ( fillSett != null ) && fillSett.value as Boolean
        val fillChanged = fill != oldFill

        val expectedSize = getSampleCount()
        val sizeChanged = expectedSize != oldSampleSize

        if (fillChanged || sizeChanged || newData) {
            oldFill = fill
            oldSampleSize = expectedSize

            val beginAt = if (trueData.size > expectedSize) trueData.size - expectedSize else 0
            val samples = if (trueData.isEmpty()) ArrayList()
            else trueData.subList(beginAt, trueData.size).toCollection(ArrayList())

            this.seriesOfData =
                DataAdapter.fromHabitParameterValueArrayToOrderedSignalfloTimeSeries(
                    samples, fill
                )

            samplesForData = samples
            newData = false
        }
    }

    override suspend fun calculatePredictionData(): Pair<ArrayList<Entry>, PredictionExtras> =
        calculateMutex.withLock{
        recalcDataIfNeeded()

        val ar: ArrayList<Entry> = arrayListOf()

        val arRank = (modelSettings[AR_RANK] as ARRank?)?.value as Int? ?: 0
        val maRank = (modelSettings[MA_RANK] as MARank?)?.value as Int? ?: 0
        val predictSize = modelSettings[PREDICT_SIZE]?.value as Int? ?: 1
        val integrationRank = ((modelSettings[I_RANK] as IntegrationRank?)?.value as Int? ?: 0)
        val season = (modelSettings[SEASONALITY] as SeasonSize?)?.value as TimePeriod?
        val includeConstant : Boolean=
            (modelSettings[INCLUDE_CONSTANT_IF_POSSIBLE] as BooleanModelSetting?)?.value as Boolean?
                ?: false
        val arimaConstant = if (includeConstant) Arima.Constant.INCLUDE else Arima.Constant.EXCLUDE

        val model: Model
        = if (season == null)
            Arima.model(
                seriesOfData,
                ArimaOrder.order( // non-seasonal
                    arRank, integrationRank, maRank, arimaConstant
                )
            )
        else
            Arima.model(
                seriesOfData,
                ArimaOrder.order( // seasonal without non-seasonal part
                    0, 0, 0, arRank, integrationRank, maRank, arimaConstant
                ),
                season
            )

        // computing residuals
        val eArr = ArrayList<Entry>()
        val fittedValues = model.fittedSeries().asList()
        val observations = model.observations().asList()
        for (i in fittedValues.indices) {
            eArr.add(
                Entry (
                    i.toFloat(),
                    (observations[i] - fittedValues[i]).toFloat(),
                )
            )
        }

        val highestX = samplesForData.maxOf { hpv -> hpv.habitDayNumber }
        val forecasts = model.forecast(predictSize, ALPHA)

        val forecastsValues = forecasts.pointEstimates().asList()
        val lowerBoundValues = forecasts.lowerPredictionInterval().asList()
        val upperBoundValues = forecasts.upperPredictionInterval().asList()
        val boundsOfConfidenceIntervals = ArrayList<Pair<Entry, Entry>>()
        for (i in 0..<predictSize) {
            val p = highestX + i + 1
            ar.add(
                Entry(
                    p.toFloat() + X_BIAS_FLOAT,
                    forecastsValues[i].toFloat()
                )
            )
            boundsOfConfidenceIntervals.add(
                Pair(
                    Entry(
                        p.toFloat() + X_BIAS_FLOAT,
                        lowerBoundValues[i].toFloat()
                    ),
                    Entry(
                        p.toFloat() + X_BIAS_FLOAT,
                        upperBoundValues[i].toFloat()
                    )
                )
            )
        }

        val extras = ARIMAModelPredictionExtras(
            translation,
            seriesOfData.size(),
            1 - ALPHA,
            boundsOfConfidenceIntervals,
            eArr
        )
        extras.setString(translation)

        return Pair(ar, extras)
    }

    override suspend fun calculateModelData(): Pair<ArrayList<Entry>, StatsOfData> = calculateMutex.withLock{
        recalcDataIfNeeded()
        val ar = ArrayList<Entry>()
        var stats = GeneralStats(0.0, 0.0,0.0,0.0,0.0)
        try {
            stats = GeneralStats.fromDoubleData(samplesForData.map { t -> t.value_ }.toDoubleArray())
            ar.addAll(samplesForData
                .map { dar ->
                    Entry(
                        dar.habitDayNumber.toFloat() + X_BIAS_FLOAT ,
                        dar.value_.toFloat())
                })
        }
        catch (e : IndexOutOfBoundsException) {
            ar.clear()
        }
        return Pair(ar, stats)
    }

    override suspend fun setData(dataArrayOfOtherKind: Any) = calculateMutex.withLock{
        if (dataArrayOfOtherKind is ArrayList<*>) {
            if (dataArrayOfOtherKind.firstOrNull() is HabitParameterValue?) {
                newData = true
                trueData = dataArrayOfOtherKind as ArrayList<HabitParameterValue>
                trueData.sortBy { hpv -> hpv.habitDayNumber }
                recalcDataIfNeeded()
            }
        }
    }


}