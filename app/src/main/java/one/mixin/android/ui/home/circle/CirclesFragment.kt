package one.mixin.android.ui.home.circle

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.CIRCLE.CIRCLE_ID
import one.mixin.android.R
import one.mixin.android.databinding.FragmentConversationCircleBinding
import one.mixin.android.databinding.ItemConversationCircleBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.notEmptyWithElse
import one.mixin.android.extension.shakeAnimator
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.EditDialog
import one.mixin.android.ui.common.editDialog
import one.mixin.android.ui.home.ConversationListViewModel
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.errorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.CircleOrder
import one.mixin.android.vo.ConversationCircleItem
import one.mixin.android.vo.getCircleColor
import one.mixin.android.widget.recyclerview.ItemTouchHelperAdapter
import one.mixin.android.widget.recyclerview.OnStartDragListener
import one.mixin.android.widget.recyclerview.SimpleItemTouchHelperCallback
import org.threeten.bp.Instant
import java.util.Collections

@AndroidEntryPoint
class CirclesFragment : BaseFragment(), OnStartDragListener {
    companion object {
        const val TAG = "CirclesFragment"

        fun newInstance(): CirclesFragment {
            return CirclesFragment()
        }
    }

    private val conversationViewModel by viewModels<ConversationListViewModel>()

