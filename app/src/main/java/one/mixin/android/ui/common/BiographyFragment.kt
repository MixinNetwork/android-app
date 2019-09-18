package one.mixin.android.ui.common

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.uber.autodispose.autoDisposable
import kotlinx.android.synthetic.main.fragment_group_edit.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.contacts.ContactViewModel
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.vo.Account
import org.jetbrains.anko.textColorResource
import javax.inject.Inject

class BiographyFragment : BaseFragment() {
    companion object {
        val TAG = BiographyFragment::class.java.simpleName

        fun newInstance(): BiographyFragment {
            return BiographyFragment().withArgs {
            }
        }
    }

    private val desc: String? by lazy {
        Session.getAccount()?.biography
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: ContactViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(ContactViewModel::class.java)
    }

    var callback: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) =
        layoutInflater.inflate(R.layout.fragment_biography_edit, container, false)!!

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener {
            desc_et.hideKeyboard()
            activity?.onBackPressed()
        }
        desc_et.setText(desc)
        desc_et.requestFocus()
        desc_et.showKeyboard()
        desc?.let {
            desc_et.setSelection(it.length)
        }
        title_view.right_tv.textColorResource = R.color.text_gray
        title_view.right_tv.isEnabled = false
        title_view.right_tv.setOnClickListener {
            if (desc_et.text.length > 140) {
                context?.toast(R.string.group_edit_too_long)
            } else {
                title_view.pb.visibility = View.VISIBLE
                title_view.right_tv.visibility = View.GONE
                viewModel.update(AccountUpdateRequest(biography = desc_et.text.toString()))
                    .autoDisposable(stopScope).subscribe({ r: MixinResponse<Account> ->
                        if (!isAdded) return@subscribe
                        if (r.isSuccess) {
                            Session.storeAccount(r.data!!)
                            desc_et.hideKeyboard()
                            fragmentManager!!.popBackStack()
                            callback?.invoke()
                        } else {
                            title_view.pb.visibility = View.GONE
                            title_view.right_tv.visibility = View.VISIBLE
                        }
                    }, {
                        title_view.pb.visibility = View.GONE
                        title_view.right_tv.visibility = View.VISIBLE
                        ErrorHandler.handleError(it)
                    })
            }
        }
        desc_et.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val enabled = s?.toString() != desc
                title_view.right_tv.isEnabled = enabled
                title_view.right_tv.textColorResource = if (enabled) {
                    R.color.selector_bn
                } else {
                    R.color.text_gray
                }
            }
        })
    }
}
