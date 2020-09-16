package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_view_emergency_contact.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.Constants
import one.mixin.android.Constants.ARGS_USER
import one.mixin.android.R
import one.mixin.android.extension.highlightLinkText
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.vo.User

@AndroidEntryPoint
class ViewEmergencyContactFragment : BaseFragment() {
    companion object {
        const val TAG = "ViewEmergencyContactFragment"

        fun newInstance(user: User) = ViewEmergencyContactFragment().withArgs {
            putParcelable(ARGS_USER, user)
        }
    }

    private val user: User by lazy { requireArguments().getParcelable(ARGS_USER)!! }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_view_emergency_contact, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener {
            activity?.onBackPressed()
        }
        title_view.right_animator.setOnClickListener { context?.openUrl(Constants.HelpLink.EMERGENCY) }
        avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
        name_tv.text = user.fullName
        id_tv.text = getString(R.string.contact_mixin_id, user.identityNumber)

        val url = Constants.HelpLink.EMERGENCY
        val target = getString(R.string.setting_emergency)
        val desc = getString(R.string.setting_emergency_desc)
        tip_tv.highlightLinkText(desc, arrayOf(target), arrayOf(url))
    }
}
