package dz.inspecta.pocket.data

/** IDs are durable database keys: keep them unchanged when revising visible wording. */
val seedChecklist: List<ChecklistItem> = listOf(
    ChecklistItem("organization_schedule", "التنظيم", "وضوح جدول العمل", "جدول محدّث ومتاح للعاملين.", 1),
    ChecklistItem("organization_roles", "التنظيم", "تحديد المهام والمسؤوليات", "توزيع واضح للمهام داخل المؤسسة.", 2),
    ChecklistItem("organization_attendance", "التنظيم", "متابعة الحضور والغياب", "وجود متابعة منتظمة وإجراءات عند الحاجة.", 3),
    ChecklistItem("documents_registers", "الوثائق", "تحديث السجلات الإدارية", "السجلات الأساسية مكتملة ومحدّثة.", 4),
    ChecklistItem("documents_archiving", "الوثائق", "تنظيم الأرشيف", "حفظ الوثائق وتصنيفها بما يسهّل الرجوع إليها.", 5),
    ChecklistItem("documents_plans", "الوثائق", "توفر خطط العمل", "خطط العمل والمتابعة متاحة ومحدّثة.", 6),
    ChecklistItem("equipment_furniture", "التجهيز", "سلامة الأثاث والتجهيزات", "الأثاث مناسب للاستعمال وخالٍ من التلف الظاهر.", 7),
    ChecklistItem("equipment_materials", "التجهيز", "توفر الوسائل الأساسية", "الوسائل اللازمة للعمل متوفرة وصالحة.", 8),
    ChecklistItem("equipment_maintenance", "التجهيز", "متابعة الصيانة", "رصد الأعطال ومتابعة إصلاحها.", 9),
    ChecklistItem("safety_exits", "السلامة", "وضوح مخارج الإخلاء", "الممرات والمخارج سالكة وواضحة.", 10),
    ChecklistItem("safety_fire", "السلامة", "جاهزية وسائل الإطفاء", "وسائل الإطفاء متاحة وتخضع للمتابعة.", 11),
    ChecklistItem("safety_hygiene", "السلامة", "نظافة المرافق", "المرافق نظيفة وتتوفر مستلزمات النظافة.", 12),
    ChecklistItem("pedagogy_preparation", "الجانب البيداغوجي", "إعداد الأنشطة التعليمية", "الأنشطة مخططة ومناسبة للأهداف التعليمية.", 13),
    ChecklistItem("pedagogy_followup", "الجانب البيداغوجي", "متابعة تقدم المتعلمين", "وجود متابعة منتظمة للتقدم والصعوبات.", 14),
    ChecklistItem("pedagogy_support", "الجانب البيداغوجي", "تقديم الدعم البيداغوجي", "إجراءات دعم ملائمة للحاجات المسجلة.", 15),
)
