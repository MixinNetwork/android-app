package one.mixin.android.ui.call

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_group.*
import kotlinx.android.synthetic.main.fragment_group_users_bottom_sheet.view.*
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.R
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.vo.User
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SearchView

class GroupUsersBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "GroupUsersBottomSheetDialogFragment"

        fun newInstance(
            conversationId: String
        ) = GroupUsersBottomSheetDialogFragment().apply {
            arguments = Bundle().apply {
                ARGS_CONVERSATION_ID to conversationId
            }
        }
    }

    private val conversationId: String by lazy {
        requireArguments().getString(ARGS_CONVERSATION_ID)!!
    }

    private var users: List<User>? = null
    private var checkedUsers: MutableList<User> = mutableListOf()

    private val groupUserAdapter = GroupUserAdapter()

    private val selectAdapter = UserSelectAdapter {
        checkedUsers.remove(it)
        groupUserAdapter.removeUser(it)
        groupUserAdapter.notifyDataSetChanged()
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_group_users_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)

        contentView.apply {
            close_iv.setOnClickListener { dismiss() }
            search_et.listener = object : SearchView.OnSearchViewListener {
                override fun afterTextChanged(s: Editable?) {
                    filter(s.toString(), users)
                }

                override fun onSearch() {
                }
            }
            search_et.setHint(getString(R.string.contact_search_hint))

            select_rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            select_rv.adapter = selectAdapter
            user_rv.adapter = groupUserAdapter
        }

        // TODO already in call users
        groupUserAdapter.alreadyUserIds = arrayListOf()

        selectAdapter.checkedUsers = checkedUsers

        bottomViewModel.getGroupParticipantsLiveData(conversationId)
            .observe(viewLifecycleOwner, Observer {
                users = it
                filter(search_et.text.toString().trim(), it)
            })
    }

    private fun filter(keyword: String, users: List<User>?) {
        groupUserAdapter.submitList(users?.filter {
            it.fullName!!.contains(keyword, true) ||
                it.identityNumber.contains(keyword, true)
        }?.sortedByDescending { it.fullName == keyword || it.identityNumber == keyword })
    }
}