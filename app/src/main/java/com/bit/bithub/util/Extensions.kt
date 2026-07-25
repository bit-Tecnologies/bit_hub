package com.bit.bithub.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.format.Formatter

/**
 * Вибрирует коротко (haptic feedback).
 */
fun Context.vibrate(duration: Long = 15) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(duration)
            }
        }
    } catch (_: Exception) { }
}

/**
 * Форматирует размер байтов в читаемую строку (10 MB, 1.2 GB и т.д.)
 */
fun Long?.formatFileSize(context: Context): String {
    if (this == null || this <= 0) return ""
    return Formatter.formatFileSize(context, this)
}
