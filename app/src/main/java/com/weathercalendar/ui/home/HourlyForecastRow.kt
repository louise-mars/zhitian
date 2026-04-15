package com.weathercalendar.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathercalendar.data.mock.MockData
import com.weathercalendar.data.model.HourlyForecast
import com.weathercalendar.data.repository.TemperatureUnit
import com.weathercalendar.ui.components.GlassCard
import com.weathercalendar.ui.theme.WeatherCalendarTheme
import com.weathercalendar.util.formatTempShort
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun HourlyForecastRow(
    items: List<HourlyForecast>,
    textColor: Color,
    modifier: Modifier = Modifier,
    tempUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(items, key = { it.time }) { item ->
            HourlyChip(item = item, textColor = textColor, tempUnit = tempUnit)
        }
    }
}

@Composable
private fun HourlyChip(
    item: HourlyForecast,
    textColor: Color,
    tempUnit: TemperatureUnit,
) {
    GlassCard(
        alpha = if (item.isNow) 0.25f else 0.10f,
        cornerRadius = 16.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (item.isNow) "现在" else item.time.format(timeFormatter),
                color = textColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (item.isNow) FontWeight.Bold else FontWeight.Normal,
            )
            Spacer(Modifier.height(6.dp))
            Text(text = item.condition.icon, fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.temperature.formatTempShort(tempUnit),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0288D1)
@Composable
private fun HourlyForecastRowPreview() {
    WeatherCalendarTheme(dynamicColor = false) {
        HourlyForecastRow(items = MockData.hourlyForecast, textColor = Color.White)
    }
}
