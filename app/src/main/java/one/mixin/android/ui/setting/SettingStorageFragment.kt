package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.collection.ArraySet
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Storage.AUDIO
import one.mixin.android.Constants.Storage.DATA
import one.mixin.android.Constants.Storage.IMAGE
import one.mixin.android.Constants.Storage.TRANSCRIPT
import one.mixin.android.Constants.Storage.VIDEO
import one.mixin.android.R
import one.mixin.android.databinding.FragmentStorageBinding
import one.mixin.android.databinding.ItemContactStorageBinding
import one.mixin.android.databinding.ItemStorageCheckBinding
import one.mixin.android.extension.FileSizeUnit
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.debug.measureTimeMillis
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStorageUsage
import one.mixin.android.vo.StorageUsage
import timber.log.Timber

@AndroidEntryPoint
class SettingStorageFragment : BaseFragment(R.layout.fragment_storage) {
    companion object {
        const val TAG = "SettingStorageFragment"

        fun newInstance(): SettingStorageFragment {
            return SettingStorageFragment()
        }
    }

    private val viewModel by viewModels<SettingStorageViewModel>()
    private val binding by viewBinding(FragmentStorageBinding::bind)

    private val adapter = StorageAdapter {
        showMenu(it)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.leftIb.setOnClickListener { activity?.onBackPressed() }
            bRv.adapter = adapter
            menuView.adapter = menuAdapter
        }

        lifecycleScope.launch {
            val list = measureTimeMillis("Storage") { viewModel.getConversationStorageUsage(requireContext()) }
            binding.progress.isVisible = false
            adapter.setData(list)
        }
    }

    private val dialog: Dialog by lazy {
        indeterminateProgressDialog(
            message = R.string.pb_dialog_message,
            title = R.string.Clearing
        ).apply {
            setCancelable(false)
        }
    }

    private val selectSet: ArraySet<StorageUsage> = ArraySet()
    private fun showMenu(conversationId: String) {
        lifecycleScope.launch {
            val list = viewModel.getStorageUsage(requireContext(), conversationId)
            menuAdapter.setData(list)
            selectSet.clear()
            for (item in list) {
                selectSet.add(item)
            }
            menuDialog.show()
        }
    }

    private val menuDialog: AlertDialog by lazy {
        alertDialogBuilder()
            .setView(menuView)
            .setNegativeButton(R.string.Cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.Clear) { dialog, _ ->
                var sum = 0L
                var size = 0L
                selectSet.forEach { sum += it.count; size += it.mediaSize }
                confirmDialog.setMessage(requireContext().resources.getQuantityString(R.plurals.setting_storage_clear, sum.toInt(), sum, size.fileSize(FileSizeUnit.KB)))
                confirmDialog.show()
                dialog.dismiss()
            }.create().apply {
                setOnShowListener {
                    val states = arrayOf(
                        intArrayOf(android.R.attr.state_enabled),
                        intArrayOf(-android.R.attr.state_enabled)
                    )
                    val colors = intArrayOf(Color.RED, Color.GRAY)
                    getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ColorStateList(states, colors))
                }
                this.window?.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
            }
    }

    private val confirmDialog: AlertDialog by lazy {
        alertDialogBuilder()
            .setNegativeButton(R.string.Cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                clear()
                dialog.dismiss()
            }.create().apply {
                setOnShowListener {
                    getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.RED)
                }
            }
    }

    private fun clear() {
        dialog.show()
        Observable.just(selectSet)
            .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
            .map {
                for (item in selectSet) {
                    when (item.type) {
                        IMAGE, VIDEO, AUDIO, DATA, TRANSCRIPT -> {
                            viewModel.clear(item.conversationId, item.type)
                        }
                        else -> {
                            Timber.e("Unknown type")
                        }
                    }
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(stopScope)
            .subscribe(
                {
                    dialog.dismiss()
                },
                {
                    Timber.e(it)
                    dialog.dismiss()
                    toast(getString(R.string.error_unknown_with_message, selectSet.toString()))
                }
            )
    }

    private val menuView: RecyclerView by lazy {
        View.inflate(requireContext(), R.layout.view_stotage_list, null) as RecyclerView
    }
    private val menuAdapter: MenuAdapter by lazy {
        MenuAdapter(
            object : (Boolean, StorageUsage) -> Unit {
                override fun invoke(checked: Boolean, storageUsage: StorageUsage) {
                    if (checked) {
                        selectSet.add(storageUsage)
                    } else {
                        selectSet.remove(storageUsage)
                    }
                    menuDialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = selectSet.size > 0
                }
            }
        )
    }

    class MenuAdapter(private val checkAction: (Boolean, StorageUsage) -> Unit) : RecyclerView.Adapter<CheckHolder>() {
        private var storageUsageList: List<StorageUsage>? = null

        @SuppressLint("NotifyDataSetChanged")
        fun setData(users: List<StorageUsage>?) {
            this.storageUsageList = users
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckHolder {
            return CheckHolder(ItemStorageCheckBinding.inflate(LayoutInflater.from(parent.context), parent, false), checkAction)
        }

        override fun getItemCount(): Int = storageUsageList?.size ?: 0

        override fun onBindViewHolder(holder: CheckHolder, position: Int) {
            storageUsageList?.let {
                holder.bind(it[position])
            }
        }
    }

    class StorageAdapter(val action: ((String) -> Unit)) : RecyclerView.Adapter<ItemHolder>() {

        private var conversationStorageUsageList: List<ConversationStorageUsage>? = null

        @SuppressLint("NotifyDataSetChanged")
        fun setData(users: List<ConversationStorageUsage>?) {
            this.conversationStorageUsageList = users
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
            return ItemHolder(ItemContactStorageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            if (conversationStorageUsageList == null || conversationStorageUsageList!!.isEmpty()) {
                return
            }
            holder.bind(conversationStorageUsageList!![position], action)
        }

        override fun getItemCount(): Int = conversationStorageUsageList?.size ?: 0
    }

    class CheckHolder(private val itemBinding: ItemStorageCheckBinding, private val checkAction: (Boolean, StorageUsage) -> Unit) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(storageUsage: StorageUsage) {
            itemBinding.apply {
                checkView.setName(
                    when (storageUsage.type) {
                        IMAGE -> R.string.Photo
                        DATA -> R.string.Files
                        VIDEO -> R.string.Video
                        AUDIO -> R.string.Audio
                        TRANSCRIPT -> R.string.Transcript
                        else -> R.string.Unknown
                    }
                )
                checkView.setSize(storageUsage.mediaSize)
                checkView.isChecked = true
                checkView.setOnCheckedChangeListener { _, checked ->
                    checkAction(checked, storageUsage)
                }
            }
        }
    }

    class ItemHolder(private val itemBinding: ItemContactStorageBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(conversationStorageUsage: ConversationStorageUsage, action: ((String) -> Unit)) {
            itemBinding.apply {
                if (conversationStorageUsage.category == ConversationCategory.GROUP.name) {
                    avatar.setGroup(conversationStorageUsage.groupIconUrl)
                    normal.text = conversationStorageUsage.groupName
                } else {
                    normal.text = conversationStorageUsage.name
                    avatar.setInfo(conversationStorageUsage.name, conversationStorageUsage.avatarUrl, conversationStorageUsage.ownerId)
                }
                storageTv.text = conversationStorageUsage.mediaSize.fileSize(FileSizeUnit.KB)
                itemView.setOnClickListener { action(conversationStorageUsage.conversationId) }
            }
        }
    }
}
