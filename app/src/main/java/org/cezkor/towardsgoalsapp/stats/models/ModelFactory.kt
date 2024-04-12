package org.cezkor.towardsgoalsapp.stats.models

import org.cezkor.towardsgoalsapp.etc.Translation

class HabitModelFactory {

    companion object {

        fun createModelFromEnum(enum: HabitModelEnum, translation: Translation) : ModelLogic<*>? {
            return when (enum) {
                HabitModelEnum.LINEAR_REGRESSION -> OneVariableLinearRegressionModel(translation)
                HabitModelEnum.ARIMA -> OneVariableARIMAModel(translation)
                else -> null
            }
        }

    }

}