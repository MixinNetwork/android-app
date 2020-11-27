package one.mixin.android.ui.search

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding3.widget.textChanges
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.view_head_search_single.view.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSearchSingleBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.search.SearchFragment.Companion.SEARCH_DEBOUNCE
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class SearchSingleFragment : BaseFragment() {
    companion object {
        const val TAG = "SearchSingleFragment"
        const val ARGS_LIST = "args_list"
        const val ARGS_QUERY = "args_query"

        fun newInstance(
            list: ArrayList<Parcelable>,
            query: String
        ) = SearchSingleFragment().withArgs {
            putParcelableArrayList(ARGS_LIST, list)
            putString(ARGS_QUERY, query)
        }
    }

    private val searchViewModel by viewModels<SearchViewModel>()

    private val data by lazy {
        requireArguments().getParcelableArrayList<Parcelable>(ARGS_LIST)
    }

    private val query by lazy {
        requireArguments().getString(ARGS_QUERY)!!
    }

    private val type by lazy {
        when (data!![0]) {
            is AssetItem -> TypeAsset
            is ChatMinimal -> TypeChat
            is User -> TypeUser
            else -> TypeMessage
        }
    }

    private val adapter by lazy {
        SearchSingleAdapter(type).apply { query = this@SearchSingleFragment.query }
    }

    private var _binding: FragmentSearchSingleBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchSingleBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backIb.setOnClickListener {
            binding.searchEt.hideKeyboard()
            requireActivity().onBackPressed()
        }
        binding.searchRv.layoutManager = LinearLayoutManager(requireContext())
        val header = LayoutInflater.from(requireContext()).inflate(R.layout.view_head_search_single, binding.searchRv, false)
        val text = when (type) {
            TypeAsset -> requireContext().getString(R.string.search_title_assets)
            TypeUser -> requireContext().getText(R.string.search_title_contacts)
            TypeChat -> requireContext().getText(R.string.search_title_chat)
            TypeMessage -> requireContext().getText(R.string.search_title_messages)
        }
        header.title_tv.text = text
        adapter.headerView = header
        binding.searchRv.adapter = adapter
        adapter.data = data
        adapter.onItemClickListener = object : SearchFragment.OnSearchClickListener {
            override fun onTipClick() {
            }

            override fun onAsset(assetItem: AssetItem) {
                activity?.let { WalletActivity.show(it, assetItem) }
            }

            override fun onMessageClick(message: SearchMessageItem) {
                binding.searchRv.hideKeyboard()
                val f = SearchMessageFragment.newInstance(message, adapter.query)
                requireActivity().addFragment(this@SearchSingleFragment, f, SearchMessageFragment.TAG, R.id.root_view)
            }

            override fun onChatClick(chatMinimal: ChatMinimal) {
                binding.searchRv.hideKeyboard()
                context?.let { ctx -> ConversationActivity.show(ctx, chatMinimal.conversationId) }
            }

            override fun onUserClick(user: User) {
                binding.searchRv.hideKeyboard()
                context?.let { ctx -> ConversationActivity.show(ctx, null, user.userId) }
            }
        }

        binding.clearIb.setOnClickListener { binding.searchEt.setText("") }
        binding.searchEt.hint = text
        binding.searchEt.setText(query)
        binding.searchEt.textChanges().debounce(SEARCH_DEBOUNCE, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe(
                {
                    binding.clearIb.isVisible = it.isNotEmpty()
                    if (it == adapter.query) return@subscribe

                    adapter.query = it.toString()
                    onTextChanged(it.toString())
                },
                {}
            )
    }

    private fun onTextChanged(s: String) = lifecycleScope.launch {
        if (!isAdded) return@launch

        val list: List<Parcelable>? = when (type) {
            TypeAsset -> searchViewModel.fuzzySearch<AssetItem>(s)
            TypeUser -> searchViewModel.fuzzySearch<User>(s)
            TypeChat -> searchViewModel.fuzzySearch<ChatMinimal>(s)
            TypeMessage -> searchViewModel.fuzzySearch<SearchMessageItem>(s, -1)
        }

        adapter.data = list
        adapter.notifyDataSetChanged()
    }
}
