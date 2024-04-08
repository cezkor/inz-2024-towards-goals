package com.example.towardsgoalsapp.stats.models

import android.content.Context
import com.example.towardsgoalsapp.etc.AndroidContextTranslation
import com.example.towardsgoalsapp.etc.Translation

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