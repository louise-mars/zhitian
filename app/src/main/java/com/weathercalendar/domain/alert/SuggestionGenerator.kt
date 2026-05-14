package com.weathercalendar.domain.alert

/**
 * 根据恶劣天气条件生成行动建议。
 * 当多个条件同时存在时，选择最高优先级的建议。
 */
object SuggestionGenerator {

    /**
     * 根据恶劣天气条件列表生成建议文本。
     * @param conditions 当天的所有恶劣天气条件
     * @return 最高优先级条件对应的建议（≤30 字符）
     */
    fun getSuggestion(conditions: List<BadWeatherCondition>): String {
        if (conditions.isEmpty()) return ""
        val highest = conditions.minByOrNull { it.priority } ?: return ""
        return mapToSuggestion(highest)
    }

    private fun mapToSuggestion(condition: BadWeatherCondition): String = when (condition) {
        is BadWeatherCondition.Stormy -> "建议改期或选择室内场所"
        is BadWeatherCondition.Snowy -> "注意保暖和路面结冰"
        is BadWeatherCondition.Rainy -> "建议带伞或调整出行时间"
        is BadWeatherCondition.StrongWind -> "注意防风，避免高空作业或户外活动"
        is BadWeatherCondition.ExtremeHeat -> "注意防暑降温，避免长时间户外活动"
        is BadWeatherCondition.ExtremeCold -> "注意防寒保暖"
    }
}
