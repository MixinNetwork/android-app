package one.mixin.android.ui.common

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentDisappearingBinding
import one.mixin.android.extension.highlightStarTag
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.isGroupConversation
import one.mixin.android.widget.picker.INTERVAL_DAY
import one.mixin.android.widget.picker.INTERVAL_MONTH
import one.mixin.android.widget.picker.INTERVAL_WEEK
import one.mixin.android.widget.picker.toTimeInterval
import one.mixin.android.widget.picker.toTimeIntervalIndex
import timber.log.Timber

@AndroidEntryPoint
class DisappearingFragment : BaseFragment(R.layout.fragment_disappearing) {
    companion object {
        const val TAG = "DisappearingFragment"
        private const val CONVERSATION_ID = "conversation_id"
        private const val USER_ID = "user_id"

        fun newInstance(
            conversationId: String,
            userId: String? = null,
        ) =
            DisappearingFragment().withArgs {
                putString(CONVERSATION_ID, conversationId)
                putString(USER_ID, userId)
            }
    }

    private val conversationId by lazy {
        requireNotNull(requireArguments().getString(CONVERSATION_ID))
    }

    private val userId by lazy {
        requireArguments().getString(USER_ID)
    }

    private val viewModel by viewModels<ConversationViewModel>()
    private val binding by viewBinding(FragmentDisappearingBinding::bind)
    private val checkGroup
        get() = binding.run {
            listOf(
                disappearingOffIv,
                disappearingOption1Iv,
                disappearingOption2Iv,
                disappearingOption3Iv,
                disappearingOption6Iv,
            )
        }

    private val pbGroup
        get() = binding.run {
            listOf(
                disappearingOffPb,
                disappearingOption1Pb,
                disappearingOption2Pb,
                disappearingOption3Pb,
                disappearingOption6Pb,
            )
        }

    private fun Long?.initOption() {
        when {
            this == null || this <= 0 -> updateOptionCheck(0)
            this == INTERVAL_DAY -> updateOptionCheck(1)
            this == INTERVAL_WEEK -> updateOptionCheck(2)
            this == INTERVAL_MONTH -> updateOptionCheck(3)
            else -> {
                updateOptionCheck(4)
                binding.disappearingOption6Interval.text = toTimeInterval(this)
            }
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        canEdit = false
        updating = false
        binding.titleView.leftIb.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        val info = getString(R.string.disappearing_message_hint)
        val learnUrl = getString(R.string.disappearing_message_url)
        binding.tipTv.highlightStarTag(info, arrayOf(learnUrl))
        val intervalPicker = parentFragmentManager.findFragmentByTag(DisappearingIntervalBottomFragment.TAG) as? DisappearingIntervalBottomFragment
        intervalPicker?.onSetCallback(null)
        val options = binding.run {
            listOf(disappearingOff, disappearingOption1, disappearingOption2, disappearingOption3, disappearingOption6)
        }
        options.forEach { it.isEnabled = false }

        viewLifecycleOwner.lifecycleScope.launch {
            val conversation = viewModel.getConversation(conversationId)
            canEdit = if (conversation?.isGroupConversation() == true) {
                val role = withContext(Dispatchers.IO) {
                    Session.getAccountId()?.let { viewModel.findParticipantById(conversationId, it)?.role }
                }
                role == ParticipantRole.OWNER.name || role == ParticipantRole.ADMIN.name
            } else {
                conversation != null || userId != null
            }
            timeInterval = conversation?.expireIn
            timeInterval.initOption()
            options.forEach {
                it.isEnabled = canEdit
                it.isFocusable = canEdit
            }
            if (!canEdit) {
                intervalPicker?.dismissAllowingStateLoss()
                return@launch
            }
            intervalPicker?.onSetCallback(::onIntervalSelected)
            binding.apply {
                disappearingOff.setOnClickListener {
                    updateUI(0, 0L)
                }
                disappearingOption1.setOnClickListener {
                    updateUI(1, INTERVAL_DAY)
                }
                disappearingOption2.setOnClickListener {
                    updateUI(2, INTERVAL_WEEK)
                }
                disappearingOption3.setOnClickListener {
                    updateUI(3, INTERVAL_MONTH)
                }

                disappearingOption6.setOnClickListener {
                    DisappearingIntervalBottomFragment.newInstance(timeInterval)
                        .apply {
                            onSetCallback(::onIntervalSelected)
                        }
                        .showNow(parentFragmentManager, DisappearingIntervalBottomFragment.TAG)
                }
            }
        }
    }

    private var timeInterval: Long? = null
    private var updating = false
    private var canEdit = false

    override fun onDestroyView() {
        canEdit = false
        (parentFragmentManager.findFragmentByTag(DisappearingIntervalBottomFragment.TAG) as? DisappearingIntervalBottomFragment)
            ?.onSetCallback(null)
        super.onDestroyView()
    }

    private fun onIntervalSelected(interval: Long) {
        if (!canEdit) return
        val index = when (interval) {
            INTERVAL_DAY -> 1
            INTERVAL_WEEK -> 2
            INTERVAL_MONTH -> 3
            else -> 4
        }
        if (interval == timeInterval) {
            updateOptionCheck(index)
            return
        }
        binding.disappearingOption6Iv.isVisible = false
        binding.disappearingOption6Interval.isVisible = false
        binding.disappearingOption6Arrow.isVisible = false
        updateUI(index, interval)
        binding.disappearingOption6Interval.text = toTimeInterval(interval)
        Timber.e("Set interval ${toTimeInterval(interval)} ${toTimeIntervalIndex(interval)}")
    }

    private fun updateUI(
        index: Int,
        interval: Long,
    ) {
        if (!canEdit || timeInterval == interval || updating) {
            return
        }
        viewLifecycleOwner.lifecycleScope.launch(ErrorHandler.errorHandler) {
            pbGroup[index].isVisible = true
            updating = true
            val conversation = viewModel.getConversation(conversationId)
            if (conversation == null) {
                if (userId != null) {
                    val result = viewModel.createConversation(conversationId, userId!!)
                    if (!result) {
                        return@launch
                    }
                } else {
                    return@launch
                }
            }
            handleMixinResponse(
                invokeNetwork = { viewModel.disappear(conversationId, interval) },
                successBlock = { response ->
                    if (response.isSuccess) {
                        timeInterval = response.data?.expireIn
                        updateOptionCheck(index)
                        viewModel.updateConversationExpireIn(conversationId, timeInterval)
                    }
                    updating = false
                },
                doAfterNetworkSuccess = {
                    pbGroup[index].isVisible = false
                    updating = false
                },
                defaultErrorHandle = {
                    ErrorHandler.handleMixinError(it.errorCode, it.errorDescription)
                    pbGroup[index].isVisible = false
                    updating = false
                },
                defaultExceptionHandle = {
                    if (it is CancellationException) throw it
                    ErrorHandler.handleError(it)
                    pbGroup[index].isVisible = false
                    updating = false
                },
            )
        }
    }

    private fun updateOptionCheck(index: Int) {
        for ((i, iv) in checkGroup.withIndex()) {
            iv.isVisible = index == i
        }
        binding.disappearingOption6Arrow.isVisible = canEdit && index != 4
        binding.disappearingOption6Interval.isVisible = index == 4
    }
}
