package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Account.PREF_TEXT_SIZE
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSizeBinding
import one.mixin.android.databinding.ItemChatTextBinding
import one.mixin.android.databinding.ItemChatTimeBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.putInt
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.chathistory.holder.TextHolder
import one.mixin.android.ui.conversation.holder.TimeHolder
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.MessageStatus

@AndroidEntryPoint
class SettingSizeFragment : BaseFragment(R.layout.fragment_size) {
    companion object {
        const val TAG = "SettingSizeFragment"

        fun newInstance() = SettingSizeFragment()
    }

    private val binding by viewBinding(FragmentSizeBinding::bind)

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        textSize = initTextSize.toFloat()
        binding.container.backgroundImage = WallpaperManager.getWallpaper(requireContext())
        binding.titleView.leftIb.setOnClickListener {
            requireActivity().onBackPressed()
        }
        binding.titleView.rightTv.setOnClickListener {
            textSize = 14f
            requireContext().defaultSharedPreferences.putInt(PREF_TEXT_SIZE, 14)
            requireContext().tickVibrate()
            binding.slider.value = textSize
            binding.chatRv.adapter?.notifyDataSetChanged()
        }
        binding.slider.value = initTextSize.toFloat()
        binding.chatRv.adapter =
            object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: Int,
                ): RecyclerView.ViewHolder {
                    return when (viewType) {
                        0 -> {
                            TimeHolder(
                                ItemChatTimeBinding.inflate(
                                    LayoutInflater.from(parent.context),
                                    parent,
                                    false,
                                ),
                            )
                        }

                        1 -> {
                            TextHolder(
                                ItemChatTextBinding.inflate(
                                    LayoutInflater.from(parent.context),
                                    parent,
                                    false,
                                ),
                            )
                        }

                        else -> {
                            TextHolder(
                                ItemChatTextBinding.inflate(
                                    LayoutInflater.from(parent.context),
                                    parent,
                                    false,
                                ),
                            )
                        }
                    }
                }

                override fun onBindViewHolder(
                    holder: RecyclerView.ViewHolder,
                    position: Int,
                ) {
                    when (position) {
                        1 ->
                            (holder as TextHolder).apply {
                                val lp = (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams)
                                lp.horizontalBias = 0f
                                binding.chatName.isVisible = false
                                binding.chatTv.text = requireContext().getString(R.string.how_are_you)
                                binding.chatTime.load(
                                    false,
                                    nowInUtc(),
                                    MessageStatus.DELIVERED.name,
                                    isPin = false,
                                    isRepresentative = false,
                                    isSecret = false,
                                )
                                binding.chatTime.changeSize(textSize - 4f)
                                binding.chatTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)
                                setItemBackgroundResource(
                                    binding.chatLayout,
                                    R.drawable.chat_bubble_other_last,
                                    R.drawable.chat_bubble_other_last_night,
                                )
                            }

                        2 ->
                            (holder as TextHolder).apply {
                                val lp = (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams)
                                lp.horizontalBias = 1f
                                binding.chatName.isVisible = false
                                binding.chatTime.load(
                                    true,
                                    nowInUtc(),
                                    MessageStatus.READ.name,
                                    isPin = false,
                                    isRepresentative = false,
                                    isSecret = false,
                                )
                                binding.chatTime.changeSize(textSize - 4f)
                                binding.chatTv.text = requireContext().getString(R.string.i_am_good)
                                binding.chatTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)
                                setItemBackgroundResource(
                                    binding.chatLayout,
                                    R.drawable.chat_bubble_me_last,
                                    R.drawable.chat_bubble_me_last_night,
                                )
                            }

                        else ->
                            (holder as TimeHolder).binding.chatTime.text =
                                requireContext().getString(R.string.Today)
                    }
                }

                override fun getItemViewType(position: Int) = position

                override fun getItemCount(): Int = 3
            }

        lifecycleScope.launch {
            callbackFlow {
                val onChangeListener =
                    Slider.OnChangeListener { _, value, _ -> trySend(value.toInt()) }
                if (viewDestroyed()) return@callbackFlow

                binding.slider.addOnChangeListener(onChangeListener)
                awaitClose {
                    if (!viewDestroyed()) {
                        binding.slider.removeOnChangeListener(onChangeListener)
                    }
                }
            }.collect {
                if (viewDestroyed()) return@collect
                
                textSize = it.toFloat()
                requireContext().defaultSharedPreferences.putInt(PREF_TEXT_SIZE, it)
                requireContext().tickVibrate()
                binding.chatRv.adapter?.notifyDataSetChanged()
            }
        }
    }

    private val initTextSize by lazy {
        requireContext().defaultSharedPreferences.getInt(PREF_TEXT_SIZE, 14)
    }

    private var textSize = 14f
}
