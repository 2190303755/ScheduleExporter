package eric.schedule_exporter.util

const val STRING_NAV_MODE = "navigation_mode"
const val INDICATOR_NAV_MODE_ANDROID = 2
const val INDICATOR_NAV_MODE_HARMONY = 105
const val DO_NOT_CONTINUE = "do not continue"
const val QUOTED_DO_NOT_CONTINUE = "\"$DO_NOT_CONTINUE\""
const val MIME_TYPE_CSV = "text/csv"
const val MIME_TYPE_ZIP = "application/zip"
const val DUMPER_NAME = $$"ScheduleExporter$WebDumper"
const val DUMP_SOURCES = """(() => {
const dumper = ${DUMPER_NAME};
const uuid = dumper.uuid();
dumper.dump(uuid, location.href, document.body.outerHTML);
for (const frame of document.getElementsByTagName("iframe")) {
    const content = frame.contentDocument;
    dumper.dump(uuid, frame.src, content ? content.body.outerHTML : "! NO ACCESS !");
}
return uuid;
})();"""
const val INJECT_CONSOLE = """(() => {
const proto = window.VConsole;
if (proto) {
    const vConsole = proto.instance;
    if (vConsole) {
        vConsole.show();
        vConsole.showSwitch();
        return "Show";
    }
}
const script = document.createElement("script");
script.src = "https://unpkg.com/vconsole@latest/dist/vconsole.min.js";
script.onload = () => {
    const vConsole = new window.VConsole();
    vConsole.show();
    vConsole.showSwitch();
};
document.head.appendChild(script);
return "Inject";
})();"""