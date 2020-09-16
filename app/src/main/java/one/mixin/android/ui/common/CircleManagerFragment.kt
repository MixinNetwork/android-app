package one.mixin.android.ui.common

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_circle_manager.*
import kotlinx.android.synthetic.main.item_circle_manager.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.ConversationCircleRequest
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.notEmptyWithElse
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.withArgs
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.vo.CircleConversation
import one.mixin.android.vo.CircleConversationAction
import one.mixin.android.vo.ConversationCircleManagerItem
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.getCircleColor
import one.mixin.android.widget.SegmentationItemDecoration

@AndroidEntryPoint
class CircleManagerFragment : BaseFragment() {
    companion object {
        const val TAG = "CirclesFragment"
        private const val NAME = "name"
        private const val CONVERSATION_ID = "conversation_id"
        private const val USER_ID = "user_id"

        fun newInstance(name: String?, conversationId: String? = null, userId: String? = null): CircleManagerFragment {
            require(!(conversationId == null && userId == null)) {
                "empty data"
            }
            return CircleManagerFragment().withArgs {
                name?.let { putString(NAME, name) }
                putString(USER_ID, userId)
                putString(CONVERSATION_ID, conversationId)
            }
        }
    }

    private val name: String by lazy {
        requireArguments().getString(NAME, "")
    }

    private val conversationId: String? by lazy {
        requireArguments().getString(CONVERSATION_ID)
    }

    private val userId: String? by lazy {
        requireArguments().getString(USER_ID)
    }

