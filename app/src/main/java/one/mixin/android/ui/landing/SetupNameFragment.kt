package one.mixin.android.ui.landing

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.fragment.app.viewModels
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.MIXIN_BOTS_USER_ID
import one.mixin.android.Constants.MIXIN_BOTS_USER_NAME
import one.mixin.android.Constants.MIXIN_DATA_USER_ID
import one.mixin.android.Constants.MIXIN_DATA_USER_NAME
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.databinding.FragmentSetupNameBinding
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.showKeyboard
import one.mixin.android.job.InitializeJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Account
import one.mixin.android.vo.toUser
import javax.inject.Inject

@AndroidEntryPoint
class SetupNameFragment : BaseFragment(R.layout.fragment_setup_name) {

    private val mobileViewModel by viewModels<MobileViewModel>()
    private val binding by viewBinding(FragmentSetupNameBinding::bind)

    @Inject
    lateinit var jobManager: MixinJobManager

    companion object {
        fun newInstance() = SetupNameFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MixinApplication.get().isOnline.set(true)
        binding.apply {
            nameFab.visibility = GONE
            nameFab.setOnClickListener {
                nameFab.show()
                nameCover.visibility = VISIBLE
                val accountUpdateRequest = AccountUpdateRequest(nameEt.text.toString())
                mobileViewModel.update(accountUpdateRequest)
                    .autoDispose(stopScope).subscribe(
                        { r: MixinResponse<Account> ->
                            nameFab.hide()
                            nameCover.visibility = INVISIBLE
                            if (!r.isSuccess) {
                                ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
                                return@subscribe
                            }
                            r.data?.let { data ->
                                Session.storeAccount(data)
                                mobileViewModel.insertUser(data.toUser())
                            }

                            nameEt.hideKeyboard()
                            startActivity(Intent(context, MainActivity::class.java))
                            initializeBots()
                            activity?.finish()
                        },
                        { t: Throwable ->
                            nameFab.hide()
                            nameCover.visibility = INVISIBLE
                            ErrorHandler.handleError(t)
                        },
                    )
            }
            nameEt.addTextChangedListener(mWatcher)
            nameCover.isClickable = true

            nameEt.post {
                nameEt.requestFocus()
                nameEt.showKeyboard()
            }
        }
    }

    private fun initializeBots() {
        if (Session.getAccount()?.phone?.run { startsWith("+971") || startsWith("+91") } == true) {
            jobManager.addJobInBackground(InitializeJob(MIXIN_BOTS_USER_ID, MIXIN_BOTS_USER_NAME))
            jobManager.addJobInBackground(InitializeJob(MIXIN_DATA_USER_ID, MIXIN_DATA_USER_NAME))
        }
    }

    private fun handleEditView(str: String) {
        binding.apply {
            nameEt.setSelection(nameEt.text.toString().length)
            if (str.isNotBlank()) {
                nameFab.visibility = VISIBLE
            } else {
                nameFab.visibility = INVISIBLE
            }
        }
    }

    private val mWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            handleEditView(s.toString())
        }
    }
}
