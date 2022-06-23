package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSettingChatBinding
import one.mixin.android.databinding.ItemBackgroudBinding
import one.mixin.android.databinding.ItemChatTextBinding
import one.mixin.android.databinding.ItemChatTimeBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.round
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.chathistory.holder.TextHolder
import one.mixin.android.ui.conversation.holder.TimeHolder
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.MessageStatus

@AndroidEntryPoint
class SettingWallpaperFragment : BaseFragment(R.layout.fragment_setting_chat) {
    companion object {
        const val TAG = "SettingChatFragment"
        fun newInstance() = SettingWallpaperFragment()
    }

    private val binding by viewBinding(FragmentSettingChatBinding::bind)

    private var currentSelected = 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentSelected = WallpaperManager.getCurrentSelected(requireContext())
        binding.container.backgroundImage =
            WallpaperManager.getWallpaper(requireContext())
        binding.titleView.leftIb.setOnClickListener {
            requireActivity().onBackPressed()
        }
        binding.titleView.rightTv.setOnClickListener {
            WallpaperManager.save(requireContext(), currentSelected)
            requireActivity().onBackPressed()
        }
        binding.backgroundRv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.backgroundRv.adapter = object : RecyclerView.Adapter<BackgroundHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): BackgroundHolder {
                return BackgroundHolder(
                    ItemBackgroudBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ).apply {
                        image.round(3.dp)
                    }
                )
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onBindViewHolder(holder: BackgroundHolder, position: Int) {
                val p = position - 1
                holder.bind(
                    WallpaperManager.getWallpaper(requireContext(), p),
                    position == 0,
                    position == currentSelected
                )
                holder.itemView.setOnClickListener {
                    currentSelected = p + 1
                    notifyDataSetChanged()
                    binding.backgroundRv.layoutManager?.smoothScrollToPosition(
                        binding.backgroundRv,
                        null,
                        position
                    )
                    if (position != 0) {
                        binding.container.backgroundImage =
                            WallpaperManager.getWallpaper(requireContext(), p)
                    }
                }
            }

            override fun getItemCount(): Int = 5
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
                    else -> (holder as TimeHolder).binding.chatTime.text =
                        requireContext().getString(R.string.Today)
                }
            }

            override fun getItemViewType(position: Int) = position

            override fun getItemCount(): Int = 3
        }
    }

    class BackgroundHolder constructor(val binding: ItemBackgroudBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(drawable: Drawable?, iconVisible: Boolean, selected: Boolean) {
            binding.image.setImageDrawable(drawable)
            binding.icon.isVisible = iconVisible
            binding.selected.isVisible = selected
        }
    }
}
