package dz.inspecta.pocket.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomPersistenceTest {
    private lateinit var context: Context
    private lateinit var databaseName: String
    private lateinit var database: InspectionDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        databaseName = "persistence-test-${UUID.randomUUID()}.db"
        database = InspectionDatabase.create(context, databaseName)
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun visitStatesAndArabicNoteSurviveDatabaseCloseAndReopen() = runBlocking {
        var repository = InspectionRepository(database)
        repository.initialize()
        val visitId = repository.createVisit("  مؤسسة اختبار  ", "2026-09-29", "متابعة ميدانية")
        val firstId = seedChecklist[0].id
        val secondId = seedChecklist[1].id
        val note = "تحتاج الوثائق إلى تحديث.\nتتم المتابعة في الزيارة القادمة."
        repository.setState(visitId, firstId, AnswerState.NON_COMPLIANT)
        repository.setNote(visitId, firstId, note)
        // Field-specific updates must retain the existing note.
        repository.setState(visitId, firstId, AnswerState.COMPLIANT)
        repository.setState(visitId, secondId, AnswerState.NOT_APPLICABLE)
        database.close()

        database = InspectionDatabase.create(context, databaseName)
        repository = InspectionRepository(database)
        repository.initialize()
        val restored = repository.getVisit(visitId)
        assertNotNull(restored)
        restored!!
        assertEquals("مؤسسة اختبار", restored.institutionName)
        assertEquals("2026-09-29", restored.visit.date)
        assertEquals("متابعة ميدانية", restored.visit.subject)
        assertTrue(restored.visit.updatedAt >= restored.visit.createdAt)
        assertEquals(15, restored.items.size)
        assertEquals(AnswerState.COMPLIANT, restored.items[0].result.state)
        assertEquals(note, restored.items[0].result.note)
        assertEquals(AnswerState.NOT_APPLICABLE, restored.items[1].result.state)
        assertEquals(ProgressCounts(2, 15, 1, 0, 1, 13), restored.counts())
        val homeVisit = repository.observeVisits().first().single()
        assertEquals(visitId, homeVisit.id)
        assertEquals(2, homeVisit.answered)
        assertEquals(15, homeVisit.total)
        assertEquals(restored, repository.observeVisit(visitId).first())
    }

    @Test
    fun visitsKeepIndependentAnswersEvenForSameInstitution() = runBlocking {
        val repository = InspectionRepository(database)
        val first = repository.createVisit("مؤسسة واحدة", "2026-09-29", "زيارة أولى")
        val second = repository.createVisit("مؤسسة واحدة", "2026-09-30", "زيارة ثانية")
        repository.setNote(first, seedChecklist[0].id, "ملاحظة الزيارة الأولى")
        repository.setState(first, seedChecklist[0].id, AnswerState.NON_COMPLIANT)
        val firstDetail = repository.getVisit(first)!!
        val secondDetail = repository.getVisit(second)!!
        assertEquals(firstDetail.visit.institutionId, secondDetail.visit.institutionId)
        assertEquals(AnswerState.UNANSWERED, secondDetail.items[0].result.state)
        assertEquals("", secondDetail.items[0].result.note)
        assertEquals("ملاحظة الزيارة الأولى", firstDetail.items[0].result.note)
    }
}
