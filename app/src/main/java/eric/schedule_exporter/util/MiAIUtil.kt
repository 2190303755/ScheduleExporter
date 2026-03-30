package eric.schedule_exporter.util

import android.util.Base64
import android.util.Log
import androidx.annotation.StringRes
import androidx.collection.IntSet
import eric.schedule_exporter.R
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
val JSON_CONFIG = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

enum class MiAISource(
    @field:StringRes @param:StringRes val app: Int,
    val alias: String,
    val host: String
) {
    INTEGRATED(R.string.miai_integrated, "course-app-miui", "i.xiaomixiaoai.com"),
    DEDICATED(R.string.miai_dedicated, "course-app-aiSchedule", "i.ai.mi.com")
}

@JvmInline
@Serializable(with = HTMLColor.Companion::class)
value class HTMLColor(val rgb: Int) {
    companion object : KSerializer<HTMLColor> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("HTMLColor", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: HTMLColor) {
            val hex = value.rgb.toString(16)
            val builder = StringBuilder().append('#')
            repeat(6 - hex.length) {
                builder.append('0')
            }
            encoder.encodeString(builder.append(hex).toString())
        }

        override fun deserialize(decoder: Decoder): HTMLColor {
            val color = decoder.decodeString()
            return HTMLColor((if (color.startsWith('#')) color.substring(1) else color).toInt(16))
        }
    }
}

@Serializable
data class MiAISessionStyle(
    val color: HTMLColor,
    val background: HTMLColor
) {
    companion object : KSerializer<MiAISessionStyle> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("MiAISessionStyle", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: MiAISessionStyle) {
            encoder.encodeString(JSON_CONFIG.encodeToString(value))
        }

        override fun deserialize(decoder: Decoder) =
            JSON_CONFIG.decodeFromString<MiAISessionStyle>(decoder.decodeString())

        @JvmField
        val BUILTIN_STYLES = listOf(
            0x00A6F2 over 0xE5F4FF,
            0xFC6B50 over 0xFDEBDE,
            0x3CB3C8 over 0xDEFBF8,
            0x7D7AEA over 0xEDEDFF,
            0xFF9900 over 0xFCEBCD,
            0xEF5B75 over 0xFFEFF0,
            0x5B8EFF over 0xEAF1FF,
            0xF067BB over 0xFFEDF8,
            0x29BBAA over 0xE2F8F3,
            0xCBA713 over 0xFFF8C8,
            0xB967E3 over 0xF9EDFF,
            0x6E8ADA over 0xF3F2FD
        )
    }
}

infix fun Int.over(background: Int): MiAISessionStyle =
    MiAISessionStyle(HTMLColor(this), HTMLColor(background))

@Serializable
data class MiAISession(
    val name: String,
    val day: Int,
    val sections: String,
    val weeks: String,
    val teacher: String = "",
    val position: String = "",
    val extend: String = "",
    @Serializable(with = MiAISessionStyle.Companion::class)
    val style: MiAISessionStyle = MiAISessionStyle.BUILTIN_STYLES.first()
)

@Serializable
data class MiAIScheduleMeta(
    val id: Long,
    val name: String
)

@Serializable
data class MiAIScheduleConfig(
    val id: Long,
    val presentWeek: Int? = null,
    val totalWeek: Int? = null,
    val isWeekend: Int? = null,
    val morningNum: Int? = null,
    val afternoonNum: Int? = null,
    val nightNum: Int? = null,
    val speak: Int? = null,
    val weekStart: Int? = null,
    val extend: String? = null,
    val startSemester: String? = null,
    val school: String? = null,
    val sections: String? = null,
    @Transient val sectionTimes: String? = null // Response
) {
    fun applyTimeTable(periods: List<MiAIPeriod>): MiAIScheduleConfig {
        return this.copy(
            sections = JSON_CONFIG.encodeToString(periods),
            sectionTimes = null
        )
    }
}

@Serializable
data class MiAIPeriod(
    @SerialName("i") val index: Int,
    @Serializable(with = Moment.Companion::class)
    @SerialName("s") val start: Moment,
    @Serializable(with = Moment.Companion::class)
    @SerialName("e") val end: Moment
)

@Serializable
data class MiAIScheduleDetails(
    val id: Long,
    val name: String,
    val setting: MiAIScheduleConfig
)

