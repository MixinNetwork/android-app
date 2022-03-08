package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.MixinApplication
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putStringSet

class RefreshExternalSchemeJob : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).requireNetwork()
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshExternalSchemeJob"
        const val PREF_REFRESH_EXTERNAL_SCHEMES = "pref_refresh_external_schemes"
        const val PREF_EXTERNAL_SCHEMES = "pref_external_schemes"
    }

    override fun onRun() = runBlocking {
        val response = accountService.getExternalSchemes()
        if (response.isSuccess && response.data != null) {
            val schemes = response.data as Set<String>
            MixinApplication.appContext.defaultSharedPreferences.putStringSet(PREF_EXTERNAL_SCHEMES, schemes)
        }
    }
}