    private val binding by viewBinding(FragmentConversationCircleBinding::bind)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_conversation_circle, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.circleRv.layoutManager = LinearLayoutManager(requireContext())
        conversationAdapter.currentCircleId = defaultSharedPreferences.getString(CIRCLE_ID, null)
        val callback: ItemTouchHelper.Callback = SimpleItemTouchHelperCallback(conversationAdapter)
        itemTouchHelper = ItemTouchHelper(callback)
        binding.circleRv.adapter = conversationAdapter
        itemTouchHelper.attachToRecyclerView(binding.circleRv)
        conversationViewModel.observeAllCircleItem().observe(
            viewLifecycleOwner,
            {
                val list = mutableListOf<ConversationCircleItem>()
                list.addAll(it)
                conversationAdapter.conversationCircles = list
            }
        )
        conversationViewModel.observeAllConversationUnread().observe(
            viewLifecycleOwner,
            {
                conversationAdapter.allUnread = it
            }
        )
    }

    private val conversationAdapter by lazy {
        ConversationCircleAdapter(
            this,
            { name, circleId ->
                (requireActivity() as MainActivity).selectCircle(name, circleId)
            },
            { view, conversationCircleItem ->
                showMenu(view, conversationCircleItem)
            },
            {
                (requireActivity() as MainActivity).sortAction()
            },
            {
                conversationViewModel.sortCircleConversations(it)
            }
        )
    }

    override fun onBackPressed(): Boolean {
        if (conversationAdapter.sorting) {
            conversationAdapter.cancelSort()
            return true
        }
        return false
    }

    fun cancelSort() {
        conversationAdapter.cancelSort()
    }

    class ConversationCircleAdapter(
        private val dragStartListener: OnStartDragListener,
        val action: (String?, String?) -> Unit,
        val showMenu: (View, ConversationCircleItem) -> Unit,
        val sortAction: () -> Unit,
        val updateAction: (List<CircleOrder>?) -> Unit
    ) :
        RecyclerView.Adapter<ConversationCircleHolder>(), ItemTouchHelperAdapter {
        var conversationCircles: MutableList<ConversationCircleItem>? = null
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                if (sorting) return
                field = value
                notifyDataSetChanged()
            }

        var currentCircleId: String? = null
        var allUnread: Int? = null
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                if (currentCircleId != null) notifyDataSetChanged()
            }

        fun cancelSort() {
            if (sorting) {
                val now = System.currentTimeMillis()
                val data = conversationCircles?.let { list ->
                    list.mapIndexed { index, item ->
                        CircleOrder(item.circleId, Instant.ofEpochMilli(now + index).toString())
                    }
                }
                sorting = false
                updateAction(data)
            }
        }

        var sorting = false
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                if (field != value) {
                    field = value
                    notifyDataSetChanged()
                    if (value) {
                        sortAction()
                    }
                }
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationCircleHolder =
            if (viewType == 1) {
                ConversationCircleHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_conversation_circle, parent, false))
            } else {
                ConversationCircleHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_conversation_circle_bottom, parent, false))
            }

        override fun getItemCount(): Int = conversationCircles.notEmptyWithElse({ it.size + 1 }, 2)

        override fun getItemViewType(position: Int): Int =
            if (conversationCircles.isNullOrEmpty() && position == 1) {
                0
            } else {
                1
            }

        private fun getItem(position: Int): ConversationCircleItem? {
            return if (position == 0) {
                return null
            } else {
                conversationCircles?.get(position - 1)
            }
        }

        @SuppressLint("ClickableViewAccessibility", "NotifyDataSetChanged")
        override fun onBindViewHolder(holder: ConversationCircleHolder, position: Int) {
            if (getItemViewType(position) == 1) {
                val conversationCircleItem = getItem(position)
                holder.bind(sorting, currentCircleId, conversationCircleItem, allUnread)
                holder.itemView.setOnClickListener {
                    currentCircleId = conversationCircleItem?.circleId
                    action(conversationCircleItem?.name, currentCircleId)
                    notifyDataSetChanged()
                }
                if (sorting) {
                    holder.itemView.setOnLongClickListener(null)
                    holder.itemView.setOnTouchListener { _, event ->
                        if (event.action and (MotionEvent.ACTION_DOWN) == 0) {
                            dragStartListener.onStartDrag(holder)
                        }
                        false
                    }
                } else {
                    holder.itemView.setOnTouchListener(null)
                    holder.itemView.setOnLongClickListener {
                        if (conversationCircleItem != null) {
                            showMenu(holder.circleTitle, conversationCircleItem)
                            true
                        } else {
                            false
                        }
                    }
                }
            }
        }

        override fun onItemDismiss(position: Int) {
            if (position == 0) return
            notifyItemRemoved(position)
        }

        override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
            if (fromPosition == 0 || toPosition == 0) return false
            conversationCircles?.let { conversationCircles ->
                Collections.swap(conversationCircles, fromPosition - 1, toPosition - 1)
                notifyItemMoved(fromPosition, toPosition)
            }
            return true
        }
    }

    private fun showMenu(view: View, conversationCircleItem: ConversationCircleItem) {
        val popMenu = PopupMenu(requireContext(), view)
        popMenu.menuInflater.inflate(R.menu.circle_menu, popMenu.menu)
        popMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.rename -> {
                    rename(conversationCircleItem)
                }
                R.id.edit -> {
                    edit(conversationCircleItem)
                }
                R.id.delete -> {
                    delete(conversationCircleItem)
                }
                R.id.sort -> {
                    conversationAdapter.sorting = true
                }

                else -> {
                }
            }
            true
        }
        popMenu.show()
    }

    private fun rename(conversationCircleItem: ConversationCircleItem) {
        editDialog {
            titleText = this@CirclesFragment.getString(R.string.circle_menu_edit_name)
            editText = conversationCircleItem.name
            maxTextCount = 64
            editMaxLines = EditDialog.MAX_LINE.toInt()
            allowEmpty = false
            rightText = android.R.string.ok
            rightAction = {
                rename(conversationCircleItem.circleId, it)
            }
        }
    }

    private fun rename(circleId: String, name: String) {
        lifecycleScope.launch(errorHandler) {
            val response = conversationViewModel.circleRename(circleId, name)
            if (response.isSuccess) {
                response.data?.let { circle ->
                    conversationViewModel.insertCircle(circle)
                    (requireActivity() as MainActivity).setCircleName(circle.name)
                }
            } else {
                ErrorHandler.handleMixinError(response.errorCode, response.errorDescription)
            }
        }
    }

    fun edit(conversationCircleItem: ConversationCircleItem) {
        requireActivity().addFragment(
            this@CirclesFragment,
            ConversationCircleEditFragment.newInstance(conversationCircleItem),
            ConversationCircleEditFragment.TAG,
            R.id.root_view
        )
    }

    private fun delete(conversationCircleItem: ConversationCircleItem) {
        alertDialogBuilder()
            .setMessage(getString(R.string.circle_delete_tip, conversationCircleItem.name))
            .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.action_confirm) { _, _ ->
                lifecycleScope.launch(errorHandler) {
                    val response = conversationViewModel.deleteCircle(conversationCircleItem.circleId)
                    if (response.isSuccess) {
                        conversationViewModel.deleteCircleById(conversationCircleItem.circleId)
                        if (conversationAdapter.currentCircleId == conversationCircleItem.circleId) {
                            (requireActivity() as MainActivity).selectCircle(null, null)
                        }
                    } else {
                        ErrorHandler.handleMixinError(response.errorCode, response.errorDescription)
                    }
                }
            }
            .show()
    }

    class ConversationCircleHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val shakeAnimator by lazy {
            itemView.shakeAnimator()
        }

        val binding get() = ItemConversationCircleBinding.bind(itemView)
        val circleTitle get() = binding.circleTitle

        fun bind(sorting: Boolean, currentCircleId: String?, conversationCircleItem: ConversationCircleItem?, allUnread: Int?) {
            if (sorting) {
                shakeAnimator.start()
            } else {
                shakeAnimator.cancel()
            }
            binding.apply {
                if (conversationCircleItem == null) {
                    circleTitle.setText(R.string.app_name)
                    circleSubtitle.setText(R.string.circle_all_conversation)
                    circleUnreadTv.isVisible =
                        currentCircleId != null && allUnread != 0 && allUnread != null
                    circleUnreadTv.text = "$allUnread"
                    circleCheck.isVisible = currentCircleId == null
                } else {
                    circleTitle.text = conversationCircleItem.name
                    circleSubtitle.text = itemView.context.resources.getQuantityString(R.plurals.circle_subtitle, conversationCircleItem.count, conversationCircleItem.count)
                    circleUnreadTv.isVisible =
                        currentCircleId != conversationCircleItem.circleId && conversationCircleItem.unseenMessageCount != 0
                    circleUnreadTv.text = "${conversationCircleItem.unseenMessageCount}"
                    circleCheck.isVisible = currentCircleId == conversationCircleItem.circleId
                }
                circleIcon.imageTintList =
                    ColorStateList.valueOf(getCircleColor(conversationCircleItem?.circleId))
            }
        }
    }

    private lateinit var itemTouchHelper: ItemTouchHelper
    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        if (conversationAdapter.sorting) {
            itemTouchHelper.startDrag(viewHolder)
        }
    }
}
