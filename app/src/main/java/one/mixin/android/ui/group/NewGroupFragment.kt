package one.mixin.android.ui.group

import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentNewGroupBinding
import one.mixin.android.extension.CodeType
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.getColorCode
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.DisappearingIntervalBottomFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.User
import one.mixin.android.vo.toUser
import one.mixin.android.widget.AvatarView
import one.mixin.android.widget.picker.INTERVAL_DAY
import one.mixin.android.widget.picker.INTERVAL_MONTH
import one.mixin.android.widget.picker.INTERVAL_WEEK
import one.mixin.android.widget.picker.getTimeInterval

@AndroidEntryPoint
class NewGroupFragment : BaseFragment() {
    companion object {
        const val TAG = "NewGroupFragment"
        private const val ARGS_USERS = "args_users"
        private const val STATE_DURATION = "duration"

        fun newInstance(users: ArrayList<User>): NewGroupFragment {
            val fragment = NewGroupFragment()
            fragment.withArgs {
                putParcelableArrayList(ARGS_USERS, users)
            }
            return fragment
        }
    }

    private val groupViewModel by viewModels<GroupViewModel>()
    private val sender: User by lazy { Session.getAccount()!!.toUser() }
    private val users: List<User> by lazy {
        requireNotNull(requireArguments().getParcelableArrayListCompat(ARGS_USERS, User::class.java))
    }
    private var dialog: Dialog? = null
    private var duration = INTERVAL_WEEK
    private var durationMenu: PopupMenu? = null

    private var _binding: FragmentNewGroupBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        duration = savedInstanceState?.getLong(STATE_DURATION, INTERVAL_WEEK) ?: INTERVAL_WEEK
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_DURATION, duration)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNewGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(view) { root, insets ->
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navigation = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            root.updatePadding(bottom = (keyboard - navigation).coerceAtLeast(0))
            insets
        }
        ViewCompat.requestApplyInsets(view)
        binding.titleView.getChildAt(0).setBackgroundColor(requireContext().colorFromAttribute(R.attr.bg_window))
        binding.titleView.titleTv.textView.setTypeface(binding.titleView.titleTv.textView.typeface, Typeface.BOLD)
        val participants = (users + sender).distinctBy { it.userId }
        binding.participantsCount.text = resources.getQuantityString(R.plurals.title_participants, participants.size, participants.size)
        showGroupAvatar(participants)
        binding.titleView.leftIb.setOnClickListener {
            binding.nameDescEt.hideKeyboard()
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        binding.createBtn.setOnClickListener {
            createGroup()
        }
        binding.disappearingValue.text = duration.getTimeInterval()
        (childFragmentManager.findFragmentByTag(DisappearingIntervalBottomFragment.TAG) as? DisappearingIntervalBottomFragment)
            ?.onSetCallback(::setDuration)
        binding.disappearingRow.setOnClickListener {
            showDurationMenu()
        }
        binding.nameDescEt.doAfterTextChanged {
            binding.createBtn.isEnabled = !it.isNullOrEmpty()
        }
        if (savedInstanceState == null) {
            binding.nameDescEt.showKeyboard()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dialog?.dismiss()
        durationMenu?.dismiss()
        _binding = null
    }

    private fun setDuration(value: Long) {
        duration = value
        binding.disappearingValue.text = value.getTimeInterval()
    }

    private fun showDurationMenu() {
        val options = listOf(
            0L to R.string.Off,
            INTERVAL_DAY to R.string.disappearing_option_4,
            INTERVAL_WEEK to R.string.disappearing_option_5,
            INTERVAL_MONTH to R.string.disappearing_option_month,
            null to R.string.Custom,
        )
        val selected = options.indexOfFirst { it.first == duration }.takeIf { it >= 0 } ?: options.lastIndex
        durationMenu?.dismiss()
        durationMenu = PopupMenu(requireContext(), binding.disappearingRow, Gravity.END).apply {
            options.forEachIndexed { index, (_, title) ->
                menu.add(0, index, index, title)
            }
            menu.setGroupCheckable(0, true, true)
            menu.findItem(selected).isChecked = true
            setOnMenuItemClickListener { item ->
                val interval = options[item.itemId].first
                if (interval == null) {
                    binding.nameDescEt.hideKeyboard()
                    DisappearingIntervalBottomFragment.newInstance(duration)
                        .apply { onSetCallback(::setDuration) }
                        .showNow(childFragmentManager, DisappearingIntervalBottomFragment.TAG)
                } else {
                    setDuration(interval)
                }
                true
            }
            setOnDismissListener { durationMenu = null }
            show()
        }
    }

    private fun createGroup() =
        lifecycleScope.launch {
            if (dialog == null) {
                dialog =
                    indeterminateProgressDialog(
                        message = R.string.Please_wait_a_bit,
                        title = R.string.Creating,
                    ).apply {
                        setCancelable(false)
                    }
            }
            dialog?.show()

            val conversation =
                groupViewModel.createGroupConversation(
                    binding.nameDescEt.text.toString(),
                    "",
                    null,
                    users,
                    sender,
                    duration = duration,
                )
            val liveData = groupViewModel.getConversationStatusById(conversation.conversationId)
            liveData.observe(
                viewLifecycleOwner,
            ) { c ->
                if (c != null) {
                    when (c.status) {
                        ConversationStatus.SUCCESS.ordinal -> {
                            liveData.removeObservers(viewLifecycleOwner)
                            binding.nameDescEt.hideKeyboard()
                            dialog?.dismiss()
                            activity?.finish()
                            ConversationActivity.showAndClear(
                                requireContext(),
                                conversation.conversationId,
                            )
                        }
                        ConversationStatus.FAILURE.ordinal -> {
                            liveData.removeObservers(viewLifecycleOwner)
                            binding.nameDescEt.hideKeyboard()
                            dialog?.dismiss()
                            MainActivity.reopen(requireContext())
                        }
                    }
                }
            }
        }

    private fun showGroupAvatar(participants: List<User>) {
        val preview = participants.take(4)
        val columns = when (preview.size) {
            3 -> listOf(preview.take(1), preview.drop(1))
            4 -> listOf(listOf(preview[0], preview[2]), listOf(preview[1], preview[3]))
            else -> preview.map { listOf(it) }
        }
        val colors = resources.getIntArray(R.array.avatar_colors)
        binding.groupAvatar.removeAllViews()
        columns.forEach { users ->
            val column = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
            }
            binding.groupAvatar.addView(column, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
            users.forEach { user ->
                val avatar = AvatarView(requireContext()).apply {
                    avatarSimple.setDisableCircularTransformation(true)
                    setBackgroundColor(colors.getOrElse(user.userId.getColorCode(CodeType.Avatar(colors.size))) { colors[0] })
                    setTextSize(if (preview.size > 2) 16f else 20f)
                    setInfo(user.fullName, user.avatarUrl, user.userId)
                }
                column.addView(avatar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            }
        }
    }
}
