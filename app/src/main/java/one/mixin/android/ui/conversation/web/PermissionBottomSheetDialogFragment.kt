package one.mixin.android.ui.conversation.web

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.fragment_permission.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.loadCircleImage
import one.mixin.android.extension.realSize
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.auth.AuthBottomSheetDialogFragment.Companion.ARGS_SCOPES
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.widget.BottomSheet

class PermissionBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "PermissionBottomSheetDialogFragment"

        private const val ARGS_PERMISSION = "args_permission"
        private const val ARGS_TITLE = "args_title"

        private const val PERMISSION_CAMERA = 0
        private const val PERMISSION_VIDEO = 1
        private fun newInstance(permission: Int, title: String, appName: String? = null, appAvatar: String? = null) =
            PermissionBottomSheetDialogFragment().withArgs {
                putInt(ARGS_SCOPES, permission)
                putString(ARGS_TITLE, title)
                putString(WebBottomSheetDialogFragment.APP_NAME, appName)
                putString(WebBottomSheetDialogFragment.APP_AVATAR, appAvatar)
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
        arguments!!.getString(WebBottomSheetDialogFragment.APP_NAME)
    }
    private val appAvatar: String? by lazy {
        arguments!!.getString(WebBottomSheetDialogFragment.APP_AVATAR)
    }

    private val miniHeight by lazy {
        context!!.realSize().y / 2
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_permission, null)
        dialog as BottomSheet
        dialog.setCustomView(contentView)
        dialog.setCustomViewHeight(miniHeight)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (permission == PERMISSION_CAMERA) {
            contentView.info.setText(R.string.permission_camera)
        } else {
            contentView.info.setText(R.string.permission_video)
        }
        contentView.authorization.setOnClickListener {
            grantedAction?.invoke()
            dismiss()
        }
        contentView.refuse.setOnClickListener {
            dismiss()
        }
        if (!appAvatar.isNullOrBlank()) {
            contentView.avatar.layoutParams.width = requireContext().dpToPx(36f)
            contentView.avatar.loadCircleImage(appAvatar)
        } else {
            contentView.avatar.layoutParams.width = requireContext().dpToPx(0f)
        }
        if (!appName.isNullOrBlank()) {
            contentView.name.text = appName
        } else {
            contentView.name.text = title
        }
    }

    private var grantedAction: (() -> Unit)? = null

    public fun setGrantedAction(action: () -> Unit): PermissionBottomSheetDialogFragment {
        grantedAction = action
        return this
    }
}
