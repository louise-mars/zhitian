package com.weathercalendar.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathercalendar.data.model.CalendarDayCell
import com.weathercalendar.data.model.CalendarEvent
import com.weathercalendar.data.model.WeatherCondition
import com.weathercalendar.ui.components.GlassCard
import com.weathercalendar.ui.components.WeatherAnimationOverlay
import com.weathercalendar.ui.theme.WeatherColors
import com.weathercalendar.util.Holidays
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

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
    onAddEvent: (LocalDate, String, java.time.LocalTime?) -> Unit = { _, _, _ -> },
    onDeleteEvent: (Long) -> Unit = {},
    onUpdateEvent: (Long, LocalDate, String, java.time.LocalTime?) -> Unit = { _, _, _, _ -> },
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var swipeDirection by remember { mutableIntStateOf(0) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    // 自动消失反馈
    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            kotlinx.coroutines.delay(1500)
            feedbackMessage = null
        }
    }

    val selectedCondition = selectedDate?.let { todayWeather?.get(it)?.first } ?: WeatherCondition.SUNNY
    val gradient = WeatherColors.calendarGradientFor(selectedCondition)
    val animStart by animateColorAsState(gradient.start, tween(600), label = "s")
    val animEnd by animateColorAsState(gradient.end, tween(600), label = "e")
    val animBottom by animateColorAsState(
        when (selectedCondition) {
            WeatherCondition.RAINY, WeatherCondition.DRIZZLE, WeatherCondition.STORMY -> Color(0xFF1A2332)
            WeatherCondition.SNOWY -> Color(0xFF2A3545)
            WeatherCondition.CLOUDY -> Color(0xFF1E2A38)
            else -> Color(0xFF0D1B2A)
        }, tween(600), label = "b",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(animStart, animEnd, animBottom))),
    ) {
        WeatherAnimationOverlay(condition = selectedCondition, isDay = true)

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // 顶部导航
            CalendarTopBar(monthLabel, onBack,
                { swipeDirection = -1; selectedDate = null; onPrevMonth() },
                { swipeDirection = 1; selectedDate = null; onNextMonth() },
            )

            Spacer(Modifier.height(8.dp))

            // 星期标题（万年历风格：周末红色）
            WeekdayHeader()

            Spacer(Modifier.height(6.dp))

            // 月视图网格
            AnimatedContent(
                targetState = monthLabel,
                transitionSpec = {
                    if (swipeDirection >= 0) slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    else slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                },
                label = "month",
            ) { _ ->
                MonthGrid(days, firstDayOfWeek, LocalDate.now(), selectedDate, todayWeather,
                    onDateClick = { selectedDate = it },
                    onSwipeLeft = { swipeDirection = 1; selectedDate = null; onNextMonth() },
                    onSwipeRight = { swipeDirection = -1; selectedDate = null; onPrevMonth() },
                )
            }

            // 选中日期的简要信息（万年历风格：网格下方固定显示）
            Spacer(Modifier.height(12.dp))
            SelectedDateSummary(
                selectedDate = selectedDate ?: LocalDate.now(),
                days = days,
                todayWeather = todayWeather,
                todayEvents = todayEvents,
            )
        }

        // 操作反馈提示
        if (feedbackMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    feedbackMessage!!,
                    modifier = Modifier
                        .padding(top = 60.dp)
                        .background(Color(0xFF2E7D32).copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // 右下角浮动添加按钮
        Box(
            modifier = Modifier.fillMaxSize().padding(20.dp).statusBarsPadding(),
            contentAlignment = Alignment.BottomEnd,
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4FC3F7))
                    .clickable { selectedDate = selectedDate ?: LocalDate.now() },
                contentAlignment = Alignment.Center,
            ) {
                Text("+", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // 底部浮层
        if (selectedDate != null) {
            val currentEvents = todayEvents[selectedDate] ?: emptyList()
            ModalBottomSheet(
                onDismissRequest = { selectedDate = null },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                containerColor = Color(0xFF1E2A3A).copy(alpha = 0.97f),
                contentColor = Color.White,
            ) {
                DayDetailContent(
                    date = selectedDate!!,
                    lunarText = days.find { it.date == selectedDate }?.lunarText ?: "",
                    isLunarFestival = days.find { it.date == selectedDate }?.isLunarFestival == true,
                    holidays = Holidays.getHolidays(selectedDate!!),
                    weatherCondition = todayWeather?.get(selectedDate)?.first,
                    tempRange = todayWeather?.get(selectedDate)?.second,
                    events = currentEvents,
                    onAddEvent = { title, time ->
                        onAddEvent(selectedDate!!, title, time)
                        feedbackMessage = "✓ 已添加"
                    },
                    onDeleteEvent = { id ->
                        onDeleteEvent(id)
                        feedbackMessage = "✓ 已删除"
                    },
                    onUpdateEvent = { id, title, time ->
                        onUpdateEvent(id, selectedDate!!, title, time)
                        feedbackMessage = "✓ 已更新"
                    },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// 选中日期简要信息（网格下方固定区域）
// ─────────────────────────────────────────────

@Composable
private fun SelectedDateSummary(
    selectedDate: LocalDate,
    days: List<CalendarDayCell>,
    todayWeather: Map<LocalDate, Pair<WeatherCondition, Pair<Int, Int>>>?,
    todayEvents: Map<LocalDate, List<CalendarEvent>>,
) {
    val cell = days.find { it.date == selectedDate }
    val weather = todayWeather?.get(selectedDate)
    val events = todayEvents[selectedDate] ?: emptyList()
    val holidays = Holidays.getHolidays(selectedDate)

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        alpha = 0.12f, cornerRadius = 16.dp, elevation = 4.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            // 日期行
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    selectedDate.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINESE)),
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White,
                )
                if (cell?.lunarText != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(cell.lunarText, fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                }
            }

            // 节日行
            if (holidays.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    holidays.joinToString("  ") { "${it.emoji}${it.name}" },
                    fontSize = 12.sp, color = Color(0xFFFFD54F),
                )
            }

            // 天气行
            if (weather != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "${weather.first.icon} ${weather.first.label} ${weather.second.first}°~${weather.second.second}°",
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f),
                )
            }

            // 事件行
            if (events.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                events.take(2).forEach { event ->
                    Text(
                        "📅 ${event.time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "全天"} ${event.title}",
                        fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 1,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 顶部导航
// ─────────────────────────────────────────────

@Composable
private fun CalendarTopBar(
    monthLabel: String, onBack: () -> Unit, onPrev: () -> Unit, onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onPrev) { Icon(Icons.Default.ChevronLeft, "上月", tint = Color.White) }
        Text(monthLabel, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, "下月", tint = Color.White) }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(48.dp))
    }
}

