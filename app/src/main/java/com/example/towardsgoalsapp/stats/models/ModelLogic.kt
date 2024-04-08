package com.example.towardsgoalsapp.stats.models

import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.R
import com.github.mikephil.charting.data.Entry
import com.example.towardsgoalsapp.etc.Translation
import kotlinx.coroutines.sync.Mutex
import java.time.Duration

abstract class ModelLogic<ChartDataType> (protected val translation: Translation) {

    protected var dataArray: Any? = null

    protected var calculateMutex = Mutex()

    abstract suspend fun setData(dataArrayOfOtherKind: Any)

    open val modelSettings: HashMap<Any, ModelSetting> = HashMap()

    // model data is data to be shown as data used in model and, if applicable, their statistics
    abstract suspend fun calculateModelData(): Pair<ArrayList<ChartDataType>, StatsOfData>

}

enum class PredictionType {
    NO_PREDICTION, CURVE, POINTS
}

abstract class PredictionExtras {
    // model should define what it extras it contains

    open var extrasAsText : String = Constants.EMPTY_STRING
        protected set
    abstract fun setString(translation: Translation)
}

abstract class PredictionExtrasWithConfidenceIntervals(
    val confidenceIntervals: ArrayList<Pair<Entry, Entry>>?
) : PredictionExtras()

interface WithExtraData {
    fun getExtraData() : ArrayList<Entry>

    fun getExtraText() : String

    fun getTitle() : String
}

abstract class ModelLogicWithPrediction<DataType> (translation: Translation) : ModelLogic<DataType>(translation) {

    // prediction data is predicted values and any additional information about predictions
    abstract suspend fun calculatePredictionData() : Pair<ArrayList<DataType>, PredictionExtras>

    open val predictionType: PredictionType = PredictionType.NO_PREDICTION

    // minimal size of sample for which prediction is possible
    open val minimalPredictionSampleSize : Int = 0

}

abstract class ModelSetting (protected val translation: Translation) {
    open var value: Any? = null
    abstract val settingName: String
}

interface WithHelpText {

    fun getHelpText() : String?

}

open class BooleanModelSetting(translation: Translation, stringResId: Int) : ModelSetting(translation) {
    override val settingName = translation.getString(stringResId)

    fun setValue(bool: Boolean) {value = bool}

}

class BooleanModelSettingWithHelp(translation: Translation, stringResId: Int,
                                  private val helpStringResId: Int)
    : BooleanModelSetting(translation, stringResId), WithHelpText {
    override fun getHelpText(): String? = translation.getString(helpStringResId)
}


abstract class ModelSettingWithChoices<T>(translation: Translation, defaultChoiceIdx: Int): ModelSetting(translation) {
    open var choiceIdx: Int = defaultChoiceIdx
        protected set
    abstract val choices: Array<T>
    abstract val choicesNames: Array<String>
    open var choice: T? = null
        protected set

    open fun setChoice(choiceIdx: Int) {
        if (choiceIdx == -1 || choiceIdx > choices.size) return
        choice = choices[choiceIdx]
        value = choice
        this.choiceIdx = choiceIdx
    }
}

class SampleSize(translation: Translation, defaultChoiceIdx: Int = 0)
    : ModelSettingWithChoices<Int>(translation, defaultChoiceIdx), WithHelpText {

    override fun getHelpText(): String? =
        translation.getString(R.string.stats_sample_size)

    override val settingName: String = translation.getString(R.string.stats_sample_size_short)
    companion object {
        const val ALL_SAMPLES:Int = -100
    }

    override val choices = arrayOf( ALL_SAMPLES, 20, 50, 100, 7, 14, 28, 6*28 )
    override val choicesNames: Array<String>
        = arrayOf( translation.getString(R.string.stats_all_samples),
            "${choices[1]}", "${choices[2]}", "${choices[3]}", "${choices[4]}",
            "${choices[5]}", "${choices[6]}",
            "${choices[7]} (${translation.getString(R.string.stats_half_a_year)}) "
        )

    override var choice: Int? = choices[defaultChoiceIdx]

}

class PredictSize(translation: Translation, defaultChoiceIdx: Int = 0)
    : ModelSettingWithChoices<Int>(translation, defaultChoiceIdx) {

    override val settingName: String = translation.getString(R.string.stats_prediction_size)

    override val choices = arrayOf( 1, 3, 7, 14, 28)
    override val choicesNames: Array<String>
            = arrayOf( choices[0].toString(),
        choices[1].toString(), choices[2].toString(), choices[3].toString(),
        choices[4].toString())

    override var choice: Int? = choices[defaultChoiceIdx]

}

class ShowDataFrom(translation: Translation, defaultChoiceIdx: Int = 0)
    : ModelSettingWithChoices<Duration?>(translation, defaultChoiceIdx) {

    override val settingName: String = translation.getString(R.string.stats_show_data_from_last)
    companion object {
    }

    override val choices = arrayOf( null,
        Duration.ofDays(7),
        Duration.ofDays(14),
        Duration.ofDays(28),
        Duration.ofDays(3*28),
        Duration.ofDays(6*28)
    )
    override val choicesNames: Array<String> = arrayOf(
        translation.getString(R.string.stats_everything),
        translation.getString(R.string.stats_one_week),
        translation.getString(R.string.stats_two_week),
        translation.getString(R.string.stats_one_month),
        translation.getString(R.string.stats_one_quarter),
        translation.getString(R.string.stats_half_a_year)
    )

    override var choice: Duration? = choices[defaultChoiceIdx]

}
