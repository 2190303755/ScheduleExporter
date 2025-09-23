package eric.schedule_exporter.util

const val STRING_NAV_MODE = "navigation_mode"
const val INDICATOR_NAV_MODE_ANDROID = 2
const val INDICATOR_NAV_MODE_HARMONY = 105
const val DO_NOT_CONTINUE = "do not continue"
const val WRAPPED_DO_NOT_CONTINUE = "\"$DO_NOT_CONTINUE\""
const val MIME_TYPE_CSV = "text/csv"
const val MIME_TYPE_ZIP = "application/zip"
const val DUMPER_NAME = "ScheduleExporter\$WebDumper"
const val DUMP_SOURCES = """(() => {
const dumper = ${DUMPER_NAME};
const uuid = dumper.uuid();
dumper.dump(uuid, location.href, document.body.outerHTML);
for (const frame of document.getElementsByTagName("iframe")) {
    dumper.dump(uuid, frame.src, frame.contentDocument?.body?.outerHTML);
}
return uuid;
})();"""