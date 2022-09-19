package one.mixin.android.job

import com.birbit.android.jobqueue.Params

class CleanCacheJob() :
    BaseJob(Params(PRIORITY_BACKGROUND).addTags(GROUP).groupBy("clean_cache").persist()) {

    companion object {
        private const val serialVersionUID: Long = 1L
        const val PREF_CLEAN_CACHE_SCHEMES = "pref_clean_cache_schemes"
        const val GROUP = "CleanCacheJob"
    }

    override fun onRun() {
        applicationContext.cacheDir.listFiles()?.forEach { f ->
            if (f.isFile && f.name.startsWith("attachment") && f.name.endsWith("tmp")) {
                f.delete()
            }
        }
    }
}
