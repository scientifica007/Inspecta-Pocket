package dz.inspecta.pocket.data

import androidx.room.withTransaction
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InspectionRepository(private val database: InspectionDatabase) {
    private val dao = database.inspectionDao()

    fun observeVisits(): Flow<List<VisitSummary>> = dao.observeVisits()

    fun observeVisit(id: Long): Flow<VisitDetail?> = dao.observeVisit(id).map { it?.toDetail() }

    suspend fun getVisit(id: Long): VisitDetail? = dao.getVisit(id)?.toDetail()

    suspend fun initialize() {
        database.withTransaction { dao.seed(seedChecklist) }
    }

    suspend fun createVisit(institution: String, date: String, subject: String): Long {
        val institutionName = institution.trim()
        val visitSubject = subject.trim()
        require(institutionName.isNotEmpty()) { "اسم المؤسسة مطلوب" }
        require(visitSubject.isNotEmpty()) { "موضوع الزيارة مطلوب" }
        val visitDate = LocalDate.parse(date).toString()
        return database.withTransaction {
            dao.seed(seedChecklist)
            val institutionId = dao.institutionNamed(institutionName)?.id
                ?: dao.insertInstitution(Institution(name = institutionName))
            val now = System.currentTimeMillis()
            val visitId = dao.insertVisit(
                InspectionVisit(
                    institutionId = institutionId,
                    date = visitDate,
                    subject = visitSubject,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            dao.insertResults(dao.checklist().map { InspectionResult(visitId, it.id) })
            visitId
        }
    }

    suspend fun setState(visitId: Long, itemId: String, state: AnswerState) {
        database.withTransaction {
            check(dao.updateState(visitId, itemId, state) == 1) { "تعذر العثور على بند الزيارة" }
            dao.touchVisit(visitId, System.currentTimeMillis())
        }
    }

    suspend fun setNote(visitId: Long, itemId: String, note: String) {
        database.withTransaction {
            check(dao.updateNote(visitId, itemId, note) == 1) { "تعذر العثور على بند الزيارة" }
            dao.touchVisit(visitId, System.currentTimeMillis())
        }
    }
}
