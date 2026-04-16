package com.weathercalendar.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * 多国节日工具 — 中国、瑞典、美国。
 * 返回节日名称，null 表示非节日。
 */
object Holidays {

    data class HolidayInfo(
        val name: String,
        val country: String, // "CN" / "SE" / "US"
        val emoji: String = "",
    )

    /** 获取指定日期的所有节日 */
    fun getHolidays(date: LocalDate): List<HolidayInfo> {
        return buildList {
            getChineseSolarHoliday(date)?.let { add(it) }
            getSwedishHoliday(date)?.let { add(it) }
            getUSHoliday(date)?.let { add(it) }
        }
    }

    /** 获取指定日期的首个节日名称（用于日历格子显示） */
    fun getFirstHolidayName(date: LocalDate): String? {
        return getHolidays(date).firstOrNull()?.let { "${it.emoji}${it.name}" }
    }

    /** 是否为任何国家的节日 */
    fun isHoliday(date: LocalDate): Boolean = getHolidays(date).isNotEmpty()

    // ─────────────────────────────────────────
    // 中国公历节日
    // ─────────────────────────────────────────

    private fun getChineseSolarHoliday(date: LocalDate): HolidayInfo? {
        val md = date.monthValue * 100 + date.dayOfMonth
        return when (md) {
            101 -> HolidayInfo("元旦", "CN", "🎉")
            214 -> HolidayInfo("情人节", "CN", "💕")
            308 -> HolidayInfo("妇女节", "CN", "👩")
            312 -> HolidayInfo("植树节", "CN", "🌳")
            401 -> HolidayInfo("愚人节", "CN", "🤡")
            501 -> HolidayInfo("劳动节", "CN", "💪")
            504 -> HolidayInfo("青年节", "CN", "🧑")
            601 -> HolidayInfo("儿童节", "CN", "👶")
            701 -> HolidayInfo("建党节", "CN", "🇨🇳")
            801 -> HolidayInfo("建军节", "CN", "🎖️")
            910 -> HolidayInfo("教师节", "CN", "📚")
            1001 -> HolidayInfo("国庆节", "CN", "🇨🇳")
            1225 -> HolidayInfo("圣诞节", "CN", "🎄")
            else -> null
        }
    }

    // ─────────────────────────────────────────
    // 瑞典节日
    // ─────────────────────────────────────────

    private fun getSwedishHoliday(date: LocalDate): HolidayInfo? {
        val md = date.monthValue * 100 + date.dayOfMonth
        val fixed = when (md) {
            101 -> HolidayInfo("Nyårsdagen", "SE", "🎆")        // 新年
            106 -> HolidayInfo("Trettondedag jul", "SE", "⭐")   // 主显节
            501 -> HolidayInfo("Första maj", "SE", "🌷")         // 五一
            606 -> HolidayInfo("Nationaldagen", "SE", "🇸🇪")     // 国庆日
            1224 -> HolidayInfo("Julafton", "SE", "🎄")          // 平安夜
            1225 -> HolidayInfo("Juldagen", "SE", "🎁")          // 圣诞节
            1226 -> HolidayInfo("Annandag jul", "SE", "🎄")      // 圣诞次日
            1231 -> HolidayInfo("Nyårsafton", "SE", "🎆")        // 除夕
            else -> null
        }
        if (fixed != null) return fixed

        // 仲夏节（6月19-25日之间的周六）
        if (date.monthValue == 6 && date.dayOfMonth in 20..26 && date.dayOfWeek == DayOfWeek.SATURDAY) {
            return HolidayInfo("Midsommar", "SE", "🌻")
        }
        // 仲夏节前夜
        if (date.monthValue == 6 && date.dayOfMonth in 19..25 && date.dayOfWeek == DayOfWeek.FRIDAY) {
            return HolidayInfo("Midsommarafton", "SE", "🌻")
        }

        return null
    }

    // ─────────────────────────────────────────
    // 美国节日
    // ─────────────────────────────────────────

    private fun getUSHoliday(date: LocalDate): HolidayInfo? {
        val md = date.monthValue * 100 + date.dayOfMonth
        val fixed = when (md) {
            101 -> HolidayInfo("New Year", "US", "🎆")
            704 -> HolidayInfo("Independence Day", "US", "🇺🇸")
            1111 -> HolidayInfo("Veterans Day", "US", "🎖️")
            1225 -> HolidayInfo("Christmas", "US", "🎄")
            else -> null
        }
        if (fixed != null) return fixed

        val year = date.year

        // MLK Day: 1月第3个周一
        if (date == nthWeekday(year, 1, DayOfWeek.MONDAY, 3)) {
            return HolidayInfo("MLK Day", "US", "✊")
        }
        // Presidents' Day: 2月第3个周一
        if (date == nthWeekday(year, 2, DayOfWeek.MONDAY, 3)) {
            return HolidayInfo("Presidents' Day", "US", "🏛️")
        }
        // Memorial Day: 5月最后一个周一
        if (date == lastWeekday(year, 5, DayOfWeek.MONDAY)) {
            return HolidayInfo("Memorial Day", "US", "🇺🇸")
        }
        // Labor Day: 9月第1个周一
        if (date == nthWeekday(year, 9, DayOfWeek.MONDAY, 1)) {
            return HolidayInfo("Labor Day", "US", "💪")
        }
        // Columbus Day: 10月第2个周一
        if (date == nthWeekday(year, 10, DayOfWeek.MONDAY, 2)) {
            return HolidayInfo("Columbus Day", "US", "🚢")
        }
        // Thanksgiving: 11月第4个周四
        if (date == nthWeekday(year, 11, DayOfWeek.THURSDAY, 4)) {
            return HolidayInfo("Thanksgiving", "US", "🦃")
        }

        return null
    }

    // ─────────────────────────────────────────
    // 工具函数
    // ─────────────────────────────────────────

    /** 某月第 N 个指定星期几 */
    private fun nthWeekday(year: Int, month: Int, dayOfWeek: DayOfWeek, n: Int): LocalDate {
        val first = LocalDate.of(year, month, 1)
            .with(TemporalAdjusters.firstInMonth(dayOfWeek))
        return first.plusWeeks((n - 1).toLong())
    }

    /** 某月最后一个指定星期几 */
    private fun lastWeekday(year: Int, month: Int, dayOfWeek: DayOfWeek): LocalDate {
        return LocalDate.of(year, month, 1)
            .with(TemporalAdjusters.lastInMonth(dayOfWeek))
    }
}
