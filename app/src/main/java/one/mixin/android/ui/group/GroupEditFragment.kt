package one.mixin.android.ui.group

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.synthetic.main.fragment_group_edit.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.group.GroupFragment.Companion.ARGS_CONVERSATION_ID
import one.mixin.android.util.ErrorHandler
import org.jetbrains.anko.textColorResource
import javax.inject.Inject

class GroupEditFragment : BaseFragment() {
    companion object {
        val TAG = GroupEditFragment::class.java.simpleName
        private const val ARGS_DESC = "args_desc"

        fun newInstance(conversationId: String, desc: String?): GroupEditFragment {
            return GroupEditFragment().withArgs {
                putString(ARGS_CONVERSATION_ID, conversationId)
                putString(ARGS_DESC, desc)
            }
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val groupViewModel: GroupViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(GroupViewModel::class.java)
    }
    private val conversationId: String by lazy {
        arguments!!.getString(ARGS_CONVERSATION_ID)
    }

    private val desc: String? by lazy {
        arguments!!.getString(ARGS_DESC, null)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        layoutInflater.inflate(R.layout.fragment_group_edit, container, false)!!

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener {
            desc_et.hideKeyboard()
            activity?.onBackPressed()
        }
        desc_et.setText(desc)
        desc_et.requestFocus()
        desc_et.selectAll()
        desc_et.showKeyboard()
        title_view.right_tv.textColorResource = R.color.text_gray
        title_view.right_tv.isEnabled = false
        title_view.right_tv.setOnClickListener {
            if (desc_et.text.length > 1024) {
                context?.toast(R.string.error_too_small)
            } else {
                title_view.pb.visibility = View.VISIBLE
                title_view.right_tv.visibility = View.GONE
                groupViewModel.updateGroup(conversationId, desc_et.text.toString()).autoDisposable(scopeProvider).subscribe({
                    if (it.isSuccess) {
                        groupViewModel.updateAnnouncement(conversationId, it.data?.announcement)
                        desc_et.hideKeyboard()
                        fragmentManager!!.popBackStack()
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
