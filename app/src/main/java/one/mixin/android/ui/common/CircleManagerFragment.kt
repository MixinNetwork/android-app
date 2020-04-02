package one.mixin.android.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_coversation_circle.*
import one.mixin.android.R
import one.mixin.android.vo.ConversationCircleItem
import one.mixin.android.widget.recyclerview.ItemTouchHelperAdapter
import one.mixin.android.widget.recyclerview.OnStartDragListener
import one.mixin.android.widget.recyclerview.SimpleItemTouchHelperCallback

class CircleManagerFragment : BaseFragment(), OnStartDragListener {
    companion object {
        const val TAG = "ConversationCircleFragment"

        fun newInstance(): CircleManagerFragment {
            return CircleManagerFragment()
        }
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
        circle_rv.adapter = circleAdapter
        val callback: ItemTouchHelper.Callback = SimpleItemTouchHelperCallback(circleAdapter)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(circle_rv)
        // bottomViewModel.observeAllCircleItem().observe(this, Observer {
        // })
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
