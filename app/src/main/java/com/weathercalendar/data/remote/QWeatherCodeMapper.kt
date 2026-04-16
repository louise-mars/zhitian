package com.weathercalendar.data.remote

import com.weathercalendar.data.model.WeatherCondition

/**
 * 和风天气图标代码 → WeatherCondition 映射。
 * 文档: https://dev.qweather.com/docs/resource/icons/
 */
object QWeatherCodeMapper {

    fun fromIconCode(code: String): WeatherCondition {
        val c = code.toIntOrNull() ?: return WeatherCondition.SUNNY
        return when (c) {
            100, 150 -> WeatherCondition.SUNNY           // 晴
            101, 102, 103, 151, 152, 153 -> WeatherCondition.PARTLY_CLOUDY // 多云
            104, 154 -> WeatherCondition.CLOUDY           // 阴
            300, 301, 305, 309, 314, 399 -> WeatherCondition.DRIZZLE // 小雨
            302, 303, 304, 306, 307, 308, 310, 311, 312, 313, 315, 316, 317, 318 ->
                WeatherCondition.RAINY                    // 中雨/大雨
            400, 401, 402, 403, 404, 405, 406, 407, 408, 409, 410, 456, 457, 499 ->
                WeatherCondition.SNOWY                    // 雪
            500, 501, 502, 503, 504, 507, 508, 509, 510, 511, 512, 513, 514, 515 ->
                WeatherCondition.FOGGY                    // 雾/霾
            in 200..299 -> WeatherCondition.STORMY        // 雷暴
            else -> WeatherCondition.CLOUDY
        }
    }
}
