package one.mixin.android.ui.forward

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.replaceFragment
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.util.ShareHelper
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.ForwardAction
import java.io.File
import java.util.UUID

@AndroidEntryPoint
class ShareActivity : BlazeBaseActivity() {
    private var stagingDirectory: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemUIManager.setSafePadding(window, colorFromAttribute(R.attr.bg_white), imePadding = true)
        setContentView(R.layout.activity_contact)
        if (Session.getAccount() == null) {
            toast(R.string.Not_logged_in)
            finish()
            return
        }
        val restored = supportFragmentManager.findFragmentByTag(ForwardFragment.TAG) != null
        if (!restored) {
            savedInstanceState?.getString(STAGING_DIRECTORY)?.let {
                File(File(cacheDir, ShareHelper.STAGING_DIRECTORY), it).deleteRecursively()
            }
        }
        val directoryName = savedInstanceState?.getString(STAGING_DIRECTORY)?.takeIf { restored } ?: UUID.randomUUID().toString()
        val directory = File(File(cacheDir, ShareHelper.STAGING_DIRECTORY), directoryName)
        stagingDirectory = directory
        if (restored) return

        lifecycleScope.launch {
            try {
                val messages = withContext(Dispatchers.IO) {
                    ShareHelper.get().generateForwardMessageList(this@ShareActivity, intent, directory)
                }
                require(!messages.isNullOrEmpty())
                val shortcut = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    intent.getStringExtra(Intent.EXTRA_SHORTCUT_ID)
                } else {
                    null
                }
                replaceFragment(
                    ForwardFragment.newInstance(messages, ForwardAction.System(shortcut, getString(R.string.Share))),
                    R.id.container,
                    ForwardFragment.TAG,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                toast(R.string.Share_error)
                finish()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STAGING_DIRECTORY, stagingDirectory?.name)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) stagingDirectory?.deleteRecursively()
    }

    companion object {
        private const val STAGING_DIRECTORY = "staging_directory"
    }
}
