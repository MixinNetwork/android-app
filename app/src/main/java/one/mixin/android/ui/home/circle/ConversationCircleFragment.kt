package one.mixin.android.ui.home.circle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_coversation_circle.*
import one.mixin.android.R
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.ConversationListViewModel
import one.mixin.android.vo.ConversationCircleItem
import timber.log.Timber

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
            ConversationCircleHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_conversation_circle, parent, false))

        override fun getItemCount(): Int = (conversationCircles?.size ?: 0) + 1

        override fun onBindViewHolder(holder: ConversationCircleHolder, position: Int) {
            holder.itemView.setOnClickListener {
                action(null)
            }
        }
    }

    class ConversationCircleHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
