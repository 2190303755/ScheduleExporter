package eric.schedule_exporter.handler

import eric.schedule_exporter.handler.impl.WakeUpHandler

enum class HandlerType(val factory: () -> ScheduleHandler) {
    WAKE_UP_HANDLER({ WakeUpHandler })
}