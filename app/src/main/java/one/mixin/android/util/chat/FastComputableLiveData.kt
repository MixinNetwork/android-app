package one.mixin.android.util.chat

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.lifecycle.LiveData
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("RestrictedApi")
abstract class FastComputableLiveData<T> @JvmOverloads constructor( /* synthetic access */val mExecutor: Executor = ArchTaskExecutor.getIOThreadExecutor()) {
    val liveData: InnerLiveData<T> = object : InnerLiveData<T>() {
        override fun onActive() {
            mExecutor.execute(mRefreshRunnable)
        }
    }
    val mInvalid = AtomicBoolean(true)
    val mComputing = AtomicBoolean(false)

    @VisibleForTesting
    val mRefreshRunnable = Runnable {
        var computed: Boolean
        do {
            computed = false
            // compute can happen only in 1 thread but no reason to lock others.
            if (mComputing.compareAndSet(false, true)) {
                // as long as it is invalid, keep computing.
                try {
                    var value: T? = null
                    while (mInvalid.compareAndSet(true, false)) {
                        computed = true
                        value = compute()
                    }
                    if (computed && value != null) {
                        liveData.post(value)
                    }
                } finally {
                    // release compute lock
                    mComputing.set(false)
                }
            }
            // check invalid after releasing compute lock to avoid the following scenario.
            // Thread A runs compute()
            // Thread A checks invalid, it is false
            // Main thread sets invalid to true
            // Thread B runs, fails to acquire compute lock and skips
            // Thread A releases compute lock
            // We've left invalid in set state. The check below recovers.
        } while (computed && mInvalid.get())
    }

    // invalidation check always happens on the main thread
    @VisibleForTesting
    val mInvalidationRunnable = Runnable {
        val isActive = liveData.hasActiveObservers()
        if (mInvalid.compareAndSet(false, true)) {
            if (isActive) {
                mExecutor.execute(mRefreshRunnable)
            }
        }
    }

    /**
     * Invalidates the LiveData.
     *
     *
     * When there are active observers, this will trigger a call to [.compute].
     */
    fun invalidate() {
        ArchTaskExecutor.getInstance().executeOnMainThread(mInvalidationRunnable)
    }

    // TODO https://issuetracker.google.com/issues/112197238
    @WorkerThread
    protected abstract fun compute(): T

    open class InnerLiveData<T> : LiveData<T>() {
        fun post(value: T) {
            postValue(value)
        }
    }
}
