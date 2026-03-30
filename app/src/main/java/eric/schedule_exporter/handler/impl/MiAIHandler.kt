package eric.schedule_exporter.handler.impl

import android.content.ClipData
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.collection.IntSet
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import eric.schedule_exporter.R
import eric.schedule_exporter.ScheduleExporterApplication.Companion.SCHEDULE_PERIODS
import eric.schedule_exporter.data.HandlerSpec
import eric.schedule_exporter.data.MiAISpec
import eric.schedule_exporter.data.setScheduleHandler
import eric.schedule_exporter.handler.HandlerType
import eric.schedule_exporter.handler.ScheduleHandler
import eric.schedule_exporter.ui.AlertDialog
import eric.schedule_exporter.ui.IconButton
import eric.schedule_exporter.ui.Indicator
import eric.schedule_exporter.ui.InfoBar
import eric.schedule_exporter.ui.TextButton
import eric.schedule_exporter.ui.TooltipBox
import eric.schedule_exporter.ui.applyInfoBarPadding
import eric.schedule_exporter.util.Either
import eric.schedule_exporter.util.JSON_CONFIG
import eric.schedule_exporter.util.MiAIContext
import eric.schedule_exporter.util.MiAIDebugInfo
import eric.schedule_exporter.util.MiAIPeriod
import eric.schedule_exporter.util.MiAISessionStyle
import eric.schedule_exporter.util.MiAISource
import eric.schedule_exporter.util.Session
import eric.schedule_exporter.util.collectText
import eric.schedule_exporter.util.configureSchedule
import eric.schedule_exporter.util.createSchedule
import eric.schedule_exporter.util.end
import eric.schedule_exporter.util.querySchedule
import eric.schedule_exporter.util.toMiAISession
import eric.schedule_exporter.util.uploadSessions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.encodeToStream
import java.io.OutputStream

fun String?.resolveContext(): Result<MiAIContext?> = if (this.isNullOrBlank()) {
    Result.success(null)
} else runCatching {
    JSON_CONFIG.decodeFromString<MiAIDebugInfo>(this).buildContext()
}

object MiAIHandler : ScheduleHandler<JsonElement> {
    override val type: HandlerType
        get() = HandlerType.MIAI_HANDLER

    @JvmField
    var miai: MiAIContext? = null

