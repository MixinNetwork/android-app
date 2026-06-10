package one.mixin.android.ui.transfer

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentShowConversationBinding
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.isNightMode
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.ConversationMinimal
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class ShowConversationBottomSheetFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "ShowConversationFragment"

        @SuppressLint("StaticFieldLeak")
        private var instant: ShowConversationBottomSheetFragment? = null

        const val ARGS_CONVERSATIONS = "args_conversations"

        fun newInstance(conversations: List<ConversationMinimal>): ShowConversationBottomSheetFragment? {
            try {
                instant?.dismiss()
            } catch (ignored: IllegalStateException) {
            }
            instant = null
            return ShowConversationBottomSheetFragment().apply {
                arguments =
                    Bundle().apply {
                        putParcelableArrayList(
                            ARGS_CONVERSATIONS,
                            arrayListOf<ConversationMinimal>().apply { addAll(conversations) },
                        )
                    }
            }.apply {
                instant = this
            }
        }
    }

    private val conversations by lazy {
        arguments?.getParcelableArrayListCompat(ARGS_CONVERSATIONS, ConversationMinimal::class.java)
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    override fun onDetach() {
        super.onDetach()
        instant = null
    }

    override fun onStart() {
        try {
            super.onStart()
        } catch (ignored: WindowManager.BadTokenException) {
        }
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night),
            )
        }
    }

    private val binding by viewBinding(FragmentShowConversationBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        dialog.window?.let { window ->
            SystemUIManager.lightUI(window, requireContext().isNightMode())
        }
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        binding.recyclerView.adapter = adapter
        binding.close.setOnClickListener {
            dismissNow()
        }
        adapter.conversations = conversations
    }

    var selectListener: ((Boolean, String) -> Unit)? = null
    private val adapter by lazy {
        SelectedAdapter(selectListener)
    }
}
