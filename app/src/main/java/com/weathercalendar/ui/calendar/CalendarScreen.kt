package com.weathercalendar.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathercalendar.data.mock.MockData
import com.weathercalendar.data.model.CalendarDayCell
import com.weathercalendar.data.model.CalendarEvent
import com.weathercalendar.data.model.WeatherCondition
import com.weathercalendar.ui.theme.WeatherCalendarTheme
import com.weathercalendar.ui.theme.WeatherColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * 日历月视图页面。
 * 天气动态背景（降低饱和度），日期格子带天气图标+农历+事件圆点。
 * 点击日期弹出 BottomSheet 显示详情。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    monthLabel: String,
    days: List<CalendarDayCell>,
    firstDayOfWeek: DayOfWeek,
    todayEvents: Map<LocalDate, List<CalendarEvent>>,
    todayWeather: Map<LocalDate, Pair<WeatherCondition, Pair<Int, Int>>>?,
    onBack: () -> Unit = {},
    onPrevMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
) {
    // 降低饱和度的天气背景
    val gradient = WeatherColors.calendarGradientFor(WeatherCondition.SUNNY)

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(gradient.start, gradient.end)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // ── 顶部栏 ──
            CalendarTopBar(
                monthLabel = monthLabel,
                onBack = onBack,
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
            )

            Spacer(Modifier.height(8.dp))

            // ── 星期标题行 ──
            WeekdayHeader()

            Spacer(Modifier.height(4.dp))

            // ── 月视图网格 ──
            MonthGrid(
                days = days,
                firstDayOfWeek = firstDayOfWeek,
                today = LocalDate.now(),
                selectedDate = selectedDate,
                onDateClick = { date ->
                    selectedDate = date
                },
            )
        }

        // ── BottomSheet: 日期详情 ──
        if (selectedDate != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedDate = null },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            ) {
                DayDetailContent(
                    date = selectedDate!!,
                    lunarText = days.find { it.date == selectedDate }?.lunarText ?: "",
                    weatherCondition = todayWeather?.get(selectedDate)?.first,
                    tempRange = todayWeather?.get(selectedDate)?.second,
                    events = todayEvents[selectedDate] ?: emptyList(),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// 顶部栏
// ─────────────────────────────────────────────

@Composable
private fun CalendarTopBar(
    monthLabel: String,
    onBack: () -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color.White,
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onPrevMonth) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "上月", tint = Color.White)
        }
        Text(
            text = monthLabel,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.ChevronRight, contentDescription = "下月", tint = Color.White)
        }
        Spacer(Modifier.weight(1f))
        // 占位，保持居中
        Spacer(Modifier.size(48.dp))
    }
}

// ─────────────────────────────────────────────
// 星期标题
// ─────────────────────────────────────────────

@Composable
private fun WeekdayHeader() {
    val weekdays = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        weekdays.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.CHINESE),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

// ─────────────────────────────────────────────
// 月视图网格
// ─────────────────────────────────────────────

@Composable
private fun MonthGrid(
    days: List<CalendarDayCell>,
    firstDayOfWeek: DayOfWeek,
    today: LocalDate,
    selectedDate: LocalDate?,
    onDateClick: (LocalDate) -> Unit,
) {
    if (days.isEmpty()) return

    val startOffset = (firstDayOfWeek.value - 1) // 周一=0 偏移
    val totalCells = startOffset + days.size
    val rows = (totalCells + 6) / 7

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col - startOffset
                    if (cellIndex in days.indices) {
                        val cell = days[cellIndex]
                        DayCellView(
                            cell = cell,
                            isToday = cell.date == today,
                            isSelected = cell.date == selectedDate,
                            onClick = { onDateClick(cell.date) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCellView(
    cell: CalendarDayCell,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = when {
        isSelected -> Color.White.copy(alpha = 0.25f)
        isToday -> Color.White.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val lunarColor = when {
        cell.isLunarFestival -> WeatherColors.LunarFestival
        else -> Color.White.copy(alpha = 0.5f)
    }

    Column(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 日期数字
        Text(
            text = "${cell.date.dayOfMonth}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = Color.White,
        )

        // 农历
        Text(
            text = cell.lunarText,
            style = MaterialTheme.typography.labelSmall,
            color = lunarColor,
            maxLines = 1,
        )

        // 天气图标（有预报时）
        if (cell.weatherIcon != null) {
            Text(text = cell.weatherIcon, fontSize = 12.sp)
        } else {
            Spacer(Modifier.height(14.dp))
        }

        // 事件圆点
        if (cell.hasEvents) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f)),
            )
        } else {
            Spacer(Modifier.height(5.dp))
        }
    }
}

// ─────────────────────────────────────────────
// BottomSheet 日期详情
// ─────────────────────────────────────────────

@Composable
private fun DayDetailContent(
    date: LocalDate,
    lunarText: String,
    weatherCondition: WeatherCondition?,
    tempRange: Pair<Int, Int>?,
    events: List<CalendarEvent>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        // 日期
        Text(
            text = date.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINESE)),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        if (lunarText.isNotEmpty()) {
            Text(
                text = "农历 $lunarText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 天气
        if (weatherCondition != null && tempRange != null) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = weatherCondition.icon, fontSize = 28.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "${weatherCondition.label}  ${tempRange.first}° ~ ${tempRange.second}°",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = weatherCondition.tip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // 事件
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        if (events.isNotEmpty()) {
            events.forEach { event ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(event.color)),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = event.time?.format(DateTimeFormatter.ofPattern("HH:mm"))
                                ?: "全天",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            Text(
                text = "暂无日程",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CalendarScreenPreview() {
    val mockDays = MockData.calendarMonth()
    val today = LocalDate.of(2026, 4, 16)
    val events = mapOf(
        today to listOf(
            CalendarEvent(1, "Team Meeting", today, java.time.LocalTime.of(10, 0), 0xFF2196F3),
            CalendarEvent(2, "Gym", today, java.time.LocalTime.of(18, 0), 0xFF4CAF50),
        ),
    )
    val weather = mapOf(
        today to (WeatherCondition.SUNNY to (8 to 23)),
        today.plusDays(1) to (WeatherCondition.RAINY to (6 to 12)),
    )

    WeatherCalendarTheme(dynamicColor = false) {
        CalendarScreen(
            monthLabel = "2026年4月",
            days = mockDays,
            firstDayOfWeek = DayOfWeek.WEDNESDAY, // 4月1日是周三
            todayEvents = events,
            todayWeather = weather,
        )
    }
}
