package one.mixin.android.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_circle_manager.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.withArgs
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.ConversationCircleItem
import one.mixin.android.widget.recyclerview.ItemTouchHelperAdapter
import one.mixin.android.widget.recyclerview.OnStartDragListener

class CircleManagerFragment : BaseFragment(), OnStartDragListener {
    companion object {
        const val TAG = "ConversationCircleFragment"
        private const val NAME = "name"
        private const val CONVERSATION_ID = "conversation_id"
        private const val USER_ID = "user_id"

        fun newInstance(name: String?, userId: String? = null, conversationId: String? = null): CircleManagerFragment {
            return CircleManagerFragment().withArgs {
                name?.let { putString(NAME, name) }
                userId?.let { putString(USER_ID, it) }
                conversationId?.let { putString(CONVERSATION_ID, it) }
            }
        }
    }

    private val name: String by lazy {
        arguments!!.getString(NAME, "")
    }
    private val userId: String? by lazy {
        arguments!!.getString(USER_ID)
    }
    private val conversationId: String? by lazy {
        arguments!!.getString(CONVERSATION_ID)
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
        // circle_rv.adapter = circleAdapter
        // val callback: ItemTouchHelper.Callback = SimpleItemTouchHelperCallback(circleAdapter)
        // itemTouchHelper = ItemTouchHelper(callback)
        // itemTouchHelper.attachToRecyclerView(circle_rv)
        // bottomViewModel.observeAllCircleItem().observe(this, Observer {
        // })
    }

    private fun addCircle() {
        editDialog {
            titleText = this@CircleManagerFragment.getString(R.string.circle_add_title)
            maxTextCount = 128
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
            val response = bottomViewModel.createCircle(name)
            if (response.isSuccess) {
                response.data?.let { circle ->
                    bottomViewModel.insertCircle(circle)
                }
            } else {
                ErrorHandler.handleMixinError(response.errorCode, response.errorDescription)
            }
        }
    }

    private val circleAdapter by lazy {
        CircleAdapter()
    }

    class CircleAdapter : RecyclerView.Adapter<CircleHolder>(), ItemTouchHelperAdapter {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CircleHolder {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun getItemCount(): Int {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun onBindViewHolder(holder: CircleHolder, position: Int) {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun onItemDismiss(position: Int) {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }
    }

    class CircleHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(conversationCircleItem: ConversationCircleItem) {
        }
    }

    private lateinit var itemTouchHelper: ItemTouchHelper

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }
}
