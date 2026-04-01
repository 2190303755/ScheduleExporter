package eric.schedule_exporter.util

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.coroutines.executeAsync
import java.util.concurrent.TimeUnit

val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
val NETWORK_JSON = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

val GLOBAL_HTTP_CLIENT: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15L, TimeUnit.SECONDS)
    .readTimeout(30L, TimeUnit.SECONDS)
    .writeTimeout(30L, TimeUnit.SECONDS)
    .callTimeout(60L, TimeUnit.SECONDS)
    .build()

suspend inline fun request(
    factory: () -> Request.Builder
): Response = GLOBAL_HTTP_CLIENT.newCall(factory().build()).executeAsync()
