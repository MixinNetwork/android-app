package one.mixin.android.ui.group

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_new_group.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.event.AvatarEvent
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.Session
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.User
import one.mixin.android.vo.toUser
import org.jetbrains.anko.textColor
import java.util.UUID
import javax.inject.Inject

class NewGroupFragment : BaseFragment() {
    companion object {
        const val TAG = "NewGroupFragment"
        private const val ARGS_USERS = "args_users"

        const val POS_TEXT = 0
        const val POS_PB = 1

        fun newInstance(users: ArrayList<User>): NewGroupFragment {
            val fragment = NewGroupFragment()
            fragment.withArgs {
                putParcelableArrayList(ARGS_USERS, users)
            }
            return fragment
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val groupViewModel: GroupViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(GroupViewModel::class.java)
    }

    private val sender: User by lazy { Session.getAccount()!!.toUser() }
    private var dialog: Dialog? = null
    private val users: List<User> by lazy { arguments!!.getParcelableArrayList<User>(ARGS_USERS)!! }
    private val conversationId = UUID.randomUUID().toString()
    private var disposable: Disposable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_new_group, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener {
            name_et.hideKeyboard()
            activity?.onBackPressed()
        }
        enableCreate(false)
        name_et.addTextChangedListener(mWatcher)
        name_et.showKeyboard()
        participant_tv.text = getString(R.string.title_participants, users.size + 1)
        create_animator.setOnClickListener { createGroup() }

        val allUserList = arrayListOf<User>().apply {
            add(sender)
            if (users.size <= 3) {
                addAll(users)
            } else {
                addAll(users.subList(0, 3))
            }
        }

        groupViewModel.generateAvatar(conversationId, allUserList)
    }

    override fun onResume() {
        super.onResume()
        if (disposable == null || disposable?.isDisposed == true) {
            disposable = RxBus.listen(AvatarEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    avatar.setGroup(it.url)
                }
        }
    }

    override fun onPause() {
        super.onPause()
        if (disposable?.isDisposed == false) {
            disposable?.dispose()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dialog?.dismiss()
    }

    private fun createGroup() {
        val conversation = groupViewModel.createGroupConversation(conversationId, name_et.text.toString(),
            "", null, users, sender)
        create_animator.displayedChild = POS_PB
        val liveData = groupViewModel.getConversationStatusById(conversation.conversationId)
        liveData.observe(this, Observer { c ->
            if (c != null) {
                when {
                    c.status == ConversationStatus.START.ordinal -> {
                        if (dialog == null) {
                            dialog = indeterminateProgressDialog(message = R.string.pb_dialog_message,
                                title = R.string.group_creating).apply {
                                setCancelable(false)
                            }
                        }
                        dialog?.show()
                    }
                    c.status == ConversationStatus.SUCCESS.ordinal -> {
                        create_animator?.displayedChild = POS_TEXT
                        liveData.removeObservers(this)
                        name_et?.hideKeyboard()
                        dialog?.dismiss()
                        startActivity(Intent(context, MainActivity::class.java))
                        ConversationActivity.show(context!!, conversation.conversationId, null)
                    }
                    c.status == ConversationStatus.FAILURE.ordinal -> {
                        create_animator?.displayedChild = POS_TEXT
                        name_et?.hideKeyboard()
                        dialog?.dismiss()
                        startActivity(Intent(context, MainActivity::class.java))
                    }
                }
            }
        })
        name_et?.hideKeyboard()
    }

    private fun enableCreate(enable: Boolean) {
        if (enable) {
            create_animator.isEnabled = true
            create_animator.background = resources.getDrawable(R.drawable.bg_wallet_blue_btn, null)
            create_animator.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                height = requireContext().dpToPx(72f)
                topMargin = requireContext().dpToPx(32f)
                bottomMargin = 0
            }
            create_tv.textColor = requireContext().getColor(R.color.white)
        } else {
            create_animator.isEnabled = false
            create_animator.background = resources.getDrawable(R.drawable.bg_gray_btn, null)
            create_animator.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                height = requireContext().dpToPx(40f)
                topMargin = requireContext().dpToPx(50f)
                bottomMargin = requireContext().dpToPx(16f)
            }
            create_tv.textColor = requireContext().getColor(R.color.wallet_text_gray)
        }
    }

    private val mWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            if (!s.isNullOrEmpty()) {
                enableCreate(true)
            } else {
                enableCreate(false)
            }
        }
    }
}