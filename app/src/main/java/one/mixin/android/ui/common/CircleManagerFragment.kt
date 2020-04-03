package one.mixin.android.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_circle_manager.*
import kotlinx.android.synthetic.main.item_circle_manager.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.notEmptyWithElse
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.withArgs
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.ConversationCircleItem
import one.mixin.android.widget.SegmentationItemDecoration

class CircleManagerFragment : BaseFragment() {
    companion object {
        const val TAG = "ConversationCircleFragment"
        private const val NAME = "name"
        private const val CONVERSATION_ID = "conversation_id"

        fun newInstance(name: String?, conversationId: String? = null): CircleManagerFragment {
            return CircleManagerFragment().withArgs {
                name?.let { putString(NAME, name) }
                conversationId?.let { putString(CONVERSATION_ID, it) }
            }
        }
    }

    private val name: String by lazy {
        arguments!!.getString(NAME, "")
    }

    private val conversationId: String by lazy {
        arguments!!.getString(CONVERSATION_ID)!!
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val bottomViewModel: BottomSheetViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(BottomSheetViewModel::class.java)
    }

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
            val includeCircleItem = bottomViewModel.getIncludeCircleItem(conversationId)
            val otherCircleItem = bottomViewModel.getOtherCircleItem(conversationId)
            circle_manager_rv.isVisible = includeCircleItem.isNotEmpty() || otherCircleItem.isNotEmpty()
            empty.isVisible = includeCircleItem.isEmpty() && otherCircleItem.isEmpty()
            circleAdapter.setData(includeCircleItem, otherCircleItem)
        }
    }

    private val onAppCircle: (app: ConversationCircleItem) -> Unit = { app ->
        bottomViewModel.viewModelScope.launch {
            val dialog = indeterminateProgressDialog(message = R.string.pb_dialog_message).apply {
                setCancelable(false)
            }
            // try {
            //     if (bottomViewModel.addFavoriteApp(app.appId)) {
            //         loadData()
            //     }
            // } catch (e: Exception) {
            //     ErrorHandler.handleError(e)
            // }
            dialog.dismiss()
        }
    }
    private val onRemveCircle: (app: ConversationCircleItem) -> Unit = { app ->
        bottomViewModel.viewModelScope.launch {
            val dialog = indeterminateProgressDialog(message = R.string.pb_dialog_message).apply {
                setCancelable(false)
            }
            // try {
            //     if (bottomViewModel.removeFavoriteApp(app.appId, Session.getAccountId()!!)) {
            //         loadData()
            //     }
            // } catch (e: Exception) {
            //     ErrorHandler.handleError(e)
            // }
            dialog.dismiss()
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
        bottomViewModel.viewModelScope.launch(ErrorHandler.errorHandler) {
            val dialog = indeterminateProgressDialog(message = R.string.pb_dialog_message).apply {
                setCancelable(false)
            }
            val response = bottomViewModel.createCircle(name)
            if (response.isSuccess) {
                response.data?.let { circle ->
                    bottomViewModel.insertCircle(circle)
                }
            } else {
                ErrorHandler.handleMixinError(response.errorCode, response.errorDescription)
            }
            dialog.dismiss()
        }
    }

    private val circleAdapter by lazy {
        CircleAdapter({
            onAppCircle(it)
        }, {
            onRemveCircle(it)
        })
    }

    class CircleAdapter(
        private val onAppCircle: (conversationCircleItem: ConversationCircleItem) -> Unit,
        private val onRemveCircle: (conversationCircleItem: ConversationCircleItem) -> Unit
    ) : RecyclerView.Adapter<CircleHolder>() {

        private var includeCircleItem: List<ConversationCircleItem>? = null
        private var otherCircleItem: List<ConversationCircleItem>? = null

        fun setData(includeCircleItem: List<ConversationCircleItem>, otherCircleItem: List<ConversationCircleItem>) {
            this.includeCircleItem = includeCircleItem
            this.otherCircleItem = otherCircleItem
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CircleHolder =
            CircleHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_circle_manager, parent, false))

        override fun getItemCount(): Int = (includeCircleItem.notEmptyWithElse({ it.size }, 0) +
            otherCircleItem.notEmptyWithElse({ it.size }, 0)) + 1

        override fun getItemViewType(position: Int): Int {
            val favoriteSize = includeCircleItem.notNullWithElse({ it.size }, 0)
            return when {
                position == 0 -> {
                    2
                }
                position - 1 < favoriteSize -> {
                    0
                }
                position - 1 < favoriteSize + otherCircleItem.notNullWithElse({ it.size }, 0) -> {
                    1
                }
                else -> {
                    2
                }
            }
        }

        private fun getItem(position: Int): ConversationCircleItem? {
            return when (getItemViewType(position)) {
                0 -> {
                    includeCircleItem!![position - 1]
                }
                1 -> {
                    val favoriteSize = includeCircleItem.notNullWithElse({ it.size }, 0)
                    otherCircleItem!![position - 1 - favoriteSize]
                }
                else -> {
                    null
                }
            }
        }

        override fun onBindViewHolder(holder: CircleHolder, position: Int) {
            when (getItemViewType(position)) {
                0 -> {
                    holder.bind(getItem(position), onRemveCircle = onRemveCircle)
                }
                1 -> {
                    holder.itemView.tag = position == includeCircleItem.notNullWithElse({ it.size + 1 }, 1)
                    holder.bind(getItem(position), onAppCircle = onAppCircle)
                }
                else -> {
                    holder.bind(getItem(position))
                }
            }
        }
    }

    class CircleHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(
            conversationCircleItem: ConversationCircleItem?,
            onAppCircle: ((conversationCircleItem: ConversationCircleItem) -> Unit)? = null,
            onRemveCircle: ((conversationCircleItem: ConversationCircleItem) -> Unit)? = null
        ) {
            if (conversationCircleItem == null) {
                itemView.action_iv.setImageResource(R.drawable.ic_circle_disable)
                itemView.action_iv.setOnClickListener { }
                itemView.circle_icon.setImageResource(R.drawable.ic_circle_mixin)
                itemView.circle_title.setText(R.string.circle_mixin)
                itemView.circle_subtitle.setText(R.string.circle_all_conversation)
            } else {
                if (onAppCircle != null) {
                    itemView.action_iv.setImageResource(R.drawable.ic_add_circle)
                    itemView.action_iv.setOnClickListener {
                        onAppCircle.invoke(conversationCircleItem)
                    }
                } else {
                    itemView.action_iv.setImageResource(R.drawable.ic_remove_circle)
                    itemView.action_iv.setOnClickListener {
                        onRemveCircle?.invoke(conversationCircleItem)
                    }
                }
                itemView.circle_icon.setImageResource(R.drawable.ic_circle)
                itemView.circle_title.text = conversationCircleItem.name
                itemView.circle_subtitle.text = itemView.context.getString(R.string.circle_subtitle, conversationCircleItem.count)
            }
        }
    }
}