@Serializable
data class MiAIDebugInfo(
    val authorization: String? = null,
    val deviceId: String? = null,
    val appId: String? = null,
    val serviceToken: String? = null,
    val userAgent: String? = null
) {
    fun buildContext(userAgent: String? = null): MiAIContext = if (
        this.authorization.isNullOrBlank()
    ) MiAIContext(
        MiAISource.DEDICATED,
        """AO-TOKEN-V1 dev_app_id:${
            requireNotNull(this.appId) {
                "Failed to construct token due to missing appId"
            }
        },access_token:${
            requireNotNull(this.serviceToken) {
                "Failed to construct token due to missing serviceToken"
            }
        },scope_data:${
            Base64.encodeToString(
                """{"d":"${
                    requireNotNull(this.deviceId) {
                        "Failed to construct token due to missing deviceId"
                    }
                }"}""".toByteArray(),
                Base64.NO_WRAP
            )
        }""",
        this.userAgent ?: userAgent
    ) else MiAIContext(
        MiAISource.INTEGRATED,
        this.authorization,
        this.userAgent ?: userAgent
    )
}

@Serializable
data class MiAIContext(
    val source: MiAISource,
    val authorization: String,
    val userAgent: String? = null
) {
    fun appendHeaders(request: Request.Builder) {
        if (this.userAgent !== null) {
            request.header("User-Agent", this.userAgent)
        }
        request.header("Authorization", this.authorization)
    }

    inline fun fetch(
        path: String,
        queries: HttpUrl.Builder.() -> Unit = {}
    ): Request.Builder = Request.Builder().url(
        HttpUrl.Builder()
            .scheme("https")
            .host(this.source.host)
            .addEncodedPathSegment("course-multi-auth")
            .addPathSegments(path)
            .addQueryParameter("requestId", randomUniqueString())
            .addQueryParameter("sourceName", this.source.alias)
            .apply { queries() }
            .toString()
    ).apply(this::appendHeaders)
}

@Serializable
data class MiAIScheduleResponse<T>(
    val code: Int = -1,
    val data: T? = null,
    val desc: String = ""
)

fun Session.toMiAISession(
    weeks: IntSet,
    style: MiAISessionStyle,
    extend: String = ""
) = MiAISession(
    this.subject,
    this.dayOfWeek.value,
    (this.start..this.end).joinToString(","),
    weeks.joinToString(","),
    this.teacher,
    this.location,
    extend,
    style
)

inline fun <reified T> Response.resolveMiAIScheduleResponse(fallback: () -> T): T = this.use {
    if (!it.isSuccessful) throw IOException("HTTP ${it.code}: ${it.message}")
    val body = it.body.string()
    val response = try {
        JSON_CONFIG.decodeFromString<MiAIScheduleResponse<T>>(body)
    } catch (e: Exception) {
        Log.e("MiAIHandler", "Failed to decoded: $body", e)
        throw IllegalArgumentException("Failed to parse JSON", e)
    }
    if (it.code < 0) throw Exception(response.desc) // TODO: custom exception with status code
    response.data ?: fallback()
}

suspend fun MiAIContext.listSchedules() = request {
    this.fetch("table")
}.resolveMiAIScheduleResponse { listOf<MiAIScheduleMeta>() }

suspend fun MiAIContext.createSchedule(name: String): Long = request {
    JSON_CONFIG.encodeToString(buildJsonObject {
        put("name", name)
        put("current", 0)
        put("sourceName", this@createSchedule.source.alias)
    }).toRequestBody(JSON_MEDIA_TYPE).postTo(
        "https://${this.source.host}/course-multi-auth/table",
        this::appendHeaders
    )
}.resolveMiAIScheduleResponse { 0L }

suspend fun MiAIContext.querySchedule(schedule: Long) = request {
    this.fetch("table") {
        addQueryParameter("ctId", schedule.toString())
    }
}.resolveMiAIScheduleResponse<MiAIScheduleDetails> {
    throw NullPointerException()
}

suspend fun MiAIContext.configureSchedule(
    details: MiAIScheduleDetails,
    periods: List<MiAIPeriod>
): Boolean = request {
    JSON_CONFIG.encodeToString(buildJsonObject {
        put("ctId", details.id)
        put("name", details.name)
        put("setting", JSON_CONFIG.encodeToJsonElement(details.setting.applyTimeTable(periods)))
        put("sourceName", this@configureSchedule.source.alias)
    }).toRequestBody(JSON_MEDIA_TYPE).putTo(
        "https://${this.source.host}/course-multi-auth/table",
        this::appendHeaders
    )
}.resolveMiAIScheduleResponse { false }

suspend fun MiAIContext.switchSchedule(old: Long, neo: Long): Boolean = request {
    JSON_CONFIG.encodeToString(buildJsonObject {
        put("fromCtId", old)
        put("toCtId", neo)
        put("sourceName", this@switchSchedule.source.alias)
    }).toRequestBody(JSON_MEDIA_TYPE).postTo(
        "https://${this.source.host}/course-multi-auth/table_switch",
        this::appendHeaders
    )
}.resolveMiAIScheduleResponse { false }

suspend fun MiAIContext.uploadSessions(
    schedule: Long,
    sessions: Either<List<MiAISession>, JsonElement>
): List<Int> = request {
    JSON_CONFIG.encodeToString(buildJsonObject {
        put("ctId", schedule)
        put("courses", sessions.mapLeft(JSON_CONFIG::encodeToJsonElement).unbox())
        put("sourceName", this@uploadSessions.source.alias)
    }).toRequestBody(JSON_MEDIA_TYPE).postTo(
        "https://${this.source.host}/course-multi-auth/courseInfos",
        this::appendHeaders
    )
}.resolveMiAIScheduleResponse { emptyList() }