package dz.inspecta.pocket.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

data class ResultWithItem(
    @Embedded val result: InspectionResult,
    @Relation(parentColumn = "checklistItemId", entityColumn = "id")
    val item: ChecklistItem,
)

data class VisitRecord(
    @Embedded val visit: InspectionVisit,
    @Relation(parentColumn = "institutionId", entityColumn = "id")
    val institution: Institution,
    @Relation(parentColumn = "id", entityColumn = "visitId", entity = InspectionResult::class)
    val results: List<ResultWithItem>,
) {
    fun toDetail(): VisitDetail = VisitDetail(
        visit = visit,
        institutionName = institution.name,
        items = results.map { ItemResult(it.item, it.result) }.sortedBy { it.item.displayOrder },
    )
}

@Dao
interface InspectionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seed(items: List<ChecklistItem>)

    @Query("SELECT * FROM checklist_items ORDER BY displayOrder, id")
    suspend fun checklist(): List<ChecklistItem>

    @Query("SELECT * FROM institutions WHERE name = :name LIMIT 1")
    suspend fun institutionNamed(name: String): Institution?

    @Insert
    suspend fun insertInstitution(institution: Institution): Long

    @Insert
    suspend fun insertVisit(visit: InspectionVisit): Long

    @Insert
    suspend fun insertResults(results: List<InspectionResult>)

    @Query("""
        SELECT v.id, i.name AS institutionName, v.date, v.subject,
               SUM(CASE WHEN r.state != 'UNANSWERED' THEN 1 ELSE 0 END) AS answered,
               COUNT(r.checklistItemId) AS total
        FROM inspection_visits AS v
        INNER JOIN institutions AS i ON i.id = v.institutionId
        LEFT JOIN inspection_results AS r ON r.visitId = v.id
        GROUP BY v.id
        ORDER BY v.updatedAt DESC, v.id DESC
    """)
    fun observeVisits(): Flow<List<VisitSummary>>

    @Transaction
    @Query("SELECT * FROM inspection_visits WHERE id = :id")
    fun observeVisit(id: Long): Flow<VisitRecord?>

    @Transaction
    @Query("SELECT * FROM inspection_visits WHERE id = :id")
    suspend fun getVisit(id: Long): VisitRecord?

    @Query("UPDATE inspection_results SET state = :state WHERE visitId = :visitId AND checklistItemId = :itemId")
    suspend fun updateState(visitId: Long, itemId: String, state: AnswerState): Int

    @Query("UPDATE inspection_results SET note = :note WHERE visitId = :visitId AND checklistItemId = :itemId")
    suspend fun updateNote(visitId: Long, itemId: String, note: String): Int

    @Query("UPDATE inspection_visits SET updatedAt = :timestamp WHERE id = :visitId")
    suspend fun touchVisit(visitId: Long, timestamp: Long)
}

@Database(
    entities = [Institution::class, InspectionVisit::class, ChecklistItem::class, InspectionResult::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(AnswerStateConverters::class)
abstract class InspectionDatabase : RoomDatabase() {
    abstract fun inspectionDao(): InspectionDao

    companion object {
        fun create(context: Context, databaseName: String = "inspecta-pocket.db"): InspectionDatabase =
            Room.databaseBuilder(context.applicationContext, InspectionDatabase::class.java, databaseName)
                .build()
    }
}
