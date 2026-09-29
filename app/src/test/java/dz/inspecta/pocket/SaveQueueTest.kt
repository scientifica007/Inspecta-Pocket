package dz.inspecta.pocket

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SaveQueueTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun suspendedWritesKeepStateAndNoteChangesInSubmissionOrder() = runTest {
        val queue = SaveQueue()
        val firstWrite = CompletableDeferred<Unit>()
        val operations = mutableListOf<String>()
        var savedState = "unanswered"
        var savedNote = ""
        queue.enqueue {
            firstWrite.await()
            savedState = "compliant"
            operations += "first state"
        }
        queue.enqueue {
            savedNote = "ملاحظة أولى"
            operations += "first note"
        }
        queue.enqueue {
            savedState = "non-compliant"
            operations += "final state"
        }
        queue.enqueue {
            savedNote = "الملاحظة النهائية"
            operations += "final note"
        }
        runCurrent()
        assertEquals(4, queue.pendingCount.value)
        assertTrue(operations.isEmpty())

        firstWrite.complete(Unit)
        assertTrue(queue.flush())
        assertEquals(listOf("first state", "first note", "final state", "final note"), operations)
        assertEquals("non-compliant", savedState)
        assertEquals("الملاحظة النهائية", savedNote)
        assertEquals(0, queue.pendingCount.value)
        assertNull(queue.error.value)
    }

    @Test
    fun flushWaitsForEarlierWritesAndActsAsAnOrderedBarrier() = runTest {
        val queue = SaveQueue()
        val earlierWrite = CompletableDeferred<Unit>()
        val laterWrite = CompletableDeferred<Unit>()
        val operations = mutableListOf<String>()
        queue.enqueue {
            earlierWrite.await()
            operations += "earlier"
        }
        val barrier = async { queue.flush() }
        runCurrent()
        assertFalse(barrier.isCompleted)
        queue.enqueue {
            laterWrite.await()
            operations += "later"
        }

        earlierWrite.complete(Unit)
        runCurrent()
        assertTrue(barrier.isCompleted)
        assertTrue(barrier.await())
        assertEquals(listOf("earlier"), operations)
        assertEquals(1, queue.pendingCount.value)

        laterWrite.complete(Unit)
        assertTrue(queue.flush())
        assertEquals(listOf("earlier", "later"), operations)
        assertEquals(0, queue.pendingCount.value)
    }

    @Test
    fun failedWriteKeepsLaterWritesPendingAndRetryPreservesTheirOrder() = runTest {
        val queue = SaveQueue()
        var shouldFail = true
        var attempts = 0
        val operations = mutableListOf<String>()
        queue.enqueue {
            attempts++
            if (shouldFail) throw IOException("Simulated storage failure")
            operations += "first"
        }
        queue.enqueue { operations += "second" }
        runCurrent()
        assertEquals(1, attempts)
        assertEquals(2, queue.pendingCount.value)
        assertNotNull(queue.error.value)
        assertTrue(operations.isEmpty())

        assertFalse(queue.flush())
        assertEquals(2, attempts)
        assertEquals(2, queue.pendingCount.value)
        assertTrue(operations.isEmpty())

        shouldFail = false
        assertTrue(queue.flush())
        assertEquals(3, attempts)
        assertEquals(listOf("first", "second"), operations)
        assertEquals(0, queue.pendingCount.value)
        assertNull(queue.error.value)
    }

    @Test
    fun leavingScreenAndCancellingItsFlushWaiterDoesNotCancelQueuedWrites() = runTest {
        val queue = SaveQueue()
        val screenScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val operations = mutableListOf<String>()
        val screenJob = screenScope.launch {
            queue.enqueue {
                delay(100)
                operations += "saved after screen left"
            }
            queue.flush()
        }
        runCurrent()
        assertEquals(1, queue.pendingCount.value)
        screenScope.cancel()
        advanceUntilIdle()

        assertTrue(screenJob.isCancelled)
        assertEquals(listOf("saved after screen left"), operations)
        assertEquals(0, queue.pendingCount.value)
        assertTrue(queue.flush())
    }
}
