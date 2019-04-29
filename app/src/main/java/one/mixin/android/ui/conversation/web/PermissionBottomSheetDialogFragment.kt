package one.mixin.android.ui.conversation.web

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.fragment_permission.view.*
import one.mixin.android.R
import one.mixin.android.extension.realSize
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.auth.AuthBottomSheetDialogFragment.Companion.ARGS_SCOPES
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.widget.BottomSheet

class PermissionBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "PermissionBottomSheetDialogFragment"

        const val ARGS_PERMISSION = "args_permission"
        private const val PERMISSION_CAMERA = 0
        private const val PERMISSION_VIDEO = 1
        private fun newInstance(permission: Int) =
            PermissionBottomSheetDialogFragment().withArgs {
                putInt(ARGS_SCOPES, permission)
            }

        fun requestCamera(): PermissionBottomSheetDialogFragment {
            return newInstance(PERMISSION_CAMERA)
        }

        fun requestVideo(): PermissionBottomSheetDialogFragment {
            return newInstance(PERMISSION_VIDEO)
        }
    }

    private val permission by lazy {
        arguments?.getInt(ARGS_PERMISSION)
    }

    private val miniHeight by lazy {
        context!!.realSize().y / 2
    }

    private var success = false

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
    }

    private var grantedAction: (() -> Unit)? = null

    public fun setGrantedAction(action: () -> Unit): PermissionBottomSheetDialogFragment {
        grantedAction = action
        return this
    }
}