// ─────────────────────────────────────────────
// 星期标题（万年历风格：周末红色/蓝色）
// ─────────────────────────────────────────────

@Composable
private fun WeekdayHeader() {
    val weekdays = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
    )
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        weekdays.forEach { day ->
            val color = when (day) {
                DayOfWeek.SATURDAY -> Color(0xFF4FC3F7)  // 蓝色
                DayOfWeek.SUNDAY -> Color(0xFFEF5350)    // 红色
                else -> Color.White.copy(alpha = 0.5f)
            }
            Text(
                day.getDisplayName(TextStyle.SHORT, Locale.CHINESE),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color,
            )
        }
    }
}

// ─────────────────────────────────────────────
// 月视图网格（万年历风格）
// ─────────────────────────────────────────────

@Composable
private fun MonthGrid(
    days: List<CalendarDayCell>, firstDayOfWeek: DayOfWeek, today: LocalDate,
    selectedDate: LocalDate?,
    weatherMap: Map<LocalDate, Pair<WeatherCondition, Pair<Int, Int>>>?,
    onDateClick: (LocalDate) -> Unit, onSwipeLeft: () -> Unit, onSwipeRight: () -> Unit,
) {
    if (days.isEmpty()) return
    val startOffset = (firstDayOfWeek.value - 1)
    val rows = (startOffset + days.size + 6) / 7
    var drag by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onDragEnd = {
                        if (drag > 100f) onSwipeRight() else if (drag < -100f) onSwipeLeft()
                        drag = 0f
                    },
                    onDragCancel = { drag = 0f },
                    onHorizontalDrag = { _, d -> drag += d },
                )
            },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (col in 0..6) {
                    val idx = row * 7 + col - startOffset
                    if (idx in days.indices) {
                        DayCellView(
                            cell = days[idx],
                            isToday = days[idx].date == today,
                            isSelected = days[idx].date == selectedDate,
                            isWeekend = col >= 5,
                            onClick = { onDateClick(days[idx].date) },
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

// ─────────────────────────────────────────────
// 日期格子（万年历风格：公历大 + 农历/节日小）
// ─────────────────────────────────────────────

@Composable
private fun DayCellView(
    cell: CalendarDayCell, isToday: Boolean, isSelected: Boolean,
    isWeekend: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        if (isSelected) 1.05f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh), label = "s",
    )

    // 背景
    val bg = when {
        isToday && isSelected -> Color(0xFFEF5350)  // 万年历风格：今天红色
        isToday -> Color(0xFFEF5350)
        isSelected -> Color.White.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    // 公历数字颜色
    val numColor = when {
        isToday -> Color.White
        isWeekend -> Color(0xFFEF5350).copy(alpha = 0.8f)  // 周末红色
        else -> Color.White.copy(alpha = 0.9f)
    }

    // 副文字（节日 > 节气 > 农历）
    val hasHoliday = cell.holidayName != null
    val hasLunarFestival = cell.isLunarFestival
    val subText = cell.holidayName?.take(4) ?: cell.lunarText
    val subColor = when {
        hasHoliday -> Color(0xFFFFD54F)          // 节日金色
        hasLunarFestival -> WeatherColors.LunarFestival  // 节气红色
        else -> Color.White.copy(alpha = 0.4f)   // 普通农历灰色
    }

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 公历数字
        Text(
            "${cell.date.dayOfMonth}",
            fontSize = 16.sp,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            color = numColor,
        )
        // 农历/节日（小字）
        Text(
            subText,
            fontSize = 9.sp,
            color = subColor,
            maxLines = 1,
            fontWeight = if (hasHoliday || hasLunarFestival) FontWeight.Medium else FontWeight.Normal,
        )
        // 事件圆点 + 天气小图标
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (cell.hasEvents) {
                Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF4FC3F7)))
                Spacer(Modifier.width(2.dp))
            }
            if (cell.weatherIcon != null) {
                Text(cell.weatherIcon, fontSize = 9.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────
// 底部浮层 — 事件增删改 + 多国节日
// ─────────────────────────────────────────────

@Composable
private fun DayDetailContent(
    date: LocalDate, lunarText: String, isLunarFestival: Boolean,
    holidays: List<Holidays.HolidayInfo>,
    weatherCondition: WeatherCondition?, tempRange: Pair<Int, Int>?,
    events: List<CalendarEvent>,
    onAddEvent: (String, java.time.LocalTime?) -> Unit,
    onDeleteEvent: (Long) -> Unit,
    onUpdateEvent: (Long, String, java.time.LocalTime?) -> Unit,
) {
    var showAddForm by remember { mutableStateOf(events.isEmpty()) }
    var newTitle by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var editingTitle by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        // 日期
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                date.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINESE)),
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE),
                fontSize = 15.sp, color = Color.White.copy(alpha = 0.6f),
            )
        }
        if (lunarText.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "农历 $lunarText", fontSize = 13.sp,
                color = if (isLunarFestival) WeatherColors.LunarFestival else Color.White.copy(alpha = 0.5f),
            )
        }

        // 多国节日
        if (holidays.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            holidays.forEach { h ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("${h.emoji} ", fontSize = 14.sp)
                    Text(h.name, fontSize = 14.sp, color = Color(0xFFFFD54F), fontWeight = FontWeight.Medium)
                    Text(
                        when (h.country) { "CN" -> " 🇨🇳"; "SE" -> " 🇸🇪"; "US" -> " 🇺🇸"; else -> "" },
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // 天气
        if (weatherCondition != null && tempRange != null) {
            Spacer(Modifier.height(12.dp))
            GlassCard(alpha = 0.12f, cornerRadius = 16.dp, elevation = 4.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(weatherCondition.icon, fontSize = 28.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("${weatherCondition.label}  ${tempRange.first}° ~ ${tempRange.second}°",
                            fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        Text(weatherCondition.tip, fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        Spacer(Modifier.height(10.dp))

        // 日程标题 + 添加按钮
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("日程", fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.4f), modifier = Modifier.weight(1f))
            Text(
                if (showAddForm) "收起" else "+ 添加",
                fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4FC3F7),
                modifier = Modifier.clickable { showAddForm = !showAddForm; newTitle = "" },
            )
        }
        Spacer(Modifier.height(8.dp))

        // 添加表单
        if (showAddForm) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newTitle, onValueChange = { newTitle = it },
                    placeholder = { Text("输入事项名称", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp) },
                    modifier = Modifier.weight(1f).height(52.dp), singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4FC3F7), unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        cursorColor = Color(0xFF4FC3F7),
                    ),
                )
                Spacer(Modifier.width(8.dp))
                Text("确定", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4FC3F7),
                    modifier = Modifier.clickable {
                        val trimmed = newTitle.trim()
                        if (trimmed.length in 1..50) {
                            onAddEvent(trimmed, null); newTitle = ""; showAddForm = false
                        }
                    })
            }
            Spacer(Modifier.height(10.dp))
        }

        // 事件列表
        if (events.isNotEmpty()) {
            events.forEach { event ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(event.color)))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        event.time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "全天",
                        fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.width(10.dp))
                    if (editingId == event.id) {
                        OutlinedTextField(
                            value = editingTitle, onValueChange = { editingTitle = it },
                            modifier = Modifier.weight(1f).height(48.dp), singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF4FC3F7), unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                cursorColor = Color(0xFF4FC3F7),
                            ),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("✓", fontSize = 16.sp, color = Color(0xFF4FC3F7), fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                if (editingTitle.isNotBlank()) { onUpdateEvent(event.id, editingTitle.trim(), event.time); editingId = null }
                            })
                        Spacer(Modifier.width(4.dp))
                        Text("✕", fontSize = 14.sp, color = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.clickable { editingId = null })
                    } else {
                        Text(event.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).clickable { editingId = event.id; editingTitle = event.title })
                        Text("✕", fontSize = 13.sp, color = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.clickable { onDeleteEvent(event.id) }.padding(start = 8.dp))
                    }
                }
            }
        } else if (!showAddForm) {
            Text("暂无日程，点击上方添加", fontSize = 13.sp, color = Color.White.copy(alpha = 0.3f))
        }
    }
}
