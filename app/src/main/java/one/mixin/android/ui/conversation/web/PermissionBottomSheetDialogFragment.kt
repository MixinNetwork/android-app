package one.mixin.android.ui.conversation.web

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentPermissionBinding
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.loadCircleImage
import one.mixin.android.extension.realSize
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.auth.AuthBottomSheetDialogFragment.Companion.ARGS_SCOPES
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class PermissionBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "PermissionBottomSheetDialogFragment"

        private const val ARGS_PERMISSION = "args_permission"
        private const val ARGS_TITLE = "args_title"
        private const val ARGS_NAME = "args_name"
        private const val ARGS_AVATAR = "args_avatar"

        private const val PERMISSION_CAMERA = 0
        private const val PERMISSION_VIDEO = 1
        private fun newInstance(permission: Int, title: String, appName: String? = null, appAvatar: String? = null) =
            PermissionBottomSheetDialogFragment().withArgs {
                putInt(ARGS_SCOPES, permission)
                putString(ARGS_TITLE, title)
                putString(ARGS_NAME, appName)
                putString(ARGS_AVATAR, appAvatar)
            }

        fun requestCamera(title: String, appName: String? = null, appAvatar: String? = null): PermissionBottomSheetDialogFragment {
            return newInstance(PERMISSION_CAMERA, title, appName, appAvatar)
        }

        fun requestVideo(title: String, appName: String? = null, appAvatar: String? = null): PermissionBottomSheetDialogFragment {
            return newInstance(PERMISSION_VIDEO, title, appName, appAvatar)
        }
    }

    private val permission by lazy {
        arguments?.getInt(ARGS_PERMISSION)
    }

    private val title by lazy {
        arguments?.getString(ARGS_TITLE)
    }

    private val appName: String? by lazy {
        requireArguments().getString(ARGS_NAME)
    }
    private val appAvatar: String? by lazy {
        requireArguments().getString(ARGS_AVATAR)
    }

    private val miniHeight by lazy {
        requireContext().realSize().y / 2
    }

    private var isHandle: Boolean = false

    private val binding by viewBinding(FragmentPermissionBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        dialog as BottomSheet
        dialog.setCustomView(contentView)
        dialog.setCustomViewHeight(miniHeight)

        isHandle = false
        if (permission == PERMISSION_CAMERA) {
            binding.info.setText(R.string.permission_camera)
        } else {
            binding.info.setText(R.string.permission_video)
        }
        binding.authorization.setOnClickListener {
            grantedAction?.invoke()
            isHandle = true
            dismiss()
        }
        binding.refuse.setOnClickListener {
            dismiss()
        }
        if (!appAvatar.isNullOrBlank()) {
            binding.avatar.layoutParams.width = requireContext().dpToPx(36f)
            binding.avatar.loadCircleImage(appAvatar)
        } else {
            binding.avatar.layoutParams.width = requireContext().dpToPx(0f)
        }
        if (!appName.isNullOrBlank()) {
            binding.name.text = appName
        } else {
            binding.name.text = title
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!isHandle) {
            cancelAction?.invoke()
        }
    }

    private var grantedAction: (() -> Unit)? = null

    fun setGrantedAction(action: () -> Unit): PermissionBottomSheetDialogFragment {
        grantedAction = action
        return this
    }

    private var cancelAction: (() -> Unit)? = null

    fun setCancelAction(action: () -> Unit): PermissionBottomSheetDialogFragment {
        cancelAction = action
        return this
    }
}
