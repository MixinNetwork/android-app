package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.Constants.ARGS_USER
import one.mixin.android.R
import one.mixin.android.databinding.FragmentViewEmergencyContactBinding
import one.mixin.android.databinding.ViewTitleBinding
import one.mixin.android.extension.highlightLinkText
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.withArgs
import one.mixin.android.vo.User

@AndroidEntryPoint
class ViewEmergencyContactFragment : BaseSettingFragment<FragmentViewEmergencyContactBinding>() {
    companion object {
        const val TAG = "ViewEmergencyContactFragment"

        fun newInstance(user: User) = ViewEmergencyContactFragment().withArgs {
            putParcelable(ARGS_USER, user)
        }
    }

    private val user: User by lazy { requireArguments().getParcelable(ARGS_USER)!! }

    override fun bind(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentViewEmergencyContactBinding.inflate(inflater, container, false).apply {
            _titleBinding = ViewTitleBinding.bind(titleView)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        titleBinding.leftIb.setOnClickListener {
            activity?.onBackPressed()
        }
        titleBinding.rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.EMERGENCY) }
        binding.apply {
            avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
            nameTv.text = user.fullName
            idTv.text = getString(R.string.contact_mixin_id, user.identityNumber)

            val url = Constants.HelpLink.EMERGENCY
            val target = getString(R.string.setting_emergency)
            val desc = getString(R.string.setting_emergency_desc)
            tipTv.highlightLinkText(desc, arrayOf(target), arrayOf(url))
        }
    }
}
