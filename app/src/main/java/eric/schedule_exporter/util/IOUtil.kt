package eric.schedule_exporter.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.Writer

inline fun <reified T : Writer> T.write(char: Char) {
    this.write(char.code)
}

fun Context.getScheduleDir() =
    File(this.externalCacheDir ?: this.cacheDir, "schedules")

fun Context.getDumpDir() =
    File(this.externalCacheDir ?: this.cacheDir, "dumps")

fun File.toUri(context: Context): Uri = FileProvider.getUriForFile(
    context,
    "eric.schedule_exporter.file_provider",
    this
)

inline fun File.resolveUnique(init: String? = null, format: (String) -> String): File {
    this.mkdirs()
    var child = File(this, format(init ?: randomUniqueString()))
    while (child.exists()) {
        child = File(this, format(randomUniqueString()))
    }
    return child
}