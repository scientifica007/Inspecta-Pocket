package dz.inspecta.pocket.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class AnswerState(val label: String) {
    UNANSWERED("لم يُقيّم"),
    COMPLIANT("مطابق"),
    NON_COMPLIANT("غير مطابق"),
    NOT_APPLICABLE("غير منطبق"),
}

@Entity(tableName = "institutions", indices = [Index(value = ["name"], unique = true)])
data class Institution(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(
    tableName = "inspection_visits",
    foreignKeys = [ForeignKey(
        entity = Institution::class,
        parentColumns = ["id"],
        childColumns = ["institutionId"],
    )],
    indices = [Index("institutionId"), Index("updatedAt")],
)
data class InspectionVisit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val institutionId: Long,
    val date: String,
    val subject: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "checklist_items")
data class ChecklistItem(
    @PrimaryKey val id: String,
    val section: String,
    val title: String,
    val description: String? = null,
    val displayOrder: Int,
)

@Entity(
    tableName = "inspection_results",
    primaryKeys = ["visitId", "checklistItemId"],
    foreignKeys = [
        ForeignKey(
            entity = InspectionVisit::class,
            parentColumns = ["id"],
            childColumns = ["visitId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ChecklistItem::class,
            parentColumns = ["id"],
            childColumns = ["checklistItemId"],
        ),
    ],
    indices = [Index("checklistItemId")],
)
data class InspectionResult(
    val visitId: Long,
    val checklistItemId: String,
    val state: AnswerState = AnswerState.UNANSWERED,
    val note: String = "",
)

data class VisitSummary(
    val id: Long,
    val institutionName: String,
    val date: String,
    val subject: String,
    val answered: Int,
    val total: Int,
)

data class ItemResult(val item: ChecklistItem, val result: InspectionResult)

data class VisitDetail(
    val visit: InspectionVisit,
    val institutionName: String,
    val items: List<ItemResult>,
)

data class ProgressCounts(
    val answered: Int,
    val total: Int,
    val compliant: Int,
    val nonCompliant: Int,
    val notApplicable: Int,
    val unanswered: Int,
) {
    val fraction: Float get() = if (total == 0) 0f else answered.toFloat() / total
}

fun calculateProgress(states: Iterable<AnswerState>): ProgressCounts {
    var compliant = 0
    var nonCompliant = 0
    var notApplicable = 0
    var unanswered = 0
    for (state in states) {
        when (state) {
            AnswerState.COMPLIANT -> compliant++
            AnswerState.NON_COMPLIANT -> nonCompliant++
            AnswerState.NOT_APPLICABLE -> notApplicable++
            AnswerState.UNANSWERED -> unanswered++
        }
    }
    val answered = compliant + nonCompliant + notApplicable
    return ProgressCounts(answered, answered + unanswered, compliant, nonCompliant, notApplicable, unanswered)
}

fun VisitDetail.counts(): ProgressCounts = calculateProgress(items.map { it.result.state })

class AnswerStateConverters {
    @TypeConverter
    fun encode(value: AnswerState): String = value.name

    @TypeConverter
    fun decode(value: String): AnswerState = AnswerState.valueOf(value)
}
