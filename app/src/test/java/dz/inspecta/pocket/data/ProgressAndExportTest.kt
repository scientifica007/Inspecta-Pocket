package dz.inspecta.pocket.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressAndExportTest {
    @Test
    fun emptyProgressIsFiniteAndZero() {
        val counts = calculateProgress(emptyList())
        assertEquals(ProgressCounts(0, 0, 0, 0, 0, 0), counts)
        assertEquals(0f, counts.fraction, 0f)
    }

    @Test
    fun unansweredItemsDoNotAdvanceProgress() {
        val counts = calculateProgress(List(15) { AnswerState.UNANSWERED })
        assertEquals(0, counts.answered)
        assertEquals(15, counts.total)
        assertEquals(15, counts.unanswered)
    }

    @Test
    fun notApplicableCountsAsAnsweredAndEachCategoryIsSeparate() {
        val counts = calculateProgress(listOf(
            AnswerState.COMPLIANT,
            AnswerState.COMPLIANT,
            AnswerState.NON_COMPLIANT,
            AnswerState.NOT_APPLICABLE,
            AnswerState.UNANSWERED,
        ))
        assertEquals(ProgressCounts(4, 5, 2, 1, 1, 1), counts)
        assertEquals(0.8f, counts.fraction, 0.0001f)
    }

    @Test
    fun checklistHasStableUniqueKeysAndFifteenOrderedItems() {
        assertEquals(15, seedChecklist.size)
        assertEquals(15, seedChecklist.map { it.id }.distinct().size)
        assertEquals((1..15).toList(), seedChecklist.map { it.displayOrder })
        assertEquals(5, seedChecklist.map { it.section }.distinct().size)
    }

    @Test
    fun exportIncludesMetadataArabicNotesCountsAndDisplayOrder() {
        val detail = VisitDetail(
            visit = InspectionVisit(7, 1, "2026-09-29", "متابعة السلامة", 1, 2),
            institutionName = "مدرسة الاختبار",
            items = listOf(
                ItemResult(seedChecklist[1], InspectionResult(7, seedChecklist[1].id, AnswerState.NOT_APPLICABLE)),
                ItemResult(seedChecklist[0], InspectionResult(7, seedChecklist[0].id, AnswerState.NON_COMPLIANT, "تحديث الجدول مطلوب\nقبل الزيارة القادمة")),
            ),
        )
        val text = detail.toShareText()
        assertTrue(text.contains("المؤسسة: مدرسة الاختبار"))
        assertTrue(text.contains("التاريخ: 2026-09-29"))
        assertTrue(text.contains("الموضوع: متابعة السلامة"))
        assertTrue(text.contains("الإنجاز: 2 / 2"))
        assertTrue(text.contains("غير مطابق: 1"))
        assertTrue(text.contains("غير منطبق: 1"))
        assertTrue(text.contains("تحديث الجدول مطلوب\nقبل الزيارة القادمة"))
        assertTrue(text.indexOf(seedChecklist[0].title) < text.indexOf(seedChecklist[1].title))
        assertEquals(1, Regex("الملاحظة:").findAll(text).count())
        assertFalse(text.contains("NON_COMPLIANT"))
    }
}
