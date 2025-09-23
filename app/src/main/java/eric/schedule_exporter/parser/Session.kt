package eric.schedule_exporter.parser

import androidx.collection.MutableIntSet
import java.time.DayOfWeek

data class Session(
    val subject: String,
    val teacher: String,
    val location: String,
    val start: Int,
    val end: Int,
    val dayOfWeek: DayOfWeek,
    val weeks: MutableIntSet
) {
    fun canMergeWith(session: Session): Boolean = this.subject == session.subject
            && this.teacher == session.teacher
            && this.location == session.location
            && this.start == session.start
            && this.end == session.end
            && this.dayOfWeek === session.dayOfWeek
}