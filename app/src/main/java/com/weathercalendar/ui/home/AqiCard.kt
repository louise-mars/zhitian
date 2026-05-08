package com.weathercalendar.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathercalendar.data.model.AirQuality
import com.weathercalendar.ui.components.GlassCard

/**
 * 空气质量卡片 — 显示 AQI 数值、类别、PM2.5 和 PM10。
 */
@Composable
fun AqiCard(
    airQuality: AirQuality?,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        alpha = 0.12f,
        cornerRadius = 20.dp,
        elevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "空气质量",
                color = textColor.copy(alpha = 0.5f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(12.dp))

            if (airQuality == null) {
                Text(
                    text = "暂无数据",
                    color = textColor.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // AQI 数值 + 颜色圆点
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(airQuality.color).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${airQuality.aqi}",
                            color = Color(airQuality.color),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // 类别标签
                    Column {
                        Text(
                            text = airQuality.category,
                            color = textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "AQI ${airQuality.aqi}",
                            color = textColor.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // PM2.5 和 PM10
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PM2.5",
                                color = textColor.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = airQuality.pm2p5,
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PM10",
                                color = textColor.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = airQuality.pm10,
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}