    private val bottomViewModel by viewModels<BottomSheetViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_circle_manager, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.setSubTitle(getString(R.string.circle_title, name), "")
        title_view.left_ib.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            activity?.onBackPressed()
        }
        title_view.right_ib.setOnClickListener {
            if (!isAdded) return@setOnClickListener

            addCircle()
        }
        circle_add.setOnClickListener {
            if (!isAdded) return@setOnClickListener

            addCircle()
        }
        circle_manager_rv.adapter = circleAdapter
        circle_manager_rv.addItemDecoration(SegmentationItemDecoration())
        loadData()
    }

    private fun loadData() {
        bottomViewModel.viewModelScope.launch {
            val includeCircleItem = bottomViewModel.getIncludeCircleItem(conversationId ?: generateConversationId(Session.getAccountId()!!, userId!!))
            val otherCircleItem = bottomViewModel.getOtherCircleItem(conversationId ?: generateConversationId(Session.getAccountId()!!, userId!!))
            circle_manager_rv.isVisible = includeCircleItem.isNotEmpty() || otherCircleItem.isNotEmpty()
            empty.isVisible = includeCircleItem.isEmpty() && otherCircleItem.isEmpty()
            circleAdapter.setData(includeCircleItem, otherCircleItem)
        }
    }

    private val onAddCircle: (item: ConversationCircleManagerItem) -> Unit = { item ->
        lifecycleScope.launch {
            val dialog = indeterminateProgressDialog(message = R.string.pb_dialog_message).apply {
                setCancelable(false)
            }
            val requests = listOf(ConversationCircleRequest(item.circleId, CircleConversationAction.ADD.name))
            handleMixinResponse(
                invokeNetwork = {
                    bottomViewModel.updateCircles(conversationId, userId, requests)
                },
                successBlock = {
                    it.data?.forEach { circleConversation ->
                        bottomViewModel.insertCircleConversation(
                            CircleConversation(
                                circleConversation.conversationId,
                                circleConversation.circleId,
                                userId,
                                circleConversation.createdAt,
                                null
                            )
                        )
                    }
                    dialog.dismiss()
                    loadData()
                },
                exceptionBlock = {
                    dialog.dismiss()
                    return@handleMixinResponse false
                },
                failureBlock = {
                    dialog.dismiss()
                    return@handleMixinResponse false
                }
            )
        }
    }

    private val onRemoveCircle: (item: ConversationCircleManagerItem) -> Unit = { item ->
        lifecycleScope.launch {
            val dialog = indeterminateProgressDialog(message = R.string.pb_dialog_message).apply {
                setCancelable(false)
            }
            val requests = listOf(ConversationCircleRequest(item.circleId, CircleConversationAction.REMOVE.name))
            handleMixinResponse(
                switchContext = Dispatchers.IO,
                invokeNetwork = {
                    bottomViewModel.updateCircles(conversationId, userId, requests)
                },
                successBlock = {
                    bottomViewModel.deleteCircleConversation(conversationId ?: generateConversationId(Session.getAccountId()!!, userId!!), item.circleId)
                    loadData()
                    dialog.dismiss()
                },
                exceptionBlock = {
                    dialog.dismiss()
                    return@handleMixinResponse false
                },
                failureBlock = {
                    dialog.dismiss()
                    return@handleMixinResponse false
                }
            )
        }
    }

    private fun addCircle() {
        editDialog {
            titleText = this@CircleManagerFragment.getString(R.string.circle_add_title)
            maxTextCount = 64
            defaultEditEnable = false
            editMaxLines = EditDialog.MAX_LINE.toInt()
            allowEmpty = false
            rightText = android.R.string.ok
            rightAction = {
                createCircle(it)
            }
        }
    }

    private fun createCircle(name: String) {
        lifecycleScope.launch(ErrorHandler.errorHandler) {
            val dialog = indeterminateProgressDialog(message = R.string.pb_dialog_message).apply {
                setCancelable(false)
            }
            val response = bottomViewModel.createCircle(name)
            if (response.isSuccess) {
                response.data?.let { circle ->
                    bottomViewModel.insertCircle(circle)
                }
                loadData()
            } else {
                ErrorHandler.handleMixinError(response.errorCode, response.errorDescription)
            }
            dialog.dismiss()
        }
    }

    private val circleAdapter by lazy {
        CircleAdapter(onAddCircle, onRemoveCircle)
    }

    class CircleAdapter(
        private val onAddCircle: (item: ConversationCircleManagerItem) -> Unit,
        private val onRemoveCircle: (item: ConversationCircleManagerItem) -> Unit
    ) : RecyclerView.Adapter<CircleHolder>() {

        private var includeCircleItem: List<ConversationCircleManagerItem>? = null
        private var otherCircleItem: List<ConversationCircleManagerItem>? = null

        fun setData(includeCircleItem: List<ConversationCircleManagerItem>, otherCircleItem: List<ConversationCircleManagerItem>) {
            this.includeCircleItem = includeCircleItem
            this.otherCircleItem = otherCircleItem
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CircleHolder =
            CircleHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_circle_manager, parent, false))

        override fun getItemCount(): Int = (
            includeCircleItem.notEmptyWithElse({ it.size }, 0) +
                otherCircleItem.notEmptyWithElse({ it.size }, 0)
            )

        override fun getItemViewType(position: Int): Int {
            val favoriteSize = includeCircleItem.notNullWithElse({ it.size }, 0)
            return when {
                position < favoriteSize -> {
                    0
                }
                position < favoriteSize + otherCircleItem.notNullWithElse({ it.size }, 0) -> {
                    1
                }
                else -> {
                    2
                }
            }
        }

        private fun getItem(position: Int): ConversationCircleManagerItem {
            return when (getItemViewType(position)) {
                0 -> {
                    includeCircleItem!![position]
                }
                1 -> {
                    val favoriteSize = includeCircleItem.notNullWithElse({ it.size }, 0)
                    otherCircleItem!![position - favoriteSize]
                }
                else -> {
                    throw IllegalArgumentException("Conversation Circle type error")
                }
            }
        }

        override fun onBindViewHolder(holder: CircleHolder, position: Int) {
            when (getItemViewType(position)) {
                0 -> {
                    holder.bind(getItem(position), onRemoveCircle = onRemoveCircle)
                }
                1 -> {
                    holder.itemView.tag = !includeCircleItem.isNullOrEmpty() && position == includeCircleItem?.size
                    holder.bind(getItem(position), onAddCircle = onAddCircle)
                }
                else -> {
                    holder.bind(getItem(position))
                }
            }
        }
    }

    class CircleHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(
            conversationCircleItem: ConversationCircleManagerItem,
            onAddCircle: ((conversationCircleItem: ConversationCircleManagerItem) -> Unit)? = null,
            onRemoveCircle: ((conversationCircleItem: ConversationCircleManagerItem) -> Unit)? = null
        ) {
            if (onAddCircle != null) {
                itemView.action_iv.setImageResource(R.drawable.ic_add_circle)
                itemView.action_iv.setOnClickListener {
                    onAddCircle.invoke(conversationCircleItem)
                }
            } else {
                itemView.action_iv.setImageResource(R.drawable.ic_remove_circle)
                itemView.action_iv.setOnClickListener {
                    onRemoveCircle?.invoke(conversationCircleItem)
                }
            }
            itemView.circle_title.text = conversationCircleItem.name
            itemView.circle_subtitle.text = itemView.context.getString(R.string.circle_subtitle, conversationCircleItem.count)
            itemView.circle_icon.imageTintList = ColorStateList.valueOf(getCircleColor(conversationCircleItem.circleId))
        }
    }
}
