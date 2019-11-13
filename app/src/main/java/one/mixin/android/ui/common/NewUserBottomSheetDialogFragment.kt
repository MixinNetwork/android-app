package one.mixin.android.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import kotlinx.android.synthetic.main.fragment_motion_bottom_sheet.view.*
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.Constants.ARGS_USER
import one.mixin.android.R
import one.mixin.android.ui.common.bottom.MotionBottomSheetDialogFragment
import one.mixin.android.vo.User

class NewUserBottomSheetDialogFragment : MotionBottomSheetDialogFragment() {

    companion object {
        const val TAG = "NewUserBottomSheetDialogFragment"
        fun newInstance(user: User, conversationId: String? = null) =
            NewUserBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGS_USER, user)
                    putString(ARGS_CONVERSATION_ID, conversationId)
                }
            }
    }

    private val user: User? by lazy {
        arguments?.getParcelable<User>(ARGS_USER)
    }

    private val conversationId: String? by lazy {
        arguments?.getString(ARGS_CONVERSATION_ID)
    }

    override fun getBottomView(): View {
        return LayoutInflater.from(requireContext()).inflate(R.layout.fragment_user_bottom_menu,null)
    }

    override fun initView(contentView: View) {
        user?.let { user ->
            contentView.user_bottom_name.text = user.fullName
            contentView.user_bottom_number.text =
                getString(R.string.contact_mixin_id, user.identityNumber)
            contentView.user_bottom_avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
        }
    }
}
