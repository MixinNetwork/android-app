package one.mixin.android.ui.setting

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.collection.ArraySet
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_storage.*
import kotlinx.android.synthetic.main.item_contact_storage.view.*
import kotlinx.android.synthetic.main.item_storage_check.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.Constants.Storage.AUDIO
import one.mixin.android.Constants.Storage.DATA
import one.mixin.android.Constants.Storage.IMAGE
import one.mixin.android.Constants.Storage.VIDEO
import one.mixin.android.R
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStorageUsage
import one.mixin.android.vo.StorageUsage
import timber.log.Timber

@AndroidEntryPoint
class SettingStorageFragment : BaseFragment() {
    companion object {
        const val TAG = "SettingStorageFragment"

        fun newInstance(): SettingStorageFragment {
            return SettingStorageFragment()
        }
    }

    private val viewModel by viewModels<SettingStorageViewModel>()

    private val adapter = StorageAdapter {
        showMenu(it)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_storage, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        b_rv.adapter = adapter
        menuView.adapter = menuAdapter
        viewModel.getConversationStorageUsage().autoDispose(stopScope)
            .subscribe(
                { list ->
                    if (progress.visibility != View.GONE) {
                        progress.visibility = View.GONE
                    }
                    adapter.setData(list)
                },
                { error ->
                    Timber.e(error)
                }
            )
    }

    private val dialog: Dialog by lazy {
        indeterminateProgressDialog(
            message = R.string.pb_dialog_message,
            title = R.string.setting_clearing
        ).apply {
            setCancelable(false)
        }
    }

    private val selectSet: ArraySet<StorageUsage> = ArraySet()
    private fun showMenu(conversationId: String) {
        viewModel.getStorageUsage(conversationId).autoDispose(stopScope).subscribe(
            {
                menuAdapter.setData(it)
                selectSet.clear()
                it?.let {
                    for (item in it) {
                        selectSet.add(item)
                    }
                }
                menuDialog.show()
            },
            {
                Timber.e(it)
            }
        )
    }

    private val menuDialog: AlertDialog by lazy {
        alertDialogBuilder()
            .setView(menuView)
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.setting_storage_bn_clear) { dialog, _ ->
                var sum = 0L
                var size = 0L
                selectSet.forEach { sum += it.count; size += it.mediaSize }
                confirmDialog.setMessage(getString(R.string.setting_storage_clear, sum, size.fileSize()))
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
            .setNegativeButton(R.string.cancel) { dialog, _ ->
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
                        IMAGE, VIDEO, AUDIO, DATA -> {
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

        fun setData(users: List<StorageUsage>?) {
            this.storageUsageList = users
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckHolder {
            return CheckHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_storage_check, parent, false), checkAction)
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

        fun setData(users: List<ConversationStorageUsage>?) {
            this.conversationStorageUsageList = users
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
            return ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_contact_storage, parent, false))
        }

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            if (conversationStorageUsageList == null || conversationStorageUsageList!!.isEmpty()) {
                return
            }
            holder.bind(conversationStorageUsageList!![position], action)
        }

        override fun getItemCount(): Int = conversationStorageUsageList?.size ?: 0
    }

    class CheckHolder(itemView: View, private val checkAction: (Boolean, StorageUsage) -> Unit) : RecyclerView.ViewHolder(itemView) {
        fun bind(storageUsage: StorageUsage) {
            itemView.check_view.setName(
                when (storageUsage.type) {
                    IMAGE -> R.string.common_pic
                    DATA -> R.string.common_file
                    VIDEO -> R.string.common_video
                    AUDIO -> R.string.common_audio
                    else -> R.string.unknown
                }
            )
            itemView.check_view.setSize(storageUsage.mediaSize)
            itemView.check_view.isChecked = true
            itemView.check_view.setOnCheckedChangeListener(
                CompoundButton.OnCheckedChangeListener { _, checked ->
                    checkAction(checked, storageUsage)
                }
            )
        }
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(conversationStorageUsage: ConversationStorageUsage, action: ((String) -> Unit)) {
            if (conversationStorageUsage.category == ConversationCategory.GROUP.name) {
                itemView.avatar.setGroup(conversationStorageUsage.groupIconUrl)
                itemView.normal.text = conversationStorageUsage.groupName
            } else {
                itemView.normal.text = conversationStorageUsage.name
                itemView.avatar.setInfo(conversationStorageUsage.name, conversationStorageUsage.avatarUrl, conversationStorageUsage.ownerId)
            }
            itemView.storage_tv.text = conversationStorageUsage.mediaSize.fileSize()
            itemView.setOnClickListener { action(conversationStorageUsage.conversationId) }
        }
    }
}
