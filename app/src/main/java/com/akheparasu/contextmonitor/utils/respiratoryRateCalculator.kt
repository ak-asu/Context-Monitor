package com.akheparasu.contextmonitor.utils

import kotlin.math.abs
import kotlin.math.pow

fun respiratoryRateCalculator(
    accelValues: MutableList<FloatArray>
): Int {
    var previousValue: Float
    var currentValue: Float
    previousValue = 10f
    var k = 0
    for (i in 11..<accelValues.size) {
        currentValue = kotlin.math.sqrt(
            accelValues[i][2].toDouble().pow(2.0) + accelValues[i][0].toDouble()
                .pow(2.0) + accelValues[i][1].toDouble().pow(2.0)
        ).toFloat()
        if (abs(x = previousValue - currentValue) > 0.15) {
            k++
        }
        previousValue = currentValue
    }
    val ret = (k.toDouble() / MAX_PROGRESS)
    return (ret * 30).toInt()
}