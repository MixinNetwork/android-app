package one.mixin.android.extension

import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@MainThread
fun LifecycleOwner.launchUntilStop(block: suspend CoroutineScope.() -> Unit): Job {
    val job = lifecycleScope.launch(start = CoroutineStart.LAZY, block = block)
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_STOP) job.cancel()
    }
    lifecycle.addObserver(observer)
    job.invokeOnCompletion { lifecycle.removeObserver(observer) }
    job.start()
    return job
}
