package dz.inspecta.pocket.data

/** Text is built from a persisted visit snapshot so sharing includes confirmed data. */
fun VisitDetail.toShareText(): String = buildString {
    appendLine("Inspecta Pocket — تقرير زيارة تفتيش")
    appendLine("المؤسسة: $institutionName")
    appendLine("التاريخ: ${visit.date}")
    appendLine("الموضوع: ${visit.subject}")
    val counts = counts()
    appendLine()
    appendLine("الإنجاز: ${counts.answered} / ${counts.total}")
    appendLine("مطابق: ${counts.compliant}")
    appendLine("غير مطابق: ${counts.nonCompliant}")
    appendLine("غير منطبق: ${counts.notApplicable}")
    appendLine("لم يُقيّم: ${counts.unanswered}")
    items.sortedBy { it.item.displayOrder }.groupBy { it.item.section }.forEach { (section, results) ->
        appendLine()
        appendLine("[$section]")
        results.forEach { (item, result) ->
            appendLine("${item.displayOrder}. ${item.title}")
            appendLine("الحالة: ${result.state.label}")
            if (result.note.isNotBlank()) appendLine("الملاحظة: ${result.note.trim()}")
            appendLine()
        }
    }
}.trimEnd()
