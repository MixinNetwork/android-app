package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSettingChatBinding
import one.mixin.android.databinding.ItemBackgroudBinding
import one.mixin.android.databinding.ItemChatTextBinding
import one.mixin.android.databinding.ItemChatTimeBinding
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.round
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.chathistory.holder.TextHolder
import one.mixin.android.ui.conversation.holder.TimeHolder
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.MessageStatus

@AndroidEntryPoint
class SettingChatFragment : BaseFragment(R.layout.fragment_setting_chat) {
    companion object {
        const val TAG = "SettingChatFragment"
        fun newInstance() = SettingChatFragment()
    }

    private val binding by viewBinding(FragmentSettingChatBinding::bind)

    private var currentSelected = 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (requireContext().booleanFromAttribute(R.attr.flag_night)) {
            binding.container.backgroundImage =
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_chat_night)
        } else {
            binding.container.backgroundImage =
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_chat)
        }
        binding.titleView.leftIb.setOnClickListener {
            requireActivity().onBackPressed()
        }
        binding.titleView.rightTv.setOnClickListener {
            // Todo save background
            requireActivity().onBackPressed()
        }
        binding.backgroundRv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)
        binding.backgroundRv.adapter = object :RecyclerView.Adapter<BackgroundHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): BackgroundHolder {
                return BackgroundHolder(
                    ItemBackgroudBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onBindViewHolder(holder: BackgroundHolder, position: Int) {
                val p = position
                holder.bind(R.drawable.bg_chat_night, position == currentSelected)
                holder.itemView.setOnClickListener {
                    currentSelected = p
                    notifyDataSetChanged()
                    binding.backgroundRv.layoutManager?.smoothScrollToPosition(
                        binding.backgroundRv,
                        null,
                        position
                    )
                }
            }

            override fun getItemCount(): Int = 4
        }
        binding.chatRv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                return when (viewType) {
                    0 -> {
                        TimeHolder(
                            ItemChatTimeBinding.inflate(
                                LayoutInflater.from(parent.context),
                                parent,
                                false
                            )
                        )
                    }
                    1 -> {
                        TextHolder(
                            ItemChatTextBinding.inflate(
                                LayoutInflater.from(parent.context),
                                parent,
                                false
                            )
                        )
                    }
                    else -> {
                        TextHolder(
                            ItemChatTextBinding.inflate(
                                LayoutInflater.from(parent.context),
                                parent,
                                false
                            )
                        )
                    }
                }
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                when (position) {
                    2 -> (holder as TextHolder).apply {
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
                            isSecret = false
                        )
                        setItemBackgroundResource(
                            binding.chatLayout,
                            R.drawable.chat_bubble_other_last,
                            R.drawable.chat_bubble_other_last_night
                        )
                    }
                    1 -> (holder as TextHolder).apply {
                        val lp = (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams)
                        lp.horizontalBias = 1f
                        binding.chatName.isVisible = false
                        binding.chatTime.load(
                            true,
                            nowInUtc(),
                            MessageStatus.READ.name,
                            isPin = false,
                            isRepresentative = false,
                            isSecret = false
                        )
                        binding.chatTv.text = requireContext().getString(R.string.i_am_good)
                        setItemBackgroundResource(
                            binding.chatLayout,
                            R.drawable.chat_bubble_me_last,
                            R.drawable.chat_bubble_me_last_night
                        )
                    }
                    else -> (holder as TimeHolder).binding.chatTime.text = "今天"
                }
            }

            override fun getItemViewType(position: Int) = position

            override fun getItemCount(): Int = 3
        }
    }

    class BackgroundHolder constructor(val binding: ItemBackgroudBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(@DrawableRes resId:Int,selected:Boolean) {
            binding.image.setImageResource(resId)
            binding.image.round(3.dp)
            binding.selected.isVisible = selected
        }
    }
}
