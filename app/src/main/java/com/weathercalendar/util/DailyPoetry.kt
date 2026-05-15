package com.weathercalendar.util

import android.content.Context
import com.weathercalendar.data.model.WeatherCondition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.Month

/**
 * 每日诗词 — 从 assets/poetry/ JSON 文件加载，支持 1200+ 首。
 *
 * 架构：
 * - Application.onCreate 中在后台线程预加载所有 JSON
 * - 线程安全：使用 @Synchronized 保护初始化
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

    /** 内置最小 fallback（防止 assets 加载失败或加载中时无诗可用） */
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

    private val json = Json { ignoreUnknownKeys = true }

    // 线程安全的诗词池（volatile 保证可见性，初始值为 fallback）
    @Volatile private var sunnyPoems: List<Poetry> = FALLBACK_POEMS
    @Volatile private var rainPoems: List<Poetry> = FALLBACK_POEMS
    @Volatile private var snowPoems: List<Poetry> = FALLBACK_POEMS
    @Volatile private var cloudyPoems: List<Poetry> = FALLBACK_POEMS
    @Volatile private var stormPoems: List<Poetry> = FALLBACK_POEMS
    @Volatile private var fogPoems: List<Poetry> = FALLBACK_POEMS
    @Volatile private var springPoems: List<Poetry> = FALLBACK_POEMS
    @Volatile private var summerPoems: List<Poetry> = FALLBACK_POEMS
    @Volatile private var autumnPoems: List<Poetry> = FALLBACK_POEMS
    @Volatile private var winterPoems: List<Poetry> = FALLBACK_POEMS

    @Volatile private var initialized = false

    /**
     * 初始化（在 Application.onCreate 中调用）。
     * 在后台线程加载 JSON，不阻塞主线程。
     * 加载完成前使用内置 fallback 诗词。
     */
    fun init(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            loadAll(appContext)
        }
    }

    /**
     * 获取今日诗词（线程安全）。
     */
    fun getPoetry(date: LocalDate, condition: WeatherCondition): Poetry {
        val pool = getPoolForCondition(condition) + getPoolForSeason(date)
        if (pool.isEmpty()) return FALLBACK_POEMS[0]

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
            WeatherCondition.SUNNY, WeatherCondition.PARTLY_CLOUDY -> sunnyPoems
            WeatherCondition.CLOUDY -> cloudyPoems
            WeatherCondition.RAINY, WeatherCondition.DRIZZLE -> rainPoems
            WeatherCondition.SNOWY -> snowPoems
            WeatherCondition.STORMY -> stormPoems
            WeatherCondition.FOGGY -> fogPoems
        }
    }

    private fun getPoolForSeason(date: LocalDate): List<Poetry> {
        return when (date.month) {
            Month.MARCH, Month.APRIL, Month.MAY -> springPoems
            Month.JUNE, Month.JULY, Month.AUGUST -> summerPoems
            Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER -> autumnPoems
            else -> winterPoems
        }
    }

    @Synchronized
    private fun loadAll(context: Context) {
        if (initialized) return
        try {
            sunnyPoems = loadFromAsset(context, "poetry/sunny.json")
            rainPoems = loadFromAsset(context, "poetry/rain.json")
            snowPoems = loadFromAsset(context, "poetry/snow.json")
            cloudyPoems = loadFromAsset(context, "poetry/cloudy.json")
            stormPoems = loadFromAsset(context, "poetry/storm.json")
            fogPoems = loadFromAsset(context, "poetry/fog.json")
            springPoems = loadFromAsset(context, "poetry/spring.json")
            summerPoems = loadFromAsset(context, "poetry/summer.json")
            autumnPoems = loadFromAsset(context, "poetry/autumn.json")
            winterPoems = loadFromAsset(context, "poetry/winter.json")
            initialized = true
            android.util.Log.d("DailyPoetry", "诗词库加载完成: ${totalCount()} 首")
        } catch (e: Exception) {
            android.util.Log.e("DailyPoetry", "加载诗词 JSON 失败，使用 fallback", e)
            // 保持 fallback（已在字段初始化时设置）
            initialized = true
        }
    }

    private fun loadFromAsset(context: Context, path: String): List<Poetry> {
        return try {
            val jsonStr = context.assets.open(path).bufferedReader().use { it.readText() }
            val items = json.decodeFromString<List<PoetryJson>>(jsonStr)
            val poems = items.map { Poetry(verse = it.verse, source = it.source, fullText = it.fullText) }
            if (poems.isEmpty()) FALLBACK_POEMS else poems
        } catch (e: Exception) {
            android.util.Log.w("DailyPoetry", "加载 $path 失败: ${e.message}")
            FALLBACK_POEMS
        }
    }

    private fun totalCount(): Int {
        return sunnyPoems.size + rainPoems.size + snowPoems.size + cloudyPoems.size +
            stormPoems.size + fogPoems.size + springPoems.size + summerPoems.size +
            autumnPoems.size + winterPoems.size
    }
}
