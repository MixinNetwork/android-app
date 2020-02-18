package one.mixin.android.ui.group

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.collection.ArrayMap
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_group_info.*
import kotlinx.android.synthetic.main.view_group_info_header.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.launch
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.event.ConversationEvent
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.job.ConversationJob.Companion.TYPE_ADD
import one.mixin.android.job.ConversationJob.Companion.TYPE_DELETE
import one.mixin.android.job.ConversationJob.Companion.TYPE_EXIT
import one.mixin.android.job.ConversationJob.Companion.TYPE_MAKE_ADMIN
import one.mixin.android.job.ConversationJob.Companion.TYPE_REMOVE
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshConversationJob
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.group.GroupFragment.Companion.MAX_USER
import one.mixin.android.ui.group.adapter.GroupInfoAdapter
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.User
import one.mixin.android.vo.isGroup

class GroupInfoFragment : BaseFragment() {
    companion object {
        const val TAG = "GroupInfoFragment"

        fun newInstance(conversationId: String): GroupInfoFragment {
            val fragment = GroupInfoFragment()
            val b = Bundle().apply {
                putString(ARGS_CONVERSATION_ID, conversationId)
            }
            fragment.arguments = b
            return fragment
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var jobManager: MixinJobManager

    private val groupViewModel: GroupViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(GroupViewModel::class.java)
    }
    private val adapter by lazy {
        GroupInfoAdapter()
    }

    private val conversationId: String by lazy {
        arguments!!.getString(ARGS_CONVERSATION_ID)!!
    }
    private var self: User? = null
    private var participantsMap: ArrayMap<String, Participant> = ArrayMap()
    private var users = arrayListOf<User>()
    private var dialog: Dialog? = null
    private lateinit var header: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        LayoutInflater.from(context).inflate(R.layout.fragment_group_info, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener {
            search_et.hideKeyboard()
            activity?.onBackPressed()
        }
        header = LayoutInflater.from(context).inflate(R.layout.view_group_info_header, group_info_rv, false)
        adapter.headerView = header
        group_info_rv.adapter = adapter
        adapter.setGroupInfoListener(object : GroupInfoAdapter.GroupInfoListener {
            override fun onAdd() {
                modifyMember(true)
            }

            override fun onClick(name: View, user: User) {
                val choices = mutableListOf<String>()
                choices.add(getString(R.string.group_pop_menu_message, user.fullName))
                choices.add(getString(R.string.group_pop_menu_view, user.fullName))
                var role: String? = null
                self?.let {
                    val p = participantsMap[it.userId]
                    p?.let { role = p.role }
                }
                if (role == ParticipantRole.OWNER.name) {
                    val userRole = (participantsMap[user.userId] as Participant).role
                    if (userRole == ParticipantRole.ADMIN.name) {
                        choices.add(getString(R.string.group_pop_menu_remove, user.fullName))
                    } else {
                        choices.add(getString(R.string.group_pop_menu_remove, user.fullName))
                        choices.add(getString(R.string.group_pop_menu_make_admin))
                    }
                } else if (role == ParticipantRole.ADMIN.name) {
                    val userRole = (participantsMap[user.userId] as Participant).role
                    if (userRole != ParticipantRole.OWNER.name && userRole != ParticipantRole.ADMIN.name) {
                        choices.add(getString(R.string.group_pop_menu_remove, user.fullName))
                    }
                }
                alertDialogBuilder()
                    .setItems(choices.toTypedArray()) { _, which ->
                        when (which) {
                            0 -> {
                                openChat(user)
                            }
                            1 -> {
                                UserBottomSheetDialogFragment.newInstance(user, conversationId).showNow(parentFragmentManager, UserBottomSheetDialogFragment.TAG)
                            }
                            2 -> {
                                showConfirmDialog(getString(R.string.group_info_remove_tip,
                                    user.fullName, adapter.conversation?.name), TYPE_REMOVE, user = user)
                            }
                            3 -> {
                                showPb()
                                groupViewModel.makeAdmin(conversationId, user)
                            }
                        }
                    }.show()
            }

            override fun onLongClick(name: View, user: User): Boolean {
                val popMenu = PopupMenu(activity!!, name)
                val c = adapter.conversation
                if (c == null || !c.isGroup()) {
                    return false
                }
                var role: String? = null
                self?.let {
                    val p = participantsMap[it.userId]
                    p?.let { role = p.role }
                }
                if (role == ParticipantRole.OWNER.name) {
                    val userRole = (participantsMap[user.userId] as Participant).role
                    if (userRole == ParticipantRole.ADMIN.name) {
                        popMenu.menuInflater.inflate(R.menu.group_item_admin, popMenu.menu)
                    } else {
                        popMenu.menuInflater.inflate(R.menu.group_item_owner, popMenu.menu)
                    }
                    popMenu.menu.findItem(R.id.remove).title = getString(R.string.group_pop_menu_remove, user.fullName)
                } else if (role == ParticipantRole.ADMIN.name) {
                    val userRole = (participantsMap[user.userId] as Participant).role
                    if (userRole == ParticipantRole.OWNER.name || userRole == ParticipantRole.ADMIN.name) {
                        popMenu.menuInflater.inflate(R.menu.group_item_simple, popMenu.menu)
                    } else {
                        popMenu.menuInflater.inflate(R.menu.group_item_admin, popMenu.menu)
                        popMenu.menu.findItem(R.id.remove).title =
                            getString(R.string.group_pop_menu_remove, user.fullName)
                    }
                } else {
                    popMenu.menuInflater.inflate(R.menu.group_item_simple, popMenu.menu)
                }
                popMenu.menu.findItem(R.id.message).title = getString(R.string.group_pop_menu_message, user.fullName)
                popMenu.menu.findItem(R.id.view).title = getString(R.string.group_pop_menu_view, user.fullName)
                popMenu.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.message -> {
                            openChat(user)
                        }
                        R.id.view -> {
                            UserBottomSheetDialogFragment.newInstance(user, conversationId)
                                .showNow(parentFragmentManager, UserBottomSheetDialogFragment.TAG)
                        }
                        R.id.remove -> {
                            showConfirmDialog(getString(R.string.group_info_remove_tip,
                                user.fullName, adapter.conversation?.name),
                                TYPE_REMOVE, user = user)
                        }
                        R.id.admin -> {
                            showPb()
                            groupViewModel.makeAdmin(conversationId, user)
                        }
                    }
                    return@setOnMenuItemClickListener true
                }
                popMenu.show()
                return true
            }
        })

