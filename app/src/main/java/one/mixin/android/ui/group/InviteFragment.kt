package one.mixin.android.ui.group

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.synthetic.main.fragment_invite.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.notNullElse
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.ui.group.InviteActivity.Companion.ARGS_ID
import one.mixin.android.util.ErrorHandler
import org.jetbrains.anko.support.v4.toast
import javax.inject.Inject

class InviteFragment : BaseFragment() {
    companion object {
        val TAG = "InviteFragment"

        fun putBundle(id: String): Bundle {
            return Bundle().apply {
                putString(ARGS_ID, id)
            }
        }

        fun newInstance(bundle: Bundle): InviteFragment {
            val fragment = InviteFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    private val conversationId: String by lazy {
        arguments!!.getString(ARGS_ID)
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val inviteViewModel: InviteViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(InviteViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_invite, container, false)

    private var ifFirst: Boolean = true

    private fun loadUrl() {
        if (ifFirst) {
            inviteViewModel.findConversation(conversationId).autoDisposable(scopeProvider).subscribe({
                if (it.isSuccess && it.data != null) {
                    ifFirst = false
                    inviteViewModel.updateCodeUrl(conversationId, it.data!!.codeUrl)
                }
            }, { ErrorHandler.handleError(it) })
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }

        inviteViewModel.getConversation(conversationId).observe(this, Observer {
            notNullElse(it, {
                if (it.codeUrl.isNullOrBlank()) {
                    loadUrl()
                }
                val url = it.codeUrl
                invite_link.text = url
                invite_forward.setOnClickListener {
                    context?.let {
                        ForwardActivity.show(it, url)
                    }
                }
                invite_copy.setOnClickListener {
                    context?.getClipboardManager()?.primaryClip = ClipData.newPlainText(null, url)
                    toast(R.string.copy_success)
                }
                invite_share.setOnClickListener {
                    val sendIntent = Intent()
                    sendIntent.action = Intent.ACTION_SEND
                    sendIntent.putExtra(Intent.EXTRA_TEXT, url)
                    sendIntent.type = "text/plain"
                    startActivity(Intent.createChooser(sendIntent, resources.getText(R.string.invite_title)))
                }
            }, {
                toast(R.string.invite_invalid)
            })
        })

        invite_revoke.setOnClickListener {
            inviteViewModel.rotate(conversationId).autoDisposable(scopeProvider).subscribe({
                if (it.isSuccess) {
                    val cr = it.data!!
                    invite_link.text = cr.codeUrl
                    inviteViewModel.updateCodeUrl(cr.conversationId, cr.codeUrl)
                }
            }, {
                ErrorHandler.handleError(it)
            })
        }
    }
}
