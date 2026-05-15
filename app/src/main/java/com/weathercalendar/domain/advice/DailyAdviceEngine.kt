package com.weathercalendar.domain.advice

import com.weathercalendar.data.model.DailyWeather
import com.weathercalendar.data.model.WeatherCondition
import java.time.LocalDate
import java.time.Month

/**
 * 今日宜忌引擎 — 基于天气+温度+风速+节气生成 3-5 条生活建议。
 *
 * 设计原则：
 * - 具象、可操作（"宜晨跑"而非"适合运动"）
 * - 基于真实数据（温度/风速/降水），不是随机生成
 * - 每天内容不同（天气变化驱动）
 */
object DailyAdviceEngine {

    data class DailyAdvice(
        val suitable: List<String>,     // 宜（3-5 条）
        val unsuitable: List<String>,   // 忌（2-3 条）
    )

    /**
     * 生成今日宜忌。
     * @param weather 今天的天气数据
     * @param date 日期（用于节气判断）
     */
    fun generate(weather: DailyWeather?, date: LocalDate = LocalDate.now()): DailyAdvice {
        if (weather == null) return DailyAdvice(listOf("查看天气"), listOf("—"))

        val suitable = mutableListOf<String>()
        val unsuitable = mutableListOf<String>()

        // ── 基于天气条件 ──
        when (weather.condition) {
            WeatherCondition.SUNNY, WeatherCondition.PARTLY_CLOUDY -> {
                suitable.add("☀️ 户外散步")
                suitable.add("🌿 晾晒衣物")
                if (weather.tempMax in 15..28) suitable.add("🏃 晨跑锻炼")
                if (weather.tempMax in 20..30) suitable.add("📸 外出拍照")
            }
            WeatherCondition.CLOUDY -> {
                suitable.add("🚶 外出办事")
                suitable.add("🌿 适合散步")
                unsuitable.add("🌅 观赏日出日落")
            }
            WeatherCondition.RAINY, WeatherCondition.DRIZZLE -> {
                suitable.add("📖 室内阅读")
                suitable.add("☕ 品茶听雨")
                unsuitable.add("🏃 户外运动")
                unsuitable.add("🌿 晾晒衣物")
                if (weather.precip > 10) unsuitable.add("🚗 长途驾车")
            }
            WeatherCondition.SNOWY -> {
                suitable.add("🏠 居家休息")
                suitable.add("🍲 煲汤暖身")
                unsuitable.add("🚗 远行出门")
                unsuitable.add("🏃 户外运动")
            }
            WeatherCondition.STORMY -> {
                suitable.add("🏠 待在室内")
                suitable.add("📖 读书学习")
                unsuitable.add("⛱️ 一切户外活动")
                unsuitable.add("🚗 驾车出行")
            }
            WeatherCondition.FOGGY -> {
                suitable.add("🏠 室内活动")
                unsuitable.add("🚗 高速驾车")
                unsuitable.add("🏃 户外晨练")
            }
        }

        // ── 基于温度 ──
        when {
            weather.tempMax > 35 -> {
                suitable.add("🧊 多喝水防暑")
                unsuitable.add("☀️ 午间户外活动")
            }
            weather.tempMax > 30 -> {
                suitable.add("🧴 注意防晒")
            }
            weather.tempMin <= 0 -> {
                suitable.add("🧣 注意保暖")
                unsuitable.add("🌊 户外长时间停留")
            }
            weather.tempMin <= 5 -> {
                suitable.add("🧥 添衣保暖")
            }
        }

        // ── 基于风速 ──
        if (weather.windSpeed > 39) {
            unsuitable.add("🪁 高空作业")
            unsuitable.add("🚴 骑行出门")
        } else if (weather.windSpeed > 20) {
            unsuitable.add("🪁 放风筝（风太大）")
        }

        // ── 基于温差 ──
        val tempDiff = weather.tempMax - weather.tempMin
        if (tempDiff >= 12) {
            suitable.add("🧥 早晚添衣（温差${tempDiff}°）")
        }

        // ── 基于季节/节气 ──
        when (date.month) {
            Month.MARCH, Month.APRIL -> {
                if (weather.condition == WeatherCondition.SUNNY) suitable.add("🌸 踏青赏花")
            }
            Month.SEPTEMBER, Month.OCTOBER -> {
                if (weather.condition == WeatherCondition.SUNNY) suitable.add("🍂 秋游登高")
            }
            else -> {}
        }

        // ── 洗车建议 ──
        if (weather.condition in listOf(WeatherCondition.SUNNY, WeatherCondition.PARTLY_CLOUDY, WeatherCondition.CLOUDY)
            && weather.precip < 1 && weather.windSpeed < 30
        ) {
            suitable.add("🚗 洗车")
        } else if (weather.condition in listOf(WeatherCondition.RAINY, WeatherCondition.SNOWY)) {
            unsuitable.add("🚗 洗车（白洗）")
        }

        // 确保数量合理
        return DailyAdvice(
            suitable = suitable.distinct().take(5),
            unsuitable = unsuitable.distinct().take(3),
        )
    }
}
