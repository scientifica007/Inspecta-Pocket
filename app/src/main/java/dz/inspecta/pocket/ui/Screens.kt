package dz.inspecta.pocket.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dz.inspecta.pocket.data.AnswerState
import dz.inspecta.pocket.data.ItemResult
import dz.inspecta.pocket.data.VisitDetail
import dz.inspecta.pocket.data.VisitSummary
import java.time.LocalDate

@Composable
fun HomeScreen(
    visits: List<VisitSummary>,
    onNew: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    Scaffold(
        topBar = { ScreenHeader("Inspecta Pocket") },
        bottomBar = {
            ActionBar {
                Button(onClick = onNew, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text("زيارة جديدة")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("زيارات التفتيش", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "زياراتك ونتائجها محفوظة على هذا الجهاز.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (visits.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("لا توجد زيارات بعد", style = MaterialTheme.typography.titleLarge)
                            Text("ابدأ زيارة جديدة لتقييم البنود وتسجيل ملاحظاتك، ثم ارجع إليها في أي وقت.")
                        }
                    }
                }
            }
            items(visits, key = { it.id }) { visit ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClickLabel = "عرض الزيارة", role = Role.Button) { onOpen(visit.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(visit.institutionName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(visit.subject, style = MaterialTheme.typography.bodyLarge)
                        Text("تاريخ الزيارة: ${visit.date}", style = MaterialTheme.typography.bodyMedium)
                        Completion(visit.answered, visit.total)
                        Text("عرض الزيارة", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun NewVisitScreen(
    onBack: () -> Unit,
    onCreate: (String, String, String) -> Unit,
    creating: Boolean,
    error: String?,
) {
    var institution by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = { ScreenHeader("زيارة جديدة", onBack, backEnabled = !creating) },
        bottomBar = {
            ActionBar {
                Button(
                    enabled = !creating,
                    onClick = {
                        submitted = true
                        if (institution.isNotBlank() && subject.isNotBlank()) {
                            onCreate(institution.trim(), date, subject.trim())
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    if (creating) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (creating) "جارٍ إنشاء الزيارة…" else "بدء الزيارة")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Text("بيانات الزيارة", style = MaterialTheme.typography.headlineSmall) }
            item {
                OutlinedTextField(
                    value = institution,
                    onValueChange = { institution = it },
                    label = { Text("اسم المؤسسة") },
                    isError = submitted && institution.isBlank(),
                    supportingText = if (submitted && institution.isBlank()) ({ Text("أدخل اسم المؤسسة") }) else null,
                    enabled = !creating,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("تاريخ الزيارة", style = MaterialTheme.typography.labelLarge)
                    OutlinedButton(
                        enabled = !creating,
                        onClick = {
                            val selected = LocalDate.parse(date)
                            DatePickerDialog(
                                context,
                                { _, year, month, day -> date = LocalDate.of(year, month + 1, day).toString() },
                                selected.year,
                                selected.monthValue - 1,
                                selected.dayOfMonth,
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) { Text("$date  ·  تغيير التاريخ") }
                }
            }
            item {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("موضوع الزيارة") },
                    isError = submitted && subject.isBlank(),
                    supportingText = if (submitted && subject.isBlank()) ({ Text("أدخل موضوع الزيارة") }) else null,
                    enabled = !creating,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
                )
            }
            item { Text("يمكنك حفظ تقييمك تدريجياً والعودة لإكمال الزيارة لاحقاً.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (error != null) item { ErrorMessage(error) }
        }
    }
}

@Composable
fun InspectionScreen(
    detail: VisitDetail,
    onBack: () -> Unit,
    onState: (String, AnswerState) -> Unit,
    onNote: (String, String) -> Unit,
    onFinish: () -> Unit,
    saving: Boolean,
    error: String?,
) {
    val answered = detail.items.count { it.result.state != AnswerState.UNANSWERED }
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = { ScreenHeader("تقييم الزيارة", onBack) },
        bottomBar = {
            ActionBar {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (error != null) {
                        ErrorMessage(error)
                    } else {
                        Text(
                            if (saving) "جارٍ حفظ التغييرات…" else "تم حفظ التغييرات على الجهاز",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(if (error != null) "إعادة محاولة الحفظ" else "حفظ وعرض الملخص")
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(detail.institutionName, style = MaterialTheme.typography.titleLarge)
                    Text(detail.visit.subject, style = MaterialTheme.typography.bodyLarge)
                    Text(detail.visit.date, style = MaterialTheme.typography.bodyMedium)
                    Completion(answered, detail.items.size)
                    Text("اختر نتيجة كل بند وأضف ملاحظة عند الحاجة.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            detail.items.groupBy { it.item.section }.forEach { (section, entries) ->
                item(key = "section_$section") { SectionHeading(section) }
                items(entries.sortedBy { it.item.displayOrder }, key = { it.item.id }) { entry ->
                    ChecklistEditor(entry, onState, onNote)
                }
            }
        }
    }
}

@Composable
fun DetailsScreen(
    detail: VisitDetail,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
) {
    val answered = detail.items.count { it.result.state != AnswerState.UNANSWERED }
    Scaffold(
        topBar = { ScreenHeader("ملخص الزيارة", onBack) },
        bottomBar = {
            ActionBar {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onEdit, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("تعديل التقييم") }
                    OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("مشاركة النتائج") }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(detail.institutionName, style = MaterialTheme.typography.titleLarge)
                        Text(detail.visit.subject, style = MaterialTheme.typography.titleMedium)
                        Text("تاريخ الزيارة: ${detail.visit.date}")
                        Completion(answered, detail.items.size)
                        HorizontalDivider()
                        SummaryLine("مطابق", detail.items.count { it.result.state == AnswerState.COMPLIANT })
                        SummaryLine("غير مطابق", detail.items.count { it.result.state == AnswerState.NON_COMPLIANT })
                        SummaryLine("غير منطبق", detail.items.count { it.result.state == AnswerState.NOT_APPLICABLE })
                        SummaryLine("لم يُقيّم بعد", detail.items.size - answered)
                    }
                }
            }
            detail.items.groupBy { it.item.section }.forEach { (section, entries) ->
                item(key = "section_$section") { SectionHeading(section) }
                items(entries.sortedBy { it.item.displayOrder }, key = { it.item.id }) { entry ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(entry.item.title, style = MaterialTheme.typography.titleMedium)
                            AnswerLabel(entry.result.state)
                            if (entry.result.note.isNotBlank()) {
                                HorizontalDivider()
                                Text("الملاحظة", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(entry.result.note, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistEditor(
    entry: ItemResult,
    onState: (String, AnswerState) -> Unit,
    onNote: (String, String) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp).selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(entry.item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            entry.item.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            listOf(AnswerState.COMPLIANT, AnswerState.NON_COMPLIANT, AnswerState.NOT_APPLICABLE).forEach { state ->
                val selected = entry.result.state == state
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(10.dp),
                        )
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { onState(entry.item.id, state) })
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RadioButton(selected = selected, onClick = null)
                    Text(answerText(state), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (entry.result.state != AnswerState.UNANSWERED) {
                TextButton(onClick = { onState(entry.item.id, AnswerState.UNANSWERED) }) { Text("إلغاء تقييم هذا البند") }
            } else {
                Text("لم يُقيّم بعد", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedTextField(
                value = entry.result.note,
                onValueChange = { onNote(entry.item.id, it) },
                label = { Text("ملاحظة (اختياري)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 6,
            )
        }
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: (() -> Unit)? = null, backEnabled: Boolean = true) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp).heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (onBack != null) TextButton(onClick = onBack, enabled = backEnabled) { Text("رجوع") }
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActionBar(content: @Composable () -> Unit) {
    Surface(shadowElevation = 3.dp) {
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) { content() }
    }
}

@Composable
private fun Completion(answered: Int, total: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("البنود المقيّمة: $answered / $total", style = MaterialTheme.typography.bodyMedium)
        LinearProgressIndicator(
            progress = { if (total > 0) answered.toFloat() / total else 0f },
            modifier = Modifier.fillMaxWidth().height(6.dp),
        )
    }
}

@Composable
private fun SectionHeading(section: String) {
    Text(
        section,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
    )
}

@Composable
private fun SummaryLine(label: String, count: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        Text(count.toString(), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AnswerLabel(state: AnswerState) {
    val color = when (state) {
        AnswerState.NON_COMPLIANT -> MaterialTheme.colorScheme.error
        AnswerState.COMPLIANT -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(answerText(state), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = color)
}

@Composable
private fun ErrorMessage(error: String) {
    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
}

private fun answerText(state: AnswerState): String = when (state) {
    AnswerState.UNANSWERED -> "لم يُقيّم بعد"
    AnswerState.COMPLIANT -> "مطابق"
    AnswerState.NON_COMPLIANT -> "غير مطابق"
    AnswerState.NOT_APPLICABLE -> "غير منطبق"
}