        groupViewModel.getGroupParticipantsLiveData(conversationId).observe(viewLifecycleOwner, Observer { u ->
            u?.let {
                var role: String? = null
                self?.let {
                    val p = participantsMap[it.userId]
                    p?.let { role = p.role }
                }
                users.clear()
                users.addAll(u)

                header.add_rl.visibility = if (it.isEmpty() || it.size >= MAX_USER || role == null ||
                    (role != ParticipantRole.OWNER.name && role != ParticipantRole.ADMIN.name))
                    GONE else VISIBLE

                lifecycleScope.launch {
                    if (!isAdded) return@launch

                    val participants = groupViewModel.getRealParticipants(conversationId)
                    participantsMap.clear()
                    for (item in it) {
                        participants.forEach {
                            if (item.userId == it.userId) {
                                participantsMap[item.userId] = it
                                return@forEach
                            }
                        }
                    }
                    adapter.participantsMap = participantsMap
                    filter()
                }
            }
        })

        groupViewModel.getConversationById(conversationId).observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.conversation = it
            }
        })

        groupViewModel.findSelf().observe(viewLifecycleOwner, Observer {
            self = it
            adapter.self = it
        })

        RxBus.listen(ConversationEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(stopScope)
            .subscribe {
                if (it.type == TYPE_MAKE_ADMIN || it.type == TYPE_REMOVE || it.type == TYPE_EXIT) {
                    dialog?.dismiss()
                }
            }

        search_et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                keyword = s.toString()
            }
        })

        jobManager.addJobInBackground(RefreshConversationJob(conversationId))
    }

    private var keyword: String = ""
        set(value) {
            field = value
            filter()
        }

    private fun filter() {
        adapter.data = if (keyword.isNotBlank()) {
            users.filter {
                it.fullName?.contains(keyword, true) == true || it.identityNumber.contains(keyword, true)
            }.sortedByDescending { it.fullName == keyword || it.identityNumber == keyword }
        } else {
            users
        }
    }

    private fun openChat(user: User) {
        context?.let { ctx -> ConversationActivity.show(ctx, null, user.userId) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dialog?.dismiss()
    }

    private fun showConfirmDialog(message: String, type: Int, user: User? = null) {
        alertDialogBuilder()
            .setMessage(message)
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                showPb()
                when (type) {
                    TYPE_REMOVE -> {
                        groupViewModel.modifyGroupMembers(conversationId, listOf(user!!), TYPE_REMOVE)
                    }
                    TYPE_EXIT -> {
                        groupViewModel.exitGroup(conversationId)
                    }
                    TYPE_DELETE -> {
                        groupViewModel.deleteMessageByConversationId(conversationId)
                        startActivity(Intent(context, MainActivity::class.java))
                    }
                }
                dialog.dismiss()
            }.show()
    }

    private fun showPb() {
        if (dialog == null) {
            dialog = indeterminateProgressDialog(message = getString(R.string.pb_dialog_message)).apply {
                setCancelable(false)
            }
        }
        dialog!!.show()
    }

    private fun modifyMember(isAdd: Boolean) {
        val list = arrayListOf<User>()
        adapter.data.let {
            list += it!!
        }
        activity?.addFragment(this@GroupInfoFragment,
            GroupFragment.newInstance(if (isAdd) TYPE_ADD else TYPE_REMOVE, list, conversationId), GroupFragment.TAG)
    }
}
