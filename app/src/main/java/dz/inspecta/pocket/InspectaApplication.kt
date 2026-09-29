package dz.inspecta.pocket

import android.app.Application
import dz.inspecta.pocket.data.InspectionDatabase
import dz.inspecta.pocket.data.InspectionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InspectaApplication : Application() {
    val repository by lazy { InspectionRepository(InspectionDatabase.create(this)) }
    val writes by lazy { SaveQueue() }
}

/** Application-owned writer: navigating away or recreating an Activity cannot cancel a save. */
class SaveQueue {
    private sealed interface Command {
        data class Write(val action: suspend () -> Unit) : Command
        data class Flush(val result: CompletableDeferred<Boolean>) : Command
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val commands = Channel<Command>(Channel.UNLIMITED)
    private val pending = MutableStateFlow(0)
    private val failure = MutableStateFlow<String?>(null)
    val pendingCount = pending.asStateFlow()
    val error = failure.asStateFlow()

    init {
        scope.launch {
            val backlog = ArrayDeque<suspend () -> Unit>()
            suspend fun drain() {
                while (backlog.isNotEmpty()) {
                    try {
                        backlog.first().invoke()
                        backlog.removeFirst()
                        pending.value -= 1
                        failure.value = null
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        failure.value = "تعذر الحفظ. تبقى التغييرات معروضة؛ اضغط إعادة محاولة الحفظ."
                        return
                    }
                }
            }
            for (command in commands) {
                when (command) {
                    is Command.Write -> {
                        backlog.addLast(command.action)
                        if (failure.value == null) drain()
                    }
                    is Command.Flush -> {
                        drain()
                        command.result.complete(backlog.isEmpty())
                    }
                }
            }
        }
    }

    fun enqueue(action: suspend () -> Unit) {
        pending.value += 1
        check(commands.trySend(Command.Write(action)).isSuccess)
    }

    suspend fun flush(): Boolean {
        val result = CompletableDeferred<Boolean>()
        commands.send(Command.Flush(result))
        return result.await()
    }
}
