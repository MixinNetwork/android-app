package one.mixin.android.startup

import android.content.Context
import androidx.startup.Initializer
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Client
import one.mixin.android.BuildConfig

class BugsnagInitializer : Initializer<Client> {
    override fun create(context: Context): Client {
        return Bugsnag.init(context, BuildConfig.BUGSNAG_API_KEY)
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}
