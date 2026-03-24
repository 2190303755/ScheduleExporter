package eric.schedule_exporter.handler

import eric.schedule_exporter.handler.impl.MiAIHandler
import eric.schedule_exporter.handler.impl.WakeUpHandler

enum class HandlerType(val handler: ScheduleHandler<*>) {
    WAKE_UP_HANDLER(WakeUpHandler),
    MIAI_HANDLER(MiAIHandler)
}