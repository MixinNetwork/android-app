package one.mixin.android.ui.landing

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.uber.autodispose.autoDispose
import kotlinx.android.synthetic.main.fragment_setup_name.*
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.showKeyboard
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.vo.Account
import one.mixin.android.vo.toUser
import javax.inject.Inject

class SetupNameFragment : BaseFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val mobileViewModel: MobileViewModel by viewModels { viewModelFactory }

    companion object {
        fun newInstance() = SetupNameFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_setup_name, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MixinApplication.get().onlining.set(true)
        name_fab.visibility = GONE
        name_fab.setOnClickListener {
            name_fab.show()
            name_cover.visibility = VISIBLE
            val accountUpdateRequest = AccountUpdateRequest(name_et.text.toString())
            mobileViewModel.update(accountUpdateRequest)
                .autoDispose(stopScope).subscribe(
                    { r: MixinResponse<Account> ->
                        name_fab?.hide()
                        name_cover?.visibility = INVISIBLE
                        if (!r.isSuccess) {
                            ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
                            return@subscribe
                        }
                        r.data?.let { data ->
                            Session.storeAccount(data)
                            mobileViewModel.insertUser(data.toUser())
                        }

                        name_et?.hideKeyboard()
                        startActivity(Intent(context, MainActivity::class.java))
                        activity?.finish()
                    },
                    { t: Throwable ->
                        name_fab?.hide()
                        name_cover?.visibility = INVISIBLE
                        ErrorHandler.handleError(t)
                    }
                )
        }
        name_et.addTextChangedListener(mWatcher)
        name_cover.isClickable = true

        name_et.post {
            name_et?.requestFocus()
            name_et?.showKeyboard()
        }
    }

    private fun handleEditView(str: String) {
        name_et.setSelection(name_et.text.toString().length)
        if (str.isNotBlank()) {
            name_fab.visibility = VISIBLE
        } else {
            name_fab.visibility = INVISIBLE
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
