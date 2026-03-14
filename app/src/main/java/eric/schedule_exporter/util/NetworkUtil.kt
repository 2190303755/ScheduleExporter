package eric.schedule_exporter.util

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.coroutines.executeAsync
import java.util.concurrent.TimeUnit

val GLOBAL_HTTP_CLIENT: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15L, TimeUnit.SECONDS)
    .readTimeout(30L, TimeUnit.SECONDS)
    .writeTimeout(30L, TimeUnit.SECONDS)
    .callTimeout(60L, TimeUnit.SECONDS)
    .build()

inline fun RequestBody.postTo(
    url: String,
    configure: Request.Builder.() -> Unit
): Request.Builder = Request.Builder()
    .url(url)
    .apply { configure() }
    .post(this)

suspend inline fun request(
    factory: () -> Request.Builder
): Response = GLOBAL_HTTP_CLIENT.newCall(factory().build()).executeAsync()
