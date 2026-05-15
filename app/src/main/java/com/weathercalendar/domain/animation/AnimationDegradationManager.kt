package com.weathercalendar.domain.animation

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 动画降级管理器 — 根据电量、省电模式和设备性能自动降级动画。
 *
 * 降级规则：
 * - 低内存设备（isLowRamDevice）：始终禁用动画
 * - 电量 < 15%：禁用图标动画，转场缩短到 200ms
 * - 省电模式：禁用图标动画，转场即时切换（0ms）
 * - 电量 > 20% 且非省电：恢复全动画
 * - 电量 15%-20%：保持当前状态（滞后区间，避免频繁切换）
 */
@Singleton
class AnimationDegradationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class DegradationState(
        val iconAnimationEnabled: Boolean = true,
        val transitionDuration: Int = 700,  // ms
        val reason: DegradationReason = DegradationReason.NONE,
    )

    enum class DegradationReason {
        NONE, LOW_BATTERY, POWER_SAVING, LOW_DEVICE
    }

    private val _state = MutableStateFlow(DegradationState())
    val state: StateFlow<DegradationState> = _state.asStateFlow()

    /** 是否为低内存设备（只检查一次） */
    private val isLowRamDevice: Boolean by lazy {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        am?.isLowRamDevice ?: false
    }

    /**
     * 检查当前电量和省电模式，更新降级状态。
     * 在 App resume 时调用。
     */
    fun checkAndUpdate() {
        // 低内存设备始终降级
        if (isLowRamDevice) {
            _state.value = DegradationState(
                iconAnimationEnabled = false,
                transitionDuration = 200,
                reason = DegradationReason.LOW_DEVICE,
            )
            return
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSaving = powerManager?.isPowerSaveMode ?: false

        if (isPowerSaving) {
            _state.value = DegradationState(
                iconAnimationEnabled = false,
                transitionDuration = 0,
                reason = DegradationReason.POWER_SAVING,
            )
            return
        }

        val batteryLevel = getBatteryLevel()

        when {
            batteryLevel < 15 -> {
                _state.value = DegradationState(
                    iconAnimationEnabled = false,
                    transitionDuration = 200,
                    reason = DegradationReason.LOW_BATTERY,
                )
            }
            batteryLevel > 20 -> {
                _state.value = DegradationState(
                    iconAnimationEnabled = true,
                    transitionDuration = 700,
                    reason = DegradationReason.NONE,
                )
            }
            // 15-20% 区间：保持当前状态（滞后）
        }
    }

    private fun getBatteryLevel(): Int {
        return try {
            val batteryStatus = context.registerReceiver(
                null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) (level * 100) / scale else 100
        } catch (_: Exception) {
            100  // 默认正常模式
        }
    }
}
