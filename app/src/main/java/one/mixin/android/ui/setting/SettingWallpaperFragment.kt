package one.mixin.android.ui.setting

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.autoDispose
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSettingChatBinding
import one.mixin.android.databinding.ItemBackgroudBinding
import one.mixin.android.databinding.ItemChatTextBinding
import one.mixin.android.databinding.ItemChatTimeBinding
import one.mixin.android.extension.REQUEST_GALLERY
import one.mixin.android.extension.displayRatio
import one.mixin.android.extension.dp
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.openImageGallery
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.round
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.screenWidth
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.chathistory.holder.TextHolder
import one.mixin.android.ui.conversation.holder.TimeHolder
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.MessageStatus
import one.mixin.android.widget.theme.Coordinate
import one.mixin.android.widget.theme.ThemeActivity

@AndroidEntryPoint
class SettingWallpaperFragment : BaseFragment(R.layout.fragment_setting_chat) {
    companion object {
        const val TAG = "SettingChatFragment"

        fun newInstance() = SettingWallpaperFragment()
    }

    private val binding by viewBinding(FragmentSettingChatBinding::bind)

    private var currentSelected = 1

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
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
        binding.backgroundRv.adapter =
            object : RecyclerView.Adapter<BackgroundHolder>() {
                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: Int,
                ): BackgroundHolder {
                    return BackgroundHolder(
                        ItemBackgroudBinding.inflate(
                            LayoutInflater.from(parent.context),
                            parent,
                            false,
                        ).apply {
                            image.round(3.dp)
                        },
                    )
                }

                @SuppressLint("NotifyDataSetChanged")
                override fun onBindViewHolder(
                    holder: BackgroundHolder,
                    @SuppressLint("RecyclerView") position: Int,
                ) {
                    holder.bind(
                        WallpaperManager.getWallpaperByPosition(requireContext(), position),
                        position == 0,
                        position == currentSelected,
                        !(position == 1 && WallpaperManager.wallpaperExists(requireContext())),
                    )
                    holder.itemView.setOnClickListener {
                        if (position == 0) {
                            selectWallpaper()
                        } else {
                            notifyDataSetChanged()
                            scrollToPosition(position)
                            switchWallpaper(it)
                            binding.container.backgroundImage =
                                WallpaperManager.getWallpaperByPosition(requireContext(), position)
                        }
                    }
                }

                override fun getItemCount(): Int = WallpaperManager.getWallpaperCount(requireContext())
            }
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
                                binding.chatTv.text = requireContext().getString(R.string.i_am_good)
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
    }

    private fun scrollToPosition(position: Int) {
        binding.apply {
            val centerOfScreen: Int = backgroundRv.width / 2 - 50.dp
            (backgroundRv.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, centerOfScreen)
        }
        currentSelected = position
    }

    private fun selectWallpaper() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            RxPermissions(requireActivity())
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .autoDispose(stopScope)
                .subscribe { granted ->
                    if (granted) {
                        openImageGallery(true)
                    } else {
                        context?.openPermissionSetting()
                    }
                }
        } else {
            openImageGallery(true)
        }
    }

    private val imageUri: Uri by lazy {
        Uri.fromFile(WallpaperManager.wallpaperFile(requireContext()))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        if (requestCode == REQUEST_GALLERY && resultCode == Activity.RESULT_OK) {
            var selectedImageUri: Uri?
            if (data == null || data.action != null && data.action == MediaStore.ACTION_IMAGE_CAPTURE) {
                selectedImageUri = imageUri
            } else {
                selectedImageUri = data.data
                if (selectedImageUri == null) {
                    selectedImageUri = imageUri
                }
            }
            val options = UCrop.Options()
            options.setToolbarColor(ContextCompat.getColor(requireContext(), R.color.black))
            options.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.black))
            options.setToolbarWidgetColor(Color.WHITE)
            options.setHideBottomControls(true)
            UCrop.of(selectedImageUri, imageUri)
                .withOptions(options)
                .withAspectRatio(1f, requireContext().displayRatio())
                .withMaxResultSize(
                    requireContext().screenWidth(),
                    requireContext().screenHeight(),
                )
                .start(requireContext(), this)
        } else if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            binding.backgroundRv.adapter?.notifyDataSetChanged()
            currentSelected = 1
            binding.container.backgroundImage =
                WallpaperManager.getWallpaperByPosition(requireContext(), 1)
        }
    }

    class BackgroundHolder constructor(val binding: ItemBackgroudBinding) :
        RecyclerView.ViewHolder(binding.root) {
            fun bind(
                drawable: Drawable?,
                iconVisible: Boolean,
                selected: Boolean,
                center: Boolean = true,
            ) {
                binding.image.setImageDrawable(drawable)
                binding.icon.isVisible = iconVisible
                binding.image.scaleType =
                    if (center) ImageView.ScaleType.CENTER else ImageView.ScaleType.CENTER_CROP
                binding.selected.isVisible = selected
            }
        }

    private fun switchWallpaper(view: View) {
        (requireActivity() as ThemeActivity).run {
            changeTheme(
                getViewCoordinates(view),
                250L,
                false,
            ) {
            }
        }
    }

    private fun getViewCoordinates(view: View): Coordinate {
        return Coordinate(
            getRelativeLeft(view) + view.width / 2,
            getRelativeTop(view) + view.height / 2,
        )
    }

    private fun getRelativeLeft(myView: View): Int {
        return if ((myView.parent as View).id == ThemeActivity.ROOT_ID) {
            myView.left
        } else {
            myView.left +
                getRelativeLeft(
                    myView.parent as View,
                )
        }
    }

    private fun getRelativeTop(myView: View): Int {
        return if ((myView.parent as View).id == ThemeActivity.ROOT_ID) {
            myView.top
        } else {
            myView.top +
                getRelativeTop(
                    myView.parent as View,
                )
        }
    }
}
