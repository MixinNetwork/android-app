package one.mixin.android.ui.common.recyclerview

import android.annotation.SuppressLint
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.paging.AsyncPagedListDiffer
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.Executor

abstract class SafePagedListAdapter<T, VH : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<T>,
) : PagedListAdapter<T, VH>(diffCallback) {

    init {
        try {
            val mDiffer = PagedListAdapter::class.java.getDeclaredField("mDiffer")
            val executor = AsyncPagedListDiffer::class.java.getDeclaredField("mMainThreadExecutor")
            mDiffer.isAccessible = true
            executor.isAccessible = true

            val myDiffer = mDiffer.get(this) as AsyncPagedListDiffer<*>
            val foreGround = object : Executor {
                val mHandler = createAsync(Looper.getMainLooper())
                override fun execute(command: Runnable?) {
                    try {
                        mHandler.post {
                            try {
                                command?.run()
                            } catch (ignored: Exception) {
                            }
                        }
                    } catch (ignored: Exception) {
                    }
                }
            }

            executor.set(myDiffer, foreGround)
        } catch (ignored: Exception) {
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createAsync(looper: Looper): Handler {
        if (Build.VERSION.SDK_INT >= 28) {
            return Handler.createAsync(looper)
        }
        if (Build.VERSION.SDK_INT >= 16) {
            try {
                return Handler::class.java.getDeclaredConstructor(
                    Looper::class.java,
                    Handler.Callback::class.java,
                    Boolean::class.javaPrimitiveType,
                ).newInstance(looper, null, true)
            } catch (ignored: IllegalAccessException) {
            } catch (ignored: InstantiationException) {
            } catch (ignored: NoSuchMethodException) {
            } catch (e: InvocationTargetException) {
                return Handler(looper)
            }
        }
        return Handler(looper)
    }
}
