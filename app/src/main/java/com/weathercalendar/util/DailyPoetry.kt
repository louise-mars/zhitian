package com.weathercalendar.util

import android.content.Context
import com.weathercalendar.data.model.WeatherCondition
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.Month

/**
 * 每日诗词 — 从 assets/poetry/ JSON 文件加载，支持 1200+ 首。
 *
 * 架构：
 * - 首次调用时从 assets 加载 JSON 到内存（懒加载）
 * - 按天气条件 + 季节双池合并
 * - 基于日期的确定性选择算法，保证同一天同一天气返回相同诗词
 * - 一周内不重复
 *
 * 扩展方式：直接往 assets/poetry/ 目录的 JSON 文件中添加条目即可。
 */
object DailyPoetry {

    data class Poetry(
        val verse: String,      // 精华一句
        val source: String,     // 出处（诗名·作者）
        val fullText: String = "",  // 完整诗词（空则不可展开）
    )

    @Serializable
    private data class PoetryJson(
        val verse: String,
        val source: String,
        val fullText: String = "",
    )

    private val json = Json { ignoreUnknownKeys = true }

    // 懒加载缓存
    private var sunnyPoems: List<Poetry>? = null
    private var rainPoems: List<Poetry>? = null
    private var snowPoems: List<Poetry>? = null
    private var cloudyPoems: List<Poetry>? = null
    private var stormPoems: List<Poetry>? = null
    private var fogPoems: List<Poetry>? = null
    private var springPoems: List<Poetry>? = null
    private var summerPoems: List<Poetry>? = null
    private var autumnPoems: List<Poetry>? = null
    private var winterPoems: List<Poetry>? = null

    private var initialized = false
    private var appContext: Context? = null

    /**
     * 初始化（在 Application.onCreate 中调用一次）。
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * 获取今日诗词。
     * @param date 日期
     * @param condition 天气条件
     * @return 匹配的诗词（verse + source + fullText）
     */
    fun getPoetry(date: LocalDate, condition: WeatherCondition): Poetry {
        ensureLoaded()
        val pool = getPoolForCondition(condition) + getPoolForSeason(date)
        if (pool.isEmpty()) return Poetry("春风又绿江南岸，明月何时照我还", "泊船瓜洲·王安石")

        // 确定性选择算法：基于日期，保证同一天返回相同诗词，一周内不重复
        val weekNumber = date.toEpochDay() / 7
        val dayInWeek = date.dayOfWeek.value  // 1-7
        val baseOffset = ((weekNumber * 13 + condition.ordinal * 7) % pool.size).toInt()
            .let { if (it < 0) it + pool.size else it }
        val step = (pool.size / 8).coerceAtLeast(1)
        val index = (baseOffset + dayInWeek * step) % pool.size
        return pool[index]
    }

    private fun getPoolForCondition(condition: WeatherCondition): List<Poetry> {
        return when (condition) {
            WeatherCondition.SUNNY, WeatherCondition.PARTLY_CLOUDY -> sunnyPoems ?: emptyList()
            WeatherCondition.CLOUDY -> cloudyPoems ?: emptyList()
            WeatherCondition.RAINY, WeatherCondition.DRIZZLE -> rainPoems ?: emptyList()
            WeatherCondition.SNOWY -> snowPoems ?: emptyList()
            WeatherCondition.STORMY -> stormPoems ?: emptyList()
            WeatherCondition.FOGGY -> fogPoems ?: emptyList()
        }
    }

    private fun getPoolForSeason(date: LocalDate): List<Poetry> {
        return when (date.month) {
            Month.MARCH, Month.APRIL, Month.MAY -> springPoems ?: emptyList()
            Month.JUNE, Month.JULY, Month.AUGUST -> summerPoems ?: emptyList()
            Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER -> autumnPoems ?: emptyList()
            else -> winterPoems ?: emptyList()
        }
    }

    private fun ensureLoaded() {
        if (initialized) return
        val ctx = appContext ?: return
        try {
            sunnyPoems = loadFromAsset(ctx, "poetry/sunny.json")
            rainPoems = loadFromAsset(ctx, "poetry/rain.json")
            snowPoems = loadFromAsset(ctx, "poetry/snow.json")
            cloudyPoems = loadFromAsset(ctx, "poetry/cloudy.json")
            stormPoems = loadFromAsset(ctx, "poetry/storm.json")
            fogPoems = loadFromAsset(ctx, "poetry/fog.json")
            springPoems = loadFromAsset(ctx, "poetry/spring.json")
            summerPoems = loadFromAsset(ctx, "poetry/summer.json")
            autumnPoems = loadFromAsset(ctx, "poetry/autumn.json")
            winterPoems = loadFromAsset(ctx, "poetry/winter.json")
            initialized = true
        } catch (e: Exception) {
            android.util.Log.e("DailyPoetry", "加载诗词 JSON 失败", e)
            // Fallback: 使用内置最小集
            sunnyPoems = FALLBACK_POEMS
            rainPoems = FALLBACK_POEMS
            snowPoems = FALLBACK_POEMS
            cloudyPoems = FALLBACK_POEMS
            stormPoems = FALLBACK_POEMS
            fogPoems = FALLBACK_POEMS
            springPoems = FALLBACK_POEMS
            summerPoems = FALLBACK_POEMS
            autumnPoems = FALLBACK_POEMS
            winterPoems = FALLBACK_POEMS
            initialized = true
        }
    }

    private fun loadFromAsset(context: Context, path: String): List<Poetry> {
        return try {
            val jsonStr = context.assets.open(path).bufferedReader().use { it.readText() }
            val items = json.decodeFromString<List<PoetryJson>>(jsonStr)
            items.map { Poetry(verse = it.verse, source = it.source, fullText = it.fullText) }
        } catch (e: Exception) {
            android.util.Log.w("DailyPoetry", "加载 $path 失败: ${e.message}")
            emptyList()
        }
    }

    /** 内置最小 fallback（防止 assets 加载失败时无诗可用） */
    private val FALLBACK_POEMS = listOf(
        Poetry("春风又绿江南岸，明月何时照我还", "泊船瓜洲·王安石",
            "京口瓜洲一水间，\n钟山只隔数重山。\n春风又绿江南岸，\n明月何时照我还。"),
        Poetry("海内存知己，天涯若比邻", "送杜少府之任蜀州·王勃",
            "城阙辅三秦，风烟望五津。\n与君离别意，同是宦游人。\n海内存知己，天涯若比邻。\n无为在歧路，儿女共沾巾。"),
        Poetry("会当凌绝顶，一览众山小", "望岳·杜甫",
            "岱宗夫如何？齐鲁青未了。\n造化钟神秀，阴阳割昏晓。\n荡胸生曾云，决眦入归鸟。\n会当凌绝顶，一览众山小。"),
        Poetry("但愿人长久，千里共婵娟", "水调歌头·苏轼",
            "明月几时有？把酒问青天。\n不知天上宫阙，今夕是何年。\n但愿人长久，千里共婵娟。"),
        Poetry("长风破浪会有时，直挂云帆济沧海", "行路难·李白",
            "行路难，行路难，多歧路，今安在？\n长风破浪会有时，直挂云帆济沧海。"),
    )
}
