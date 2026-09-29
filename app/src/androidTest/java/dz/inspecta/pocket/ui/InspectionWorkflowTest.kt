package dz.inspecta.pocket.ui

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dz.inspecta.pocket.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * Runs the field workflow against the real application and its Room database.
 * Deliberately retains its visit and never clears pre-existing inspection data.
 */
@RunWith(AndroidJUnit4::class)
class InspectionWorkflowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun createEvaluateSaveReopenAndRecreateVisit() {
        val institution = "مؤسسة اختبار القبول"
        val subject = "اختبار حفظ الزيارة"
        val observation = "ملاحظة اختبار القبول: يلزم تحديث الجدول ومتابعة التنفيذ."

        waitForText("زيارات التفتيش")
        compose.onNodeWithText("زيارة جديدة").performClick()
        compose.onNodeWithText("بدء الزيارة").performClick()
        scrollToText("أدخل اسم المؤسسة")
        compose.onNodeWithText("أدخل اسم المؤسسة").assertIsDisplayed()
        scrollToText("أدخل موضوع الزيارة")
        compose.onNodeWithText("أدخل موضوع الزيارة").assertIsDisplayed()

        compose.onNode(hasSetTextAction() and hasText("اسم المؤسسة"))
            .performScrollTo().performTextInput(institution)
        compose.onNode(hasSetTextAction() and hasText("موضوع الزيارة"))
            .performScrollTo().performTextInput(subject)
        compose.onNodeWithText("بدء الزيارة").performClick()
        waitForText("تقييم الزيارة")

        choose("وضوح جدول العمل", "مطابق")
        choose("تحديد المهام والمسؤوليات", "غير مطابق")
        choose("متابعة الحضور والغياب", "غير منطبق")

        scrollToText("وضوح جدول العمل")
        compose.onNode(inChecklistCard("وضوح جدول العمل", hasSetTextAction()))
            .performScrollTo().performTextInput(observation)
        compose.onNodeWithText("حفظ وعرض الملخص").performClick()
        waitForText("ملخص الزيارة")
        verifySummaryAndNote(observation)

        compose.onNodeWithText("رجوع").performClick()
        waitForText("زيارات التفتيش")
        // The just-updated visit is first, even when this acceptance test has run before.
        compose.onAllNodesWithText(institution).onFirst().performClick()
        waitForText("ملخص الزيارة")
        verifySummaryAndNote(observation)

        compose.activityRule.scenario.recreate()
        waitForText("ملخص الزيارة")
        scrollToText(observation)
        compose.onNodeWithText(observation).assertIsDisplayed()

        compose.onNodeWithText("تعديل التقييم").performClick()
        waitForText("تقييم الزيارة")
        verifyAnswer("وضوح جدول العمل", "مطابق")
        verifyAnswer("تحديد المهام والمسؤوليات", "غير مطابق")
        verifyAnswer("متابعة الحضور والغياب", "غير منطبق")
        compose.onNodeWithText("حفظ وعرض الملخص").performClick()
        waitForText("ملخص الزيارة")
        verifyShareIntent(institution, subject, observation)
    }

    private fun verifyShareIntent(institution: String, subject: String, observation: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val captured = AtomicReference<Intent?>()
        // The no-argument monitor intercepts starts on API 26+. Block every start during
        // this click, so even a malformed share cannot open or send to an external app.
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult {
                captured.set(Intent(intent))
                return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            compose.onNodeWithText("مشاركة النتائج").performClick()
            compose.waitUntil(timeoutMillis = 5_000) { captured.get() != null }
            val chooser = requireNotNull(captured.get())
            assertEquals(Intent.ACTION_CHOOSER, chooser.action)
            @Suppress("DEPRECATION")
            val share = requireNotNull(chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))
            assertEquals(Intent.ACTION_SEND, share.action)
            assertEquals("text/plain", share.type)
            assertTrue(share.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty().contains(subject))
            val report = share.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            assertTrue(report.contains(institution))
            assertTrue(report.contains(subject))
            assertTrue(report.contains(observation))
            val expectedItems = listOf(
                "وضوح جدول العمل",
                "تحديد المهام والمسؤوليات",
                "متابعة الحضور والغياب",
                "تحديث السجلات الإدارية",
                "تنظيم الأرشيف",
                "توفر خطط العمل",
                "سلامة الأثاث والتجهيزات",
                "توفر الوسائل الأساسية",
                "متابعة الصيانة",
                "وضوح مخارج الإخلاء",
                "جاهزية وسائل الإطفاء",
                "نظافة المرافق",
                "إعداد الأنشطة التعليمية",
                "متابعة تقدم المتعلمين",
                "تقديم الدعم البيداغوجي",
            )
            expectedItems.forEach { title ->
                assertTrue("The shared report must contain checklist item: $title", report.contains(title))
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    private fun choose(itemTitle: String, answer: String) {
        scrollToText(itemTitle)
        compose.onNode(inChecklistCard(itemTitle, hasText(answer)))
            .performScrollTo().performClick().assertIsSelected()
    }

    private fun verifyAnswer(itemTitle: String, answer: String) {
        scrollToText(itemTitle)
        compose.onNode(inChecklistCard(itemTitle, hasText(answer)))
            .performScrollTo().assertIsSelected()
    }

    private fun verifySummaryAndNote(observation: String) {
        scrollToText("البنود المقيّمة: 3 / 15")
        compose.onNodeWithText("البنود المقيّمة: 3 / 15").assertIsDisplayed()
        compose.onAllNodesWithText("1").assertCountEquals(3)
        compose.onNodeWithText("12").assertIsDisplayed()
        scrollToText(observation)
        compose.onNodeWithText(observation).assertIsDisplayed()
    }

    private fun inChecklistCard(title: String, target: SemanticsMatcher): SemanticsMatcher =
        target and hasAnyAncestor(hasAnyChild(hasText(title)))

    private fun scrollToText(text: String) {
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
    }

    private fun waitForText(text: String) {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
