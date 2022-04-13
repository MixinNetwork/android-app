package one.mixin.android.ui.group

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentInviteBinding
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.ui.group.InviteActivity.Companion.ARGS_ID
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
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
        requireArguments().getString(ARGS_ID)!!
    }

    private val inviteViewModel by viewModels<InviteViewModel>()

    private val binding by viewBinding(FragmentInviteBinding::bind)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_invite, container, false)

    private fun refreshUrl() {
        inviteViewModel.findConversation(conversationId).autoDispose(stopScope).subscribe(
            {
                if (it.isSuccess && it.data != null) {
                    inviteViewModel.updateCodeUrl(conversationId, it.data!!.codeUrl)
                }
            },
            { ErrorHandler.handleError(it) }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener { activity?.onBackPressed() }

        inviteViewModel.getConversation(conversationId).observe(
            viewLifecycleOwner
        ) {
            it.notNullWithElse(
                { c ->
                    val url = c.codeUrl
                    binding.inviteLink.text = url
                    binding.inviteForward.setOnClickListener {
                        url?.let { ForwardActivity.show(requireContext(), url) }
                    }
                    binding.inviteCopy.setOnClickListener {
                        context?.getClipboardManager()
                            ?.setPrimaryClip(ClipData.newPlainText(null, url))
                        toast(R.string.copied_to_clipboard)
                    }
                    binding.inviteQr.setOnClickListener {
                        InviteQrBottomFragment.newInstance(c.name, c.iconUrl, url)
                            .show(parentFragmentManager, InviteQrBottomFragment.TAG)
                    }
                    binding.inviteShare.setOnClickListener {
                        val sendIntent = Intent()
                        sendIntent.action = Intent.ACTION_SEND
                        sendIntent.putExtra(Intent.EXTRA_TEXT, url)
                        sendIntent.type = "text/plain"
                        startActivity(
                            Intent.createChooser(
                                sendIntent,
                                resources.getText(R.string.group_invite)
                            )
                        )
                    }
                },
                {
                    toast(R.string.invite_invalid)
                }
            )
        }

        binding.inviteRevoke.setOnClickListener {
            inviteViewModel.rotate(conversationId).autoDispose(stopScope).subscribe(
                {
                    if (it.isSuccess) {
                        val cr = it.data!!
                        binding.inviteLink.text = cr.codeUrl
                        inviteViewModel.updateCodeUrl(cr.conversationId, cr.codeUrl)
                    }
                },
                {
                    ErrorHandler.handleError(it)
                }
            )
        }

        refreshUrl()
    }
}
