package one.mixin.android.ui.common

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentDisappearingBinding
import one.mixin.android.extension.highlightLinkText
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.picker.toTimeInterval
import one.mixin.android.widget.picker.toTimeIntervalIndex
import timber.log.Timber

@AndroidEntryPoint
class DisappearingFragment : BaseFragment(R.layout.fragment_disappearing) {
    companion object {
        const val TAG = "DisappearingFragment"
        private const val CONVERSATION_ID = "conversation_id"
        private const val USER_ID = "user_id"
        fun newInstance(conversationId: String, userId: String? = null) = DisappearingFragment().withArgs {
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
    private val checkGroup by lazy {
        binding.run {
            listOf(
                disappearingOffIv,
                disappearingOption1Iv,
                disappearingOption2Iv,
                disappearingOption3Iv,
                disappearingOption4Iv,
                disappearingOption5Iv,
                disappearingOption6Iv
            )
        }
    }

    private val pbGroup by lazy {
        binding.run {
            listOf(
                disappearingOffPb,
                disappearingOption1Pb,
                disappearingOption2Pb,
                disappearingOption3Pb,
                disappearingOption4Pb,
                disappearingOption5Pb,
                disappearingOption6Pb
            )
        }
    }

    private fun Long?.initOption() {
        when {
            this == null || this <= 0 -> updateOptionCheck(0)
            this == 30L -> updateOptionCheck(1)
            this == 600L -> updateOptionCheck(2)
            this == 7200L -> updateOptionCheck(3)
            this == 86400L -> updateOptionCheck(4)
            this == 604800L -> updateOptionCheck(5)
            else -> {
                updateOptionCheck(6)
                binding.disappearingOption6Interval.text = toTimeInterval(this)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val learn: String = requireContext().getString(R.string.disappearing_content_learn)
        val info = requireContext().getString(R.string.disappearing_content)
        // Todo replace url
        val learnUrl = requireContext().getString(R.string.setting_delete_account_url)
        binding.tipTv.highlightLinkText(info, arrayOf(learn), arrayOf(learnUrl))

        lifecycleScope.launch {
            val conversation = viewModel.getConversation(conversationId)
            conversation?.expireIn.initOption()
            timeInterval = conversation?.expireIn
            binding.apply {
                disappearingOff.setOnClickListener {
                    updateUI(0, 0L)
                }
                disappearingOption1.setOnClickListener {
                    updateUI(1, 30L)
                }
                disappearingOption2.setOnClickListener {
                    updateUI(2, 600L)
                }
                disappearingOption3.setOnClickListener {
                    updateUI(3, 7200L)
                }
                disappearingOption4.setOnClickListener {
                    updateUI(4, 86400L)
                }
                disappearingOption5.setOnClickListener {
                    updateUI(5, 604800L)
                }

                disappearingOption6.setOnClickListener {
                    DisappearingIntervalBottomFragment.newInstance(timeInterval)
                        .apply {
                            onSetCallback {
                                this@DisappearingFragment.lifecycleScope.launch {
                                    disappearingOption6Iv.isVisible = false
                                    disappearingOption6Interval.isVisible = false
                                    disappearingOption6Arrow.isVisible = false
                                    updateUI(6, it)
                                    disappearingOption6Interval.text = toTimeInterval(it)
                                    Timber.e(
                                        "Set interval ${toTimeInterval(it)} ${
                                        toTimeIntervalIndex(
                                            it
                                        )
                                        }"
                                    )
                                }
                            }
                        }
                        .showNow(parentFragmentManager, DisappearingIntervalBottomFragment.TAG)
                }
            }
        }
    }

    private var timeInterval: Long? = null

    private fun updateUI(index: Int, interval: Long) {
        lifecycleScope.launch(ErrorHandler.errorHandler) {
            pbGroup[index].isVisible = true
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
                        // Todo save conversation
                        timeInterval = response.data?.expireIn
                        updateOptionCheck(index)
                        viewModel.updateConversationExpireIn(conversationId, timeInterval)
                    }
                },
                doAfterNetworkSuccess = {
                    pbGroup[index].isVisible = false
                },
                defaultErrorHandle = {
                    ErrorHandler.handleMixinError(it.errorCode, it.errorDescription)
                    pbGroup[index].isVisible = false
                },
                defaultExceptionHandle = {
                    ErrorHandler.handleError(it)
                    pbGroup[index].isVisible = false
                }
            )
        }
    }

    private fun updateOptionCheck(index: Int) {
        for ((i, iv) in checkGroup.withIndex()) {
            iv.isVisible = index == i
        }
        binding.disappearingOption6Arrow.isVisible = index != 6
        binding.disappearingOption6Interval.isVisible = index == 6
    }
}
