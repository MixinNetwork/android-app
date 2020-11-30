package one.mixin.android.ui.contacts

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import one.mixin.android.Constants.ARGS_USER
import one.mixin.android.R
import one.mixin.android.databinding.FragmentContactBottomSheetBinding
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.vo.User
import one.mixin.android.widget.BottomSheet

class ContactBottomSheetDialog : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "ContactBottomSheetDialog"

        fun newInstance(user: User) = ContactBottomSheetDialog().apply {
            arguments = Bundle().apply {
                putParcelable(ARGS_USER, user)
            }
        }
    }

    private val user: User by lazy { requireArguments().getParcelable(ARGS_USER)!! }

    private var _binding: FragmentContactBottomSheetBinding? = null
    private val binding get() = requireNotNull(_binding)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        _binding = FragmentContactBottomSheetBinding.bind(View.inflate(context, R.layout.fragment_contact_bottom_sheet, null))
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)

        binding.apply {
            index.text = if (user.fullName != null && user.fullName!!.isNotEmpty()) user.fullName!![0].toString() else ""
            name.text = user.fullName
            mobileTv.text = getString(R.string.contact_mobile, user.phone)
            inviteTv.setOnClickListener { openSms(user.phone) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun openSms(mobile: String?) {
        if (mobile == null) {
            return
        }
        val smsUri = Uri.parse("smsto:$mobile")
        val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri)
        smsIntent.putExtra("sms_body", getString(R.string.contact_less_invite_content))
        startActivity(smsIntent)
    }
}
