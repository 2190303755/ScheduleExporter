package eric.schedule_exporter.util

import android.util.Base64
import android.util.Log
import androidx.collection.IntSet
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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

@Serializable
data class MiAISession(
    val name: String,
    val teacher: String,
    val position: String,
    val day: Int,
    val sections: String,
    val weeks: String
)


@Serializable
data class MiAIScheduleMeta(
    val id: Long,
    val name: String
)


@Serializable
data class MiAIScheduleConfig(
    val id: Long
)

@Serializable
data class MiAIScheduleDetails(
    val ctId: Long,
    val setting: MiAIScheduleConfig
)

@Serializable
data class MiAIDebugInfo(
    val authorization: String?,
    val deviceId: String?,
    val appId: String?,
    val serviceToken: String?,
    val userAgent: String?
) {
    fun buildContext(userAgent: String? = null): MiAIContext {
        if (this.authorization !== null && this.authorization.isNotEmpty()) {
            return MiAIContext(
                "course-app-miui",
                "i.xiaomixiaoai.com",
                this.authorization,
                this.userAgent ?: userAgent
            )
        }
        return MiAIContext(
            "course-app-aiSchedule",
            "i.ai.mi.com",
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
        )
    }
}

class MiAIContext(
    val source: String,
    val host: String,
    val authorization: String,
    val userAgent: String?
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
            .host(this.host)
            .addEncodedPathSegment("course-multi-auth")
            .addPathSegments(path)
            .addQueryParameter("requestId", randomUniqueString())
            .addQueryParameter("sourceName", this.source)
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

fun Session.toMiAISession(weeks: IntSet) = MiAISession(
    this.subject,
    this.teacher,
    this.location,
    this.dayOfWeek.value,
    (this.start..this.end).joinToString(","),
    weeks.joinToString(",")
)

inline fun <reified T> Response.resolveMiAIScheduleResponse(fallback: () -> T): T = this.use {
    if (!it.isSuccessful) throw IOException("HTTP ${it.code}: ${it.message}")
    val response = try {
        JSON_CONFIG.decodeFromString<MiAIScheduleResponse<T>>(it.body.string())
    } catch (e: Exception) {
        throw IllegalArgumentException("Failed to parse JSON: ${e.message}", e)
    }
    if (it.code < 0) throw Exception(response.desc) // TODO: custom exception with status code
    response.data ?: fallback()
}

suspend fun listMiAISchedule(context: MiAIContext) = request {
    context.fetch("table")
}.resolveMiAIScheduleResponse { listOf<MiAIScheduleMeta>() }

suspend fun queryMiAISchedule(context: MiAIContext, schedule: Long) = request {
    context.fetch("table") {
        addQueryParameter("ctId", schedule.toString())
    }
}.resolveMiAIScheduleResponse<MiAIScheduleDetails> {
    throw NullPointerException()
}

suspend fun createMiAISchedule(context: MiAIContext, name: String): Long = request {
    JSON_CONFIG.encodeToString(buildJsonObject {
        put("name", name)
        put("current", 0)
        put("sourceName", context.source)
    }).toRequestBody(JSON_MEDIA_TYPE).postTo(
        "https://${context.host}/course-multi-auth/table",
        context::appendHeaders
    )
}.resolveMiAIScheduleResponse { 0L }

suspend fun uploadMiAISessions(
    context: MiAIContext,
    schedule: Long,
    sessions: List<MiAISession>
): Unit = request {
    JSON_CONFIG.encodeToString(buildJsonObject {
        put("ctId", schedule)
        put("courses", JSON_CONFIG.encodeToJsonElement(sessions))
        put("sourceName", context.source)
    }).toRequestBody(JSON_MEDIA_TYPE).postTo(
        "https://${context.host}/course-multi-auth/courseInfos",
        context::appendHeaders
    )
}.resolveMiAIScheduleResponse {
    Log.d("MiAIUtil", "")
}