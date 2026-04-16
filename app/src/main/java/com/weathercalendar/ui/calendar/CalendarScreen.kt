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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathercalendar.data.mock.MockData
import com.weathercalendar.data.model.CalendarDayCell
import com.weathercalendar.data.model.CalendarEvent
import com.weathercalendar.data.model.WeatherCondition
import com.weathercalendar.ui.components.GlassCard
import com.weathercalendar.ui.components.WeatherAnimationOverlay
import com.weathercalendar.ui.theme.WeatherCalendarTheme
import com.weathercalendar.ui.theme.WeatherColors
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

    // 天气联动
    val selectedCondition = selectedDate?.let { todayWeather?.get(it)?.first } ?: WeatherCondition.SUNNY
    val gradient = WeatherColors.calendarGradientFor(selectedCondition)
    val animatedStart by animateColorAsState(gradient.start, tween(600), label = "s")
    val animatedEnd by animateColorAsState(gradient.end, tween(600), label = "e")
    val animatedBottom by animateColorAsState(
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
            .background(Brush.verticalGradient(listOf(animatedStart, animatedEnd, animatedBottom))),
    ) {
        WeatherAnimationOverlay(condition = selectedCondition, isDay = true)

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            CalendarTopBar(monthLabel, onBack, {
                swipeDirection = -1; selectedDate = null; onPrevMonth()
            }, {
                swipeDirection = 1; selectedDate = null; onNextMonth()
            })

            Spacer(Modifier.height(12.dp))
            WeekdayHeader()
            Spacer(Modifier.height(8.dp))

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
        }

        // 底部浮层 — 使用实时的 todayEvents 数据
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
                    weatherCondition = todayWeather?.get(selectedDate)?.first,
                    tempRange = todayWeather?.get(selectedDate)?.second,
                    events = currentEvents,
                    onAddEvent = { title, time ->
                        onAddEvent(selectedDate!!, title, time)
                    },
                    onDeleteEvent = onDeleteEvent,
                    onUpdateEvent = { id, title, time ->
                        onUpdateEvent(id, selectedDate!!, title, time)
                    },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────

@Composable
private fun CalendarTopBar(
    monthLabel: String,
    onBack: () -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onPrevMonth) {
            Icon(Icons.Default.ChevronLeft, "上月", tint = Color.White)
        }
        Text(monthLabel, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.ChevronRight, "下月", tint = Color.White)
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun WeekdayHeader() {
    val weekdays = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
    )
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        weekdays.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.CHINESE),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.5f),
            )
        }
    }
}

// ─────────────────────────────────────────────
// 月视图网格 — 更紧凑的间距
// ─────────────────────────────────────────────

@Composable
private fun MonthGrid(
    days: List<CalendarDayCell>,
    firstDayOfWeek: DayOfWeek,
    today: LocalDate,
    selectedDate: LocalDate?,
    weatherMap: Map<LocalDate, Pair<WeatherCondition, Pair<Int, Int>>>?,
    onDateClick: (LocalDate) -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
) {
    if (days.isEmpty()) return
    val startOffset = (firstDayOfWeek.value - 1)
    val totalCells = startOffset + days.size
    val rows = (totalCells + 6) / 7
    var dragAccumulator by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onDragEnd = {
                        if (dragAccumulator > 100f) onSwipeRight()
                        else if (dragAccumulator < -100f) onSwipeLeft()
                        dragAccumulator = 0f
                    },
                    onDragCancel = { dragAccumulator = 0f },
                    onHorizontalDrag = { _, d -> dragAccumulator += d },
                )
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (col in 0..6) {
                    val idx = row * 7 + col - startOffset
                    if (idx in days.indices) {
                        DayCellView(
                            cell = days[idx],
                            isToday = days[idx].date == today,
                            isSelected = days[idx].date == selectedDate,
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
// 日期格子 — 更紧凑
// ─────────────────────────────────────────────

@Composable
private fun DayCellView(
    cell: CalendarDayCell,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        if (isSelected) 1.08f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label = "s",
    )
    val bg = when {
        isToday && isSelected -> Color.White
        isToday -> Color.White
        isSelected -> Color(0xFF4FC3F7).copy(alpha = 0.3f)
        else -> Color.Transparent
    }
    val textColor = if (isToday) Color(0xFF1A2332) else Color.White.copy(alpha = 0.9f)

    Column(
        modifier = modifier
            .scale(scale)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp).clip(CircleShape).background(bg),
        ) {
            Text(
                "${cell.date.dayOfMonth}",
                fontSize = 15.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
            )
        }
        // 节气/节日 或 天气图标
        when {
            cell.isLunarFestival -> Text(cell.lunarText, fontSize = 9.sp, color = WeatherColors.LunarFestival, maxLines = 1, fontWeight = FontWeight.Medium)
            cell.weatherIcon != null -> Text(cell.weatherIcon, fontSize = 10.sp)
            else -> Spacer(Modifier.height(12.dp))
        }
        // 事件圆点
        if (cell.hasEvents) {
            Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF4FC3F7)))
        }
    }
}

// ─────────────────────────────────────────────
// 底部浮层 — 事件增删改
// ─────────────────────────────────────────────

@Composable
private fun DayDetailContent(
    date: LocalDate,
    lunarText: String,
    isLunarFestival: Boolean,
    weatherCondition: WeatherCondition?,
    tempRange: Pair<Int, Int>?,
    events: List<CalendarEvent>,
    onAddEvent: (String, java.time.LocalTime?) -> Unit,
    onDeleteEvent: (Long) -> Unit,
    onUpdateEvent: (Long, String, java.time.LocalTime?) -> Unit,
) {
    var showAddForm by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var editingTitle by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        // 日期标题
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
                fontWeight = if (isLunarFestival) FontWeight.Medium else FontWeight.Normal,
            )
        }

        // 天气卡
        if (weatherCondition != null && tempRange != null) {
            Spacer(Modifier.height(14.dp))
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
            Text("+ 添加", fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = Color(0xFF4FC3F7),
                modifier = Modifier.clickable { showAddForm = !showAddForm; newTitle = "" })
        }
        Spacer(Modifier.height(8.dp))

        // 添加表单（展开在事件列表上方）
        if (showAddForm) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    placeholder = { Text("输入事项名称", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    singleLine = true,
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
                        if (newTitle.isNotBlank()) {
                            onAddEvent(newTitle.trim(), null)
                            newTitle = ""
                            showAddForm = false
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
                            value = editingTitle,
                            onValueChange = { editingTitle = it },
                            modifier = Modifier.weight(1f).height(48.dp),
                            singleLine = true,
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
                                if (editingTitle.isNotBlank()) {
                                    onUpdateEvent(event.id, editingTitle.trim(), event.time)
                                    editingId = null
                                }
                            })
                        Spacer(Modifier.width(4.dp))
                        Text("✕", fontSize = 14.sp, color = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.clickable { editingId = null })
                    } else {
                        Text(
                            event.title, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                            color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).clickable {
                                editingId = event.id; editingTitle = event.title
                            },
                        )
                        // 删除按钮
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
    val weather = mapOf(today to (WeatherCondition.SUNNY to (8 to 23)))
    WeatherCalendarTheme(dynamicColor = false) {
        CalendarScreen(
            monthLabel = "2026年4月", days = mockDays, firstDayOfWeek = DayOfWeek.WEDNESDAY,
            todayEvents = events, todayWeather = weather,
        )
    }
}
