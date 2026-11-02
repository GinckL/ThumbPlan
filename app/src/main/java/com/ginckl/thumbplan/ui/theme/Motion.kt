package com.ginckl.thumbplan.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween

object Motion {
    // Durations
    const val fast = 150
    const val base = 250
    const val slow = 400

    // Curves
    val standardCurve: Easing = CubicBezierEasing(0.2f, 0.0f, 0.2f, 1.0f) // easeOutCubic approximation
    val entranceCurve: Easing = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.275f) // easeOutBack (mild overshoot)
    val exitCurve: Easing = CubicBezierEasing(0.55f, 0.055f, 0.675f, 0.19f) // easeInCubic

    // Tweens
    fun <T> standardSpec() = tween<T>(durationMillis = base, easing = standardCurve)
    fun <T> entranceSpec() = tween<T>(durationMillis = base, easing = entranceCurve)
    fun <T> exitSpec() = tween<T>(durationMillis = fast, easing = exitCurve)
}
