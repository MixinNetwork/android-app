package one.mixin.android.extension

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

fun <T> LiveData<T>.observeOnce(
    lifecycleOwner: LifecycleOwner,
    observer: Observer<T>,
) {
    observe(
        lifecycleOwner,
        object : Observer<T> {
            override fun onChanged(value: T) {
                observer.onChanged(value)
                removeObserver(this)
            }
        },
    )
}

fun <T> LiveData<T>.observeUntil(
    target: T,
    observer: Observer<T>,
) {
    observeForever(
        object : Observer<T> {
            override fun onChanged(value: T) {
                observer.onChanged(value)
                if (target == value) {
                    removeObserver(this)
                }
            }
        },
    )
}

fun <T> LiveData<T>.observeOnceAtMost(
    lifecycleOwner: LifecycleOwner,
    observer: Observer<T>,
): Observer<T> {
    val o =
        object : Observer<T> {
            override fun onChanged(value: T) {
                observer.onChanged(value)
                removeObserver(this)
            }
        }
    observe(lifecycleOwner, o)
    return o
}
