package one.mixin.android.ui.home.web3.trade

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CandleChartStateTest {
    @Test
    fun selectionFollowsTimestampWhenWindowSlides() {
        val state = CandleChartState()
        state.updateCandles(candles(0..99))
        state.scrollByPx(1f)
        state.select(state.xOf(50.5f))

        state.updateCandles(candles(1..100))

        assertEquals(49, state.selectedIndex)
        assertEquals(50L, state.candles[state.selectedIndex!!].timestamp)
    }

    @Test
    fun selectionClearsWhenSelectedCandleDisappears() {
        val state = CandleChartState()
        state.updateCandles(candles(0..29))
        state.select(state.xOf(0.5f))

        state.updateCandles(candles(1..30))

        assertNull(state.selectedIndex)
    }

    @Test
    fun selectionSurvivesPriceRefresh() {
        val state = CandleChartState()
        state.updateCandles(candles(0..29))
        state.select(state.xOf(29.5f))
        val updated = state.candles.map { it.copy(close = 2.0) }

        state.updateCandles(updated)

        assertEquals(29, state.selectedIndex)
        assertEquals(2.0, state.candles[state.selectedIndex!!].close, 0.0)
    }

    @Test
    fun longPressTriggersAfterScrollEvenWithHighFrequencyMicroMoves() = runBlocking(ManualFrameClock()) {
        val state = CandleChartState().apply {
            plotWidthPx = 300f
            updateCandles(candles(0..99))
        }
        val pointerScope = FakePointerInputScope()
        var longPressCount = 0
        var selectionMoveCount = 0
        val gestureJob = launch(start = CoroutineStart.UNDISPATCHED) {
            pointerScope.detectChartGestures(
                state = state,
                scope = this,
                decay = exponentialDecay(),
                onLongPress = { longPressCount++ },
                onSelectionMove = { selectionMoveCount++ },
            )
        }

        // Scroll the chart horizontally first and start a fling.
        pointerScope.send(down(id = 1L, uptimeMillis = 0L, x = 150f))
        pointerScope.advanceTimeBy(16L)
        pointerScope.send(move(id = 1L, uptimeMillis = 16L, previousX = 150f, x = 190f))
        pointerScope.advanceTimeBy(16L)
        pointerScope.send(move(id = 1L, uptimeMillis = 32L, previousX = 190f, x = 230f))
        pointerScope.advanceTimeBy(16L)
        pointerScope.send(up(id = 1L, uptimeMillis = 48L, previousX = 230f, x = 230f))
        assertTrue(state.isScrolling)

        // Press down to stop the fling and emit 4ms sub-slop micro-moves
        // (faster than Compose's 8ms timeout micro-delay floor).
        pointerScope.advanceTimeBy(52L)
        var now = 100L
        var x = 150f
        pointerScope.send(down(id = 2L, uptimeMillis = now, x = x))
        assertFalse(state.isScrolling)
        repeat(80) { step ->
            pointerScope.advanceTimeBy(4L)
            now += 4L
            val nextX = if (step % 2 == 0) 150.5f else 150f
            pointerScope.send(move(id = 2L, uptimeMillis = now, previousX = x, x = nextX))
            x = nextX
        }

        assertEquals(1, longPressCount)
        assertNotNull(state.selectedIndex)

        // Scrub to another candle and release.
        pointerScope.advanceTimeBy(16L)
        now += 16L
        pointerScope.send(move(id = 2L, uptimeMillis = now, previousX = x, x = 90f))
        assertEquals(1, selectionMoveCount)

        pointerScope.advanceTimeBy(16L)
        now += 16L
        pointerScope.send(up(id = 2L, uptimeMillis = now, previousX = 90f, x = 90f))
        assertNull(state.selectedIndex)

        gestureJob.cancelAndJoin()
    }

    @Test
    fun longPressTriggersWithoutMoveEventsAndClearsOnCancel() = runBlocking(ManualFrameClock()) {
        val state = CandleChartState().apply {
            plotWidthPx = 300f
            updateCandles(candles(0..99))
        }
        val pointerScope = FakePointerInputScope()
        var longPressCount = 0
        val gestureJob = launch(start = CoroutineStart.UNDISPATCHED) {
            pointerScope.detectChartGestures(
                state = state,
                scope = this,
                decay = exponentialDecay(),
                onLongPress = { longPressCount++ },
                onSelectionMove = {},
            )
        }

        pointerScope.send(down(id = 1L, uptimeMillis = 0L, x = 150f))
        pointerScope.advanceTimeBy(310L)

        assertEquals(1, longPressCount)
        assertNotNull(state.selectedIndex)

        gestureJob.cancelAndJoin()
        assertNull(state.selectedIndex)
        assertFalse(state.isScrolling)
    }

    private fun candles(indices: IntRange) = indices.map {
        ChartCandle(it.toLong(), 1.0, 2.0, 0.5, 1.0)
    }

    private fun down(id: Long, uptimeMillis: Long, x: Float, y: Float = 50f): PointerEvent =
        PointerEvent(
            listOf(
                PointerInputChange(
                    id = PointerId(id),
                    uptimeMillis = uptimeMillis,
                    position = Offset(x, y),
                    pressed = true,
                    pressure = 1f,
                    previousUptimeMillis = uptimeMillis,
                    previousPosition = Offset(x, y),
                    previousPressed = false,
                    isInitiallyConsumed = false,
                    type = PointerType.Touch,
                ),
            ),
        )

    private fun move(id: Long, uptimeMillis: Long, previousX: Float, x: Float, y: Float = 50f): PointerEvent =
        PointerEvent(
            listOf(
                PointerInputChange(
                    id = PointerId(id),
                    uptimeMillis = uptimeMillis,
                    position = Offset(x, y),
                    pressed = true,
                    pressure = 1f,
                    previousUptimeMillis = uptimeMillis - 4L,
                    previousPosition = Offset(previousX, y),
                    previousPressed = true,
                    isInitiallyConsumed = false,
                    type = PointerType.Touch,
                ),
            ),
        )

    private fun up(id: Long, uptimeMillis: Long, previousX: Float, x: Float, y: Float = 50f): PointerEvent =
        PointerEvent(
            listOf(
                PointerInputChange(
                    id = PointerId(id),
                    uptimeMillis = uptimeMillis,
                    position = Offset(x, y),
                    pressed = false,
                    pressure = 0f,
                    previousUptimeMillis = uptimeMillis - 4L,
                    previousPosition = Offset(previousX, y),
                    previousPressed = true,
                    isInitiallyConsumed = false,
                    type = PointerType.Touch,
                ),
            ),
        )

    private class ManualFrameClock : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R =
            suspendCancellableCoroutine { }
    }

    private class FakePointerInputScope : PointerInputScope {
        override val density: Float = 1f
        override val fontScale: Float = 1f
        override val size: IntSize = IntSize(360, 180)
        override val viewConfiguration: ViewConfiguration = object : ViewConfiguration {
            override val longPressTimeoutMillis: Long = 400L
            override val doubleTapTimeoutMillis: Long = 300L
            override val doubleTapMinTimeMillis: Long = 40L
            override val touchSlop: Float = 24f
        }

        private class ScheduledTimeout(val fireAtMillis: Long, val onFire: () -> Unit)
        private class FakeTimeoutException : CancellationException("PointerEventTimeout")

        private var currentTimeMillis: Long = 0L
        private var latestEvent: PointerEvent = PointerEvent(emptyList())
        private var awaiter: CancellableContinuation<PointerEvent>? = null
        private val timeouts = mutableListOf<ScheduledTimeout>()

        suspend fun send(event: PointerEvent) {
            latestEvent = event
            awaiter?.let { continuation ->
                awaiter = null
                continuation.resume(event)
            }
            yield()
        }

        suspend fun advanceTimeBy(deltaMillis: Long) {
            currentTimeMillis += deltaMillis
            val due = timeouts.filter { it.fireAtMillis <= currentTimeMillis }
            timeouts.removeAll(due)
            due.forEach { it.onFire() }
            yield()
        }

        override suspend fun <R> awaitPointerEventScope(block: suspend AwaitPointerEventScope.() -> R): R =
            suspendCancellableCoroutine { completion ->
                val scope = object : AwaitPointerEventScope, Continuation<R> {
                    override val context: CoroutineContext = EmptyCoroutineContext
                    override val density: Float = this@FakePointerInputScope.density
                    override val fontScale: Float = this@FakePointerInputScope.fontScale
                    override val size: IntSize = this@FakePointerInputScope.size
                    override val currentEvent: PointerEvent get() = latestEvent
                    override val viewConfiguration: ViewConfiguration = this@FakePointerInputScope.viewConfiguration

                    override fun resumeWith(result: Result<R>) {
                        completion.resumeWith(result)
                    }

                    override suspend fun awaitPointerEvent(pass: PointerEventPass): PointerEvent =
                        suspendCancellableCoroutine { continuation ->
                            awaiter = continuation
                            continuation.invokeOnCancellation {
                                if (awaiter === continuation) awaiter = null
                            }
                        }

                    // Mirrors Compose's SuspendingPointerInputFilter.withTimeout 8ms micro-delay floor.
                    override suspend fun <T> withTimeoutOrNull(
                        timeMillis: Long,
                        block: suspend AwaitPointerEventScope.() -> T,
                    ): T? {
                        val effectiveDelay = (timeMillis - 8L).coerceAtLeast(0L) + 8L
                        val timeout = ScheduledTimeout(currentTimeMillis + effectiveDelay) {
                            awaiter?.let { continuation ->
                                awaiter = null
                                continuation.resumeWithException(FakeTimeoutException())
                            }
                        }
                        timeouts.add(timeout)
                        return try {
                            block()
                        } catch (_: FakeTimeoutException) {
                            null
                        } finally {
                            timeouts.remove(timeout)
                        }
                    }
                }
                completion.invokeOnCancellation { cause ->
                    awaiter?.cancel(cause)
                    awaiter = null
                }
                block.createCoroutine(scope, scope).resume(Unit)
            }
    }
}
