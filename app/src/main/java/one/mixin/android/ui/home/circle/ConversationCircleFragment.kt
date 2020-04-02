package one.mixin.android.ui.home.circle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_coversation_circle.*
import kotlinx.android.synthetic.main.item_conversation_circle.view.*
import one.mixin.android.R
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.ConversationListViewModel
import one.mixin.android.vo.ConversationCircleItem
import timber.log.Timber
import javax.inject.Inject

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
    }

    private val conversationAdapter by lazy {
        ConversationCircleAdapter {
            Timber.d(it)
        }
    }

    class ConversationCircleAdapter(val action: (String?) -> Unit) : RecyclerView.Adapter<ConversationCircleHolder>() {
        var conversationCircles: List<ConversationCircleItem>? = null
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationCircleHolder =
            if (viewType == 1) {
                ConversationCircleHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_conversation_circle, parent, false))
            } else {
                ConversationCircleHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_conversation_circle_bottom, parent, false))
            }

        override fun getItemCount(): Int = conversationCircles.notNullWithElse({ it.size + 1 }, 2)

        override fun getItemViewType(position: Int): Int =
            if (conversationCircles == null && position == 1) {
                0
            } else {
                1
            }

        override fun onBindViewHolder(holder: ConversationCircleHolder, position: Int) {
            if (getItemViewType(position) == 1) {
                holder.itemView.setOnClickListener {
                    action(null)
                }
            }
        }
    }

    class ConversationCircleHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(conversationCircleItem: ConversationCircleItem?) {
            if (conversationCircleItem == null) {
                itemView.circle_icon.setImageResource(R.drawable.ic_circle_mixin)
            } else {
                itemView.circle_icon.setImageResource(R.drawable.ic_circle)
            }
        }
    }
}
