package one.mixin.android.ui.home.circle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_coversation_circle.*
import kotlinx.android.synthetic.main.item_conversation_circle.view.*
import one.mixin.android.R
import one.mixin.android.extension.notEmptyWithElse
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.ConversationListViewModel
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.ConversationCircleItem

class ConversationCircleFragment : BaseFragment() {
    companion object {
        const val TAG = "ConversationCircleFragment"

        fun newInstance(): ConversationCircleFragment {
            return ConversationCircleFragment()
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
        circle_rv.adapter = conversationAdapter
        conversationViewModel.observeAllCircleItem().observe(this, Observer {
            GsonHelper.customGson.toJson(it)
            conversationAdapter.conversationCircles = it
        })
    }

    private val conversationAdapter by lazy {
        ConversationCircleAdapter { name, circleId ->
            (requireActivity() as MainActivity).selectCircle(name, circleId)
        }
    }

    class ConversationCircleAdapter(val action: (String?, String?) -> Unit) : RecyclerView.Adapter<ConversationCircleHolder>() {
        var conversationCircles: List<ConversationCircleItem>? = null
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        private var currentCircleId: String? = null

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
                holder.bind(currentCircleId, conversationCircleItem)
                holder.itemView.setOnClickListener {
                    currentCircleId = conversationCircleItem?.circleId
                    action(conversationCircleItem?.name, currentCircleId)
                    notifyDataSetChanged()
                }
            }
        }
    }

    class ConversationCircleHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(currentCircleId: String?, conversationCircleItem: ConversationCircleItem?) {
            if (conversationCircleItem == null) {
                itemView.circle_icon.setImageResource(R.drawable.ic_circle_mixin)
                itemView.circle_title.setText(R.string.circle_mixin)
                itemView.circle_subtitle.setText(R.string.circle_all_conversation)
                itemView.circle_unread_tv.isVisible = false
                itemView.circle_check.isVisible = currentCircleId == null
            } else {
                itemView.circle_icon.setImageResource(R.drawable.ic_circle)
                itemView.circle_title.text = conversationCircleItem.name
                itemView.circle_subtitle.text = itemView.context.getString(R.string.circle_subtitle, conversationCircleItem.count)
                itemView.circle_unread_tv.isVisible = currentCircleId != conversationCircleItem.circleId && conversationCircleItem.unseenMessageCount != 0
                itemView.circle_unread_tv.text = "${conversationCircleItem.unseenMessageCount}"
                itemView.circle_check.isVisible = currentCircleId == conversationCircleItem.circleId
            }
        }
    }
}