    override fun formatName(id: String) = "${id}.json"
    override fun convert(sessions: Map<Session, IntSet>): JsonElement {
        var index = 0
        val styles = hashMapOf<String, MiAISessionStyle>()
        return JSON_CONFIG.encodeToJsonElement(
            sessions.map {
                it.key.toMiAISession(it.value, styles.getOrPut(it.key.subject) {
                    val style = MiAISessionStyle.BUILTIN_STYLES[index]
                    index = (index + 1) % MiAISessionStyle.BUILTIN_STYLES.size
                    style
                })
            }
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(sessions: JsonElement, stream: OutputStream) {
        JSON_CONFIG.encodeToStream(sessions, stream)
    }

    override suspend fun export(data: JsonElement, context: Context) {
        val miai = this.miai ?: return
        withContext(Dispatchers.IO) {
            try {
                val name = "schedule"
                val schedule = miai.createSchedule(name)
                require(schedule != 0L) {
                    "Failed to create schedule"
                }
                val detail = miai.querySchedule(schedule)
                if (miai.configureSchedule(
                        detail,
                        SCHEDULE_PERIODS
                            .sortedBy { it.period.start }
                            .mapIndexed { index, period ->
                                MiAIPeriod(
                                    index = index + 1,
                                    start = period.start,
                                    end = period.end
                                )
                            }
                    )
                ) {
                    val ids = miai.uploadSessions(schedule, Either.Right(data))
                    Log.d("MiAIHandler", "Upload ${ids.size} session(s)")
                }
            } catch (e: Exception) {
                Log.e("MiAIHandler", "Failed to upload", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, e.message ?: "未知错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override suspend fun loadSpec(config: HandlerSpec?): Boolean {
        if (config is MiAISpec) {
            this.miai = config.miai
            return true
        }
        return false
    }

    override suspend fun saveSpec(): MiAISpec? {
        return MiAISpec(this.miai ?: return null)
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    override fun ConfigSection(onCancel: () -> Unit) {
        val context = LocalContext.current
        val clipboard = LocalClipboard.current
        val scope = rememberCoroutineScope()
        var miai by remember { mutableStateOf(this.miai) }
        var requireInfo by rememberSaveable { mutableStateOf(miai === null) }
        miai?.apply {
            HorizontalDivider()
            InfoBar(
                title = "用户信息源",
                description = stringResource(this.source.app),
                modifier = Modifier
                    .clickable { requireInfo = true }
                    .applyInfoBarPadding(),
                indicator = {
                    Indicator(icon = Icons.Filled.Edit)
                }
            )
            this.userAgent?.let {
                HorizontalDivider()
                InfoBar(
                    title = "用户代理",
                    description = it,
                    enableMarquee = true,
                    modifier = Modifier
                        .clickable {
                            scope.launch {
                                clipboard.setClipEntry(
                                    ClipData.newPlainText("UserAgent", it).toClipEntry()
                                )
                            }
                        }
                        .applyInfoBarPadding(),
                    indicator = {
                        Indicator(icon = Icons.Filled.ContentCopy)
                    }
                )
            }
            LaunchedEffect(Unit) {
                context.setScheduleHandler(MiAIHandler)
            }
        }
        if (requireInfo) {
            var source: MiAISource? by remember { mutableStateOf(miai?.source) }
            val authorization = rememberTextFieldState(miai?.authorization ?: "")
            val userAgent = rememberTextFieldState(miai?.userAgent ?: "")
            val requireSource by remember { derivedStateOf { source === null } }
            val requireToken by remember { derivedStateOf { authorization.text.isBlank() } }
            var expanded by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = { requireInfo = false },
                title = { Text("用户信息") },
                neutralButton = {
                    TextButton("从JSON解析") {
                        scope.launch {
                            clipboard.getClipEntry()
                                ?.clipData
                                ?.collectText()
                                .resolveContext()
                                .fold(onSuccess = {
                                    if (it === null) {
                                        Toast.makeText(
                                            context,
                                            "剪切板为空",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        source = it.source
                                        authorization.setTextAndPlaceCursorAtEnd(it.authorization)
                                        if (!it.userAgent.isNullOrEmpty()) {
                                            userAgent.setTextAndPlaceCursorAtEnd(it.userAgent)
                                        }
                                    }
                                }) {
                                    Toast.makeText(
                                        context,
                                        "解析失败：${it.message ?: it.cause ?: "未知错误"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                },
                confirmButton = {
                    TextButton("继续", !requireSource && !requireToken) {
                        if (source !== null && authorization.text.isNotBlank()) {
                            this.miai = MiAIContext(
                                source!!,
                                authorization.text.toString(),
                                userAgent.text.ifBlank { null }?.toString()
                            )
                            miai = this.miai
                            requireInfo = false
                        }
                    }
                },
                dismissButton = {
                    TextButton("取消") {
                        requireInfo = false
                        onCancel()
                    }
                },
                properties = DialogProperties(dismissOnClickOutside = false)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = source?.let { stringResource(it.app) } ?: "",
                            onValueChange = {},
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            readOnly = true,
                            singleLine = true,
                            label = { Text("用户信息源") },
                            isError = requireSource,
                            supportingText = {
                                if (requireSource) {
                                    Text("必须选择信息源")
                                }
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = expanded
                                )
                            },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            containerColor = MenuDefaults.groupStandardContainerColor,
                            shape = MenuDefaults.standaloneGroupShape,
                        ) {
                            val sources = MiAISource.entries
                            val options = sources.size
                            sources.forEachIndexed { index, option ->
                                DropdownMenuItem(
                                    shapes = MenuDefaults.itemShape(index, options),
                                    text = {
                                        Text(
                                            stringResource(option.app),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    },
                                    selected = option === source,
                                    onClick = {
                                        source = option
                                        expanded = false
                                    },
                                    selectedLeadingIcon = {
                                        Icon(
                                            Icons.Filled.Check,
                                            modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                            contentDescription = null,
                                        )
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                )
                            }
                        }
                    }
                    var hideToken by remember { mutableStateOf(true) }
                    OutlinedSecureTextField(
                        state = authorization,
                        label = { Text("身份令牌") },
                        textObfuscationMode = if (hideToken) TextObfuscationMode.RevealLastTyped else TextObfuscationMode.Visible,
                        trailingIcon = {
                            TooltipBox(if (hideToken) "Show" else "Hide") { tooltip ->
                                IconButton(
                                    if (hideToken) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    tooltip
                                ) {
                                    hideToken = !hideToken
                                }
                            }
                        },
                        isError = requireToken,
                        supportingText = {
                            if (requireToken) {
                                Text("必须填写身份令牌")
                            }
                        },
                    )
                    OutlinedTextField(
                        state = userAgent,
                        label = { Text("用户代理") },
                        lineLimits = TextFieldLineLimits.SingleLine,
                        trailingIcon = {
                            TooltipBox("Clear") { tooltip ->
                                IconButton(Icons.Filled.Clear, tooltip) {
                                    userAgent.clearText()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    @Composable
    override fun displayName() = stringResource(R.string.app_miai)
}