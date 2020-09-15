package one.mixin.android.ui.home.circle

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_coversation_circle.*
import kotlinx.android.synthetic.main.item_conversation_circle.view.*
import kotlinx.coroutines.launch
import one.mixin.android.Constants.CIRCLE.CIRCLE_ID
import one.mixin.android.R
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
import one.mixin.android.vo.CircleOrder
import one.mixin.android.vo.ConversationCircleItem
import one.mixin.android.vo.getCircleColor
import one.mixin.android.widget.recyclerview.ItemTouchHelperAdapter
import one.mixin.android.widget.recyclerview.OnStartDragListener
import one.mixin.android.widget.recyclerview.SimpleItemTouchHelperCallback
import org.threeten.bp.Instant
import java.util.Collections
import javax.inject.Inject

@AndroidEntryPoint
class CirclesFragment : BaseFragment(), OnStartDragListener {
    companion object {
        const val TAG = "CirclesFragment"

        fun newInstance(): CirclesFragment {
            return CirclesFragment()
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val conversationViewModel: ConversationListViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(ConversationListViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_coversation_circle, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        circle_rv.layoutManager = LinearLayoutManager(requireContext())
        conversationAdapter.currentCircleId = defaultSharedPreferences.getString(CIRCLE_ID, null)
        val callback: ItemTouchHelper.Callback = SimpleItemTouchHelperCallback(conversationAdapter)
        itemTouchHelper = ItemTouchHelper(callback)
        circle_rv.adapter = conversationAdapter
        itemTouchHelper.attachToRecyclerView(circle_rv)
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
            set(value) {
                if (sorting) return
                field = value
                notifyDataSetChanged()
            }

        var currentCircleId: String? = null
        var allUnread: Int? = null
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
                            showMenu(it.circle_title, conversationCircleItem)
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
        conversationViewModel.viewModelScope.launch(errorHandler) {
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
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.confirm) { _, _ ->
                conversationViewModel.viewModelScope.launch(errorHandler) {
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

        fun bind(sorting: Boolean, currentCircleId: String?, conversationCircleItem: ConversationCircleItem?, allUnread: Int?) {
            if (sorting) {
                shakeAnimator.start()
            } else {
                shakeAnimator.cancel()
            }
            if (conversationCircleItem == null) {
                itemView.circle_title.setText(R.string.circle_mixin)
                itemView.circle_subtitle.setText(R.string.circle_all_conversation)
                itemView.circle_unread_tv.isVisible = currentCircleId != null && allUnread != 0 && allUnread != null
                itemView.circle_unread_tv.text = "$allUnread"
                itemView.circle_check.isVisible = currentCircleId == null
            } else {
                itemView.circle_title.text = conversationCircleItem.name
                itemView.circle_subtitle.text = itemView.context.getString(R.string.circle_subtitle, conversationCircleItem.count)
                itemView.circle_unread_tv.isVisible = currentCircleId != conversationCircleItem.circleId && conversationCircleItem.unseenMessageCount != 0
                itemView.circle_unread_tv.text = "${conversationCircleItem.unseenMessageCount}"
                itemView.circle_check.isVisible = currentCircleId == conversationCircleItem.circleId
            }
            itemView.circle_icon.imageTintList = ColorStateList.valueOf(getCircleColor(conversationCircleItem?.circleId))
        }
    }

    private lateinit var itemTouchHelper: ItemTouchHelper
    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        if (conversationAdapter.sorting) {
            itemTouchHelper.startDrag(viewHolder)
        }
    }
}
