package one.mixin.android.widget.markdown

import android.content.Context
import io.noties.markwon.Markwon
import io.noties.markwon.core.SimplePlugin

var simpleMarkwon: Markwon? = null

fun getSimpleMarkwon(context: Context): Markwon {
    return simpleMarkwon ?: Markwon.builderNoCore(context).usePlugin(SimplePlugin.create()).build().apply {
        simpleMarkwon = this
    }
}
