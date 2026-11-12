package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.getFileNameNoEx
import one.mixin.android.extension.getMediaPath
import one.mixin.android.extension.isUUID
import one.mixin.android.util.clear.CleanNotification
import one.mixin.android.util.getLocalString
import timber.log.Timber

class StorageCleanJob :
    BaseJob(Params(PRIORITY_BACKGROUND).groupBy(GROUP_ID).persist()) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP_ID = "storage_clean"
        var isRunning = false
    }

    override fun onAdded() {
        isRunning = true
    }

    override fun onRun() =
        runBlocking {
            val mediaPath = MixinApplication.appContext.getMediaPath()
            var size = 0L
            CleanNotification.show()
            mediaPath?.listFiles()?.forEach { parentDir ->
                parentDir.listFiles()?.forEach { dir ->
                    val files =
                        dir.listFiles()
                            ?.filter { it.isFile }
                            ?.groupBy { it.name.getFileNameNoEx() }
                            ?.filterKeys { it.isUUID() }
                            .orEmpty()
                    if (files.isNotEmpty()) {
                        val existing = files.keys.chunked(500).flatMapTo(HashSet()) { ids -> messageDao.findExistingIds(ids) }
                        var deleted = false
                        files.forEach { (name, list) ->
                            if (name in existing) return@forEach
                            list.forEach { file ->
                                size += file.length()
                                file.delete()
                                deleted = true
                            }
                        }
                        if (deleted) {
                            CleanNotification.show(getLocalString(MixinApplication.appContext, R.string.deep_cleaning_deleted, size.fileSize()))
                        }
                    }
                }
            }
            Timber.e("delete total: ${size.fileSize()}")
            CleanNotification.cancel()
            isRunning = false
        }
}
