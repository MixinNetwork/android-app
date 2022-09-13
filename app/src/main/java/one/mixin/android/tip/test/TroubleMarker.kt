package one.mixin.android.tip.test

import android.os.Process
import timber.log.Timber
import kotlin.random.Random

// Todo est the tool class, remember to delete
object TroubleMarker {
    const val STOP_NODE_SIGN = 0
    const val STOP_CREATE_PIN = 1
    const val STOP_STORE_TIP = 2
    const val STOP_STORE_TIP_PRIV = 3
    const val STOP_REPLACE_OLD = 4

    private var random: Int = -1

    private var tag = -1
    fun enableStop(tag: Int) {
        this.tag = tag
    }

    fun randomExit(count: Int, size: Int) {
        if (tag != STOP_NODE_SIGN) return
        if (random == -1) {
            random = Random.nextInt(size - 1) + 1
        }
        if (count >= 1) {
            Timber.e("$random $size $count")
            if (count == random) {
                Timber.e("exit: $count")
                Process.killProcess(Process.myPid())
            }
        }
    }

    fun trouble(tag: Int) {
        if (tag == this.tag) {
            Timber.e("exit tag: $tag")
            Process.killProcess(Process.myPid())
        }
    }
}
