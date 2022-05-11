package one.mixin.android.ui.contacts

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.fragment.app.viewModels
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAddPeopleBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.common.profile.ProfileBottomSheetDialogFragment
import one.mixin.android.ui.common.showUserBottom
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.isValidNumber
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.Keyboard
import timber.log.Timber
import java.util.Locale

@AndroidEntryPoint
class AddPeopleFragment : BaseFragment(R.layout.fragment_add_people) {

    companion object {
        const val TAG = "AddPeopleFragment"
        val keys = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "+", "0", "")
        const val POS_SEARCH = 0
        const val POS_PROGRESS = 1

        fun newInstance(): AddPeopleFragment {
            return AddPeopleFragment()
        }
    }

    private val contactsViewModel by viewModels<ContactViewModel>()
    private val binding by viewBinding(FragmentAddPeopleBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressed()
            }
            val account = Session.getAccount()
            if (account != null) {
                tipTv.text = getString(R.string.my_mixin_id, account.identityNumber)
            }
            searchEt.addTextChangedListener(mWatcher)
            searchEt.showSoftInputOnFocus = false
            searchEt.isClickable = true
            searchEt.requestFocus()
            keyboard.setKeyboardKeys(keys)
            keyboard.setOnClickKeyboardListener(mKeyboardListener)
            keyboard.animate().translationY(0f).start()

            searchTv.setOnClickListener {
                searchAnimator.displayedChild = POS_PROGRESS
                searchTv.isEnabled = false
                contactsViewModel.search(searchEt.text.toString()).autoDispose(stopScope).subscribe(
                    { r ->
                        searchAnimator.displayedChild = POS_SEARCH
                        searchTv.isEnabled = true
                        when {
                            r.isSuccess -> r.data?.let { data ->
                                if (data.userId == Session.getAccountId()) {
                                    ProfileBottomSheetDialogFragment.newInstance().showNow(
                                        parentFragmentManager,
                                        UserBottomSheetDialogFragment.TAG
                                    )
                                } else {
                                    contactsViewModel.insertUser(user = data)
                                    showUserBottom(parentFragmentManager, data)
                                }
                            }
                            r.errorCode == ErrorHandler.NOT_FOUND -> toast(R.string.User_not_found)
                            else -> ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
                        }
                    },
                    { t: Throwable ->
                        searchAnimator.displayedChild = POS_SEARCH
                        searchTv.isEnabled = true
                        ErrorHandler.handleError(t)
                    }
                )
            }
        }
    }

    private fun valid(number: String): Boolean {
        if (number.startsWith("+")) {
            val phoneUtil = PhoneNumberUtil.getInstance()
            val validationResult = isValidNumber(phoneUtil, number, Locale.getDefault().country)
            return validationResult.first
        }
        if (number.length >= 2) {
            return true
        }
        return false
    }

    private val mKeyboardListener: Keyboard.OnClickKeyboardListener = object : Keyboard.OnClickKeyboardListener {
        override fun onKeyClick(position: Int, value: String) {
            context?.tickVibrate()
            if (viewDestroyed()) {
                return
            }
            binding.apply {
                val editable = searchEt.text
                val start = searchEt.selectionStart
                val end = searchEt.selectionEnd

                try {
                    if (position == 11) {
                        if (editable.isEmpty()) return

                        if (start == end) {
                            if (start == 0) {
                                searchEt.text.delete(0, end)
                            } else {
                                searchEt.text.delete(start - 1, end)
                            }
                            if (start > 0) {
                                searchEt.setSelection(start - 1)
                            }
                        } else {
                            searchEt.text.delete(start, end)
                            searchEt.setSelection(start)
                        }
                    } else {
                        searchEt.text = editable.insert(start, value)
                        searchEt.setSelection(start + 1)
                    }
                } catch (e: IndexOutOfBoundsException) {
                    Timber.w(e)
                }
            }
        }

        override fun onLongClick(position: Int, value: String) {
            context?.clickVibrate()
            if (viewDestroyed()) {
                return
            }
            binding.apply {
                val editable = searchEt.text
                if (position == 11) {
                    if (editable.isEmpty()) return

                    searchEt.text.clear()
                } else {
                    val start = searchEt.selectionStart
                    searchEt.text = editable.insert(start, value)
                    searchEt.setSelection(start + 1)
                }
            }
        }
    }

    private val mWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            if (viewDestroyed()) return

            binding.searchAnimator.visibility = if (valid(s.toString())) VISIBLE else GONE
        }
    }
}
