package one.mixin.android.ui.group

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.synthetic.main.fragment_invite.*
import kotlinx.android.synthetic.main.view_group_link_bottom.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.QrBottomSheetDialogFragment
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.ui.group.InviteActivity.Companion.ARGS_ID
import one.mixin.android.util.ErrorHandler
import one.mixin.android.widget.BottomSheet
import javax.inject.Inject

class InviteFragment : BaseFragment() {
    companion object {
        const val TAG = "InviteFragment"

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

    private fun refreshUrl() {
        inviteViewModel.findConversation(conversationId).autoDisposable(scopeProvider).subscribe({
            if (it.isSuccess && it.data != null) {
                inviteViewModel.updateCodeUrl(conversationId, it.data!!.codeUrl)
            }
        }, { ErrorHandler.handleError(it) })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        title_view.right_animator.setOnClickListener { showBottom() }
        inviteViewModel.getConversation(conversationId).observe(this, Observer {
            notNullElse(it, { c ->
                name_tv.text = c.name
                avatar.setGroup(c.iconUrl)
                val url = c.codeUrl
                invite_link.text = url
                copy_ll.setOnClickListener {
                    context?.getClipboardManager()?.primaryClip = ClipData.newPlainText(null, url)
                    context?.toast(R.string.copy_success)
                }
                share_ll.setOnClickListener {
                    val sendIntent = Intent()
                    sendIntent.action = Intent.ACTION_SEND
                    sendIntent.putExtra(Intent.EXTRA_TEXT, url)
                    sendIntent.type = "text/plain"
                    startActivity(Intent.createChooser(sendIntent, resources.getText(R.string.invite_title)))
                }
                qr_ll.setOnClickListener {
                    QrBottomSheetDialogFragment.newInstance(QrBottomSheetDialogFragment.TYPE_GROUP_QR, groupCode = url, groupIcon = c.iconUrl)
                        .showNow(requireFragmentManager(), QrBottomSheetDialogFragment.TAG)
                }
            }, {
                context?.toast(R.string.invite_invalid)
            })
        })

        refreshUrl()
    }

    private fun showBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        val view = View.inflate(ContextThemeWrapper(requireActivity(), R.style.Custom), R.layout.view_group_link_bottom, null)
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        view.forward.setOnClickListener {
            ForwardActivity.show(requireContext(), invite_link.text.toString())
            bottomSheet.dismiss()
        }
        view.revoke.setOnClickListener {
            inviteViewModel.rotate(conversationId).autoDisposable(scopeProvider).subscribe({
                if (it.isSuccess) {
                    val cr = it.data!!
                    invite_link.text = cr.codeUrl
                    inviteViewModel.updateCodeUrl(cr.conversationId, cr.codeUrl)
                }
            }, {
                ErrorHandler.handleError(it)
            })
            bottomSheet.dismiss()
        }
        view.cancel.setOnClickListener { bottomSheet.dismiss() }
        bottomSheet.show()
    }
}
