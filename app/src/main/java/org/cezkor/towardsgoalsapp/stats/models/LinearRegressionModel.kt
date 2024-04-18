package org.cezkor.towardsgoalsapp.stats.models

import org.cezkor.towardsgoalsapp.database.HabitParameterValue
import com.github.mikephil.charting.data.Entry
import org.apache.commons.math3.stat.regression.SimpleRegression
import org.cezkor.towardsgoalsapp.Constants.Companion.X_BIAS_FLOAT
import org.cezkor.towardsgoalsapp.R
import org.cezkor.towardsgoalsapp.etc.Translation
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode

class RegressionResults(
    var rSquared: Double,
    var a: Double,
    var b: Double,
): PredictionExtras() {
    companion object {
        val stringFormatId: Int = R.string.stats_regression_text
    }
    override fun setString(translation: Translation) {
        extrasAsText = translation.getString(stringFormatId)
            .format(
                BigDecimal(rSquared).setScale(2, RoundingMode.HALF_EVEN).toString(),
                BigDecimal(a).setScale(2, RoundingMode.HALF_EVEN).toString(),
                BigDecimal(b).setScale(2, RoundingMode.HALF_EVEN).toString()
            )
    }
}
class OneVariableLinearRegressionModel(translation: Translation): ModelLogicWithPrediction<Entry>(translation) {

    companion object {
        private const val SAMPLE_SIZE = 0
        private const val PREDICT_SIZE = 1
    }

    private var data: Array<DoubleArray> = arrayOf()

    init {
        // SampleSize, by default 14
        modelSettings[SAMPLE_SIZE] = SampleSize(translation).apply { this.setChoice(1) }
        modelSettings[PREDICT_SIZE] = PredictSize(translation)
    }

    override val predictionType: PredictionType = PredictionType.CURVE

    // line needs at least 2 points
    override val minimalPredictionSampleSize: Int = 2

    private val regression: SimpleRegression = SimpleRegression()

    private fun getSampleCount() : Int {
        val all = data.size
        val sampleSize = modelSettings[SAMPLE_SIZE] ?: return all
        if (sampleSize !is ModelSettingWithChoices<*>) return all
        if (sampleSize.choice == null) return all
        if (sampleSize.choice == SampleSize.ALL_SAMPLES) return all
        return sampleSize.choice as Int
    }

    override suspend fun calculatePredictionData(): Pair<ArrayList<Entry>, PredictionExtras> =
    calculateMutex.withLock {
        val expectedSize = getSampleCount()
        val beginAt = if (data.size > expectedSize) data.size - expectedSize else 0
        val predictSize = modelSettings[PREDICT_SIZE]?.value as Int? ?: 1

        val ar = ArrayList<Entry>()
        val extras = RegressionResults(0.0, 0.0, 0.0)
        try {
            val samples = data.sliceArray(IntRange(beginAt, data.size - 1))
            regression.clear()
            regression.addData(samples)
            val results = regression.regress()
            extras.rSquared = results.rSquared
            extras.a = regression.slope
            extras.b = regression.intercept

            val zerothX = data.first()[0]
            val lastKnownX = data.last()[0]
            ar.add(Entry(
                zerothX.toFloat() + X_BIAS_FLOAT,
                regression.predict(zerothX).toFloat()))
            ar.add(Entry(
                lastKnownX.toFloat() + X_BIAS_FLOAT,
                regression.predict(lastKnownX).toFloat()))
            for (i in 1..predictSize) {
                val xx = lastKnownX + i
                ar.add( Entry(
                    xx.toFloat(),
                    regression.predict(xx).toFloat()
                ))
            }

            extras.setString(translation)

        }
        catch (e : IndexOutOfBoundsException) {
            ar.clear()
        }
        return Pair(ar, extras)
    }

    override suspend fun calculateModelData(): Pair<ArrayList<Entry>, StatsOfData> =
    calculateMutex.withLock {
        val expectedSize = getSampleCount()
        val beginAt = if (data.size > expectedSize) data.size - expectedSize else 0

        val ar = ArrayList<Entry>()
        var stats = GeneralStats(0.0, 0.0,0.0,0.0,0.0)
        try {
            val sample = data.sliceArray(IntRange(beginAt, data.size - 1))
            stats = GeneralStats.fromDoubleData(sample.map { t -> t[1] }.toDoubleArray())
            ar.addAll(sample
                .map { dar -> Entry(dar[0].toFloat() + X_BIAS_FLOAT , dar[1].toFloat()) })
        }
        catch (e : IndexOutOfBoundsException) {
            ar.clear()
        }
        return Pair(ar, stats)
    }

    override suspend fun setData(dataArrayOfOtherKind: Any) = calculateMutex.withLock {
        if (dataArrayOfOtherKind is ArrayList<*>) {
            if (dataArrayOfOtherKind.firstOrNull() is HabitParameterValue?) {
                this.data =
                    DataAdapter.fromHabitParameterValueArrayTo2DDoubleArray(
                        dataArrayOfOtherKind as ArrayList<HabitParameterValue>
                    )
            }
        }

        if (data.isNotEmpty()) {
            // try to sort by x
            try {
                data.sortBy { t -> t[0] }
            }
            catch (e: IndexOutOfBoundsException) {
                // ignore
            }
        }
    }


}