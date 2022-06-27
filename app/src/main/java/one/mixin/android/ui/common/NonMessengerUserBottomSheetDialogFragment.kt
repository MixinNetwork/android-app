package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.Constants.ARGS_USER
import one.mixin.android.Constants.Colors.LINK_COLOR
import one.mixin.android.databinding.FragmentNonMessengerUserBottomSheetBinding
import one.mixin.android.extension.openAsUrlOrWeb
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.User
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.linktext.AutoLinkMode

@AndroidEntryPoint
class NonMessengerUserBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "NonMessengerUserBottomSheetDialogFragment"

        @SuppressLint("StaticFieldLeak")
        private var instant: NonMessengerUserBottomSheetDialogFragment? = null
        fun newInstance(
            user: User,
            conversationId: String? = null
        ): NonMessengerUserBottomSheetDialogFragment {
            try {
                instant?.dismiss()
            } catch (ignored: IllegalStateException) {
            }
            instant = null
            return NonMessengerUserBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGS_USER, user)
                    putString(ARGS_CONVERSATION_ID, conversationId)
                }
                instant = this
            }
        }
    }

    private val binding by viewBinding(FragmentNonMessengerUserBottomSheetBinding::inflate)

    private lateinit var user: User
    // bot need conversation id
    private var conversationId: String? = null

    @SuppressLint("RestrictedApi", "ClickableViewAccessibility")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        user = requireNotNull(requireArguments().getParcelable(ARGS_USER))
        conversationId = requireArguments().getString(ARGS_CONVERSATION_ID)
        binding.apply {
            title.rightIv.setOnClickListener { dismiss() }
            detailTv.movementMethod = LinkMovementMethod()
            detailTv.addAutoLinkMode(AutoLinkMode.MODE_URL)
            detailTv.setUrlModeColor(LINK_COLOR)
            detailTv.setAutoLinkOnClickListener { _, url ->
                url.openAsUrlOrWeb(requireContext(), conversationId, parentFragmentManager, lifecycleScope)
                dismiss()
            }
            bottomViewModel.refreshUser(user.userId, true)
            bottomViewModel.findUserById(user.userId).observe(
                this@NonMessengerUserBottomSheetDialogFragment,
                Observer { u ->
                    if (u == null) return@Observer

                    binding.avatar.setInfo(u.fullName, u.avatarUrl, u.userId)
                    binding.name.text = u.fullName
                    if (u.biography.isNotEmpty()) {
                        binding.detailTv.text = u.biography
                        binding.detailTv.isVisible = true
                    } else {
                        binding.detailTv.isVisible = false
                    }
                }
            )
        }
    }
}
