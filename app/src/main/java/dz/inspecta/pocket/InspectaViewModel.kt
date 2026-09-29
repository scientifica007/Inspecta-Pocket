package dz.inspecta.pocket

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dz.inspecta.pocket.data.AnswerState
import dz.inspecta.pocket.data.VisitDetail
import dz.inspecta.pocket.data.VisitSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InspectaViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as InspectaApplication
    private val repository = app.repository
    private val current = MutableStateFlow<VisitDetail?>(null)
    val detail = current.asStateFlow()
    private val operationError = MutableStateFlow<String?>(null)
    val error = operationError.asStateFlow()
    private val busy = MutableStateFlow(false)
    val creating = busy.asStateFlow()
    private val newVisit = MutableStateFlow<Long?>(null)
    val createdVisit = newVisit.asStateFlow()
    val pending = app.writes.pendingCount
    val saveError = app.writes.error
    val visits = repository.observeVisits()
        .catch { operationError.value = "تعذر تحميل الزيارات. أعد فتح التطبيق للمحاولة مجدداً." }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null as List<VisitSummary>?)

    fun create(institution: String, date: String, subject: String) {
        if (busy.value) return
        busy.value = true
        operationError.value = null
        viewModelScope.launch {
            try {
                val id = repository.createVisit(institution, date, subject)
                current.value = repository.getVisit(id)
                newVisit.value = id
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                operationError.value = "تعذر إنشاء الزيارة. تحقق من البيانات وحاول مجدداً."
            } finally {
                busy.value = false
            }
        }
    }

    fun createdVisitHandled() { newVisit.value = null }

    suspend fun open(id: Long) {
        operationError.value = null
        // This snapshot already includes every local edit. Re-reading it while typing
        // could replace newer input with an older database query result.
        if (current.value?.visit?.id == id) return
        current.value = null
        try {
            if (!app.writes.flush()) return
            current.value = repository.getVisit(id)
            if (current.value == null) operationError.value = "لم يتم العثور على هذه الزيارة."
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            operationError.value = "تعذر فتح الزيارة. ارجع إلى القائمة وحاول مجدداً."
        }
    }

    fun setState(itemId: String, state: AnswerState) {
        val snapshot = current.value ?: return
        current.value = snapshot.copy(items = snapshot.items.map {
            if (it.item.id == itemId) it.copy(result = it.result.copy(state = state)) else it
        })
        app.writes.enqueue { repository.setState(snapshot.visit.id, itemId, state) }
    }

    fun setNote(itemId: String, note: String) {
        val snapshot = current.value ?: return
        current.value = snapshot.copy(items = snapshot.items.map {
            if (it.item.id == itemId) it.copy(result = it.result.copy(note = note)) else it
        })
        app.writes.enqueue { repository.setNote(snapshot.visit.id, itemId, note) }
    }

    suspend fun flush(): Boolean {
        // Include edits made while an earlier database write was still finishing.
        do {
            if (!app.writes.flush()) return false
        } while (pending.value > 0)
        return true
    }
}
