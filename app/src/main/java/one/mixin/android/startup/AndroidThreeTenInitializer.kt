package one.mixin.android.startup

import android.content.Context
import androidx.startup.Initializer
import com.jakewharton.threetenabp.AndroidThreeTen

class AndroidThreeTenInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        return AndroidThreeTen.init(context)
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}
