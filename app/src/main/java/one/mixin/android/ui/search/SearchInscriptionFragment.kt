package one.mixin.android.ui.search

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CancellationSignal
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.jakewharton.rxbinding3.widget.textChanges
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.Account.PREF_RECENT_USED_BOTS
import one.mixin.android.Constants.RECENT_USED_BOTS_MAX_COUNT
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSearchInscriptionBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.deserialize
import one.mixin.android.extension.dp
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.adapter.StickerSpacingItemDecoration
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.home.inscription.CollectiblesAdapter
import one.mixin.android.ui.home.inscription.InscriptionActivity
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.User
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class SearchInscriptionFragment : BaseFragment(R.layout.fragment_search_inscription) {
    private val searchViewModel by viewModels<SearchViewModel>()

    private val searchAdapter by lazy {
        CollectiblesAdapter {
            InscriptionActivity.show(requireContext(), it.inscriptionHash)
        }
    }

    companion object {
        const val TAG = "SearchInscriptionFragment"
        const val SEARCH_DEBOUNCE = 300L
    }

    private var keyword: String? = null
        set(value) {
            if (field != value) {
                field = value
                bindData()
            }
        }

    private fun setQueryText(text: String) {
        if (isAdded && text != keyword) {
            keyword = text
        }
    }

    private var searchJob: Job? = null

    @Suppress("UNCHECKED_CAST")
    private fun bindData(keyword: String? = this@SearchInscriptionFragment.keyword) {
        searchJob?.cancel()
        searchJob = fuzzySearch(keyword)
    }

    private val binding by viewBinding(FragmentSearchInscriptionBinding::bind)
    private val padding: Int by lazy {
        15.dp
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        view.setOnClickListener {
            if (keyword.isNullOrBlank()) {
                (requireActivity() as MainActivity).closeSearch()
            }
        }
        binding.searchRv.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.searchRv.addItemDecoration(StickerSpacingItemDecoration(2, padding, true))
        binding.searchRv.adapter = searchAdapter

        binding.backIb.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        lifecycleScope.launch {
            delay(200)
            if (isAdded) {
                binding.searchEt.showKeyboard()
            }
        }

        binding.searchEt.textChanges().debounce(SEARCH_DEBOUNCE, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(stopScope)
            .subscribe(
                {
                    setQueryText(it.toString())
                },
                {},
            )
        lifecycleScope.launch {
            refreshRecentUsedApps()
            fuzzySearch(null)
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            refreshRecentUsedApps()
        }
    }

    private suspend fun refreshRecentUsedApps() {
        val apps =
            withContext(Dispatchers.IO) {
                var botsList =
                    defaultSharedPreferences.getString(PREF_RECENT_USED_BOTS, null)?.split("=")
                        ?: return@withContext null
                if (botsList.size == 1 && !botsList[0].isUUID()) {
                    getPreviousVersionBotsList()?.let {
                        botsList = it
                    }
                }
                if (botsList.isEmpty()) return@withContext null
                val result = searchViewModel.findBotsByIds(botsList.take(RECENT_USED_BOTS_MAX_COUNT).toSet())
                if (result.isEmpty()) return@withContext null
                result.sortedBy {
                    botsList.indexOf(it.appId)
                }
            }
        recentUsedBots = apps
    }

    private var recentUsedBots: List<User>? = null

    private fun getPreviousVersionBotsList(): List<String>? {
        defaultSharedPreferences.getString(PREF_RECENT_USED_BOTS, null)?.let { botsString ->
            return botsString.deserialize<Array<String>>()?.toList()
        } ?: return null
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fuzzySearch(keyword: String?) =
        lifecycleScope.launch {
            if (viewDestroyed()) return@launch
            if (keyword.isNullOrBlank()) {
                binding.searchRv.isVisible = true
                binding.empty.isVisible = false
                searchAdapter.notifyDataSetChanged()
            } else {
                val cancellationSignal = CancellationSignal()
                val inscriptions = searchViewModel.fuzzyInscription(cancellationSignal, keyword)
                searchAdapter.list = inscriptions ?: emptyList()
                if (searchAdapter.itemCount <= 0) {
                    binding.searchRv.isVisible = false
                    binding.empty.isVisible = true
                } else {
                    binding.searchRv.isVisible = true
                    binding.empty.isVisible = false
                }
                searchAdapter.notifyDataSetChanged()
            }
        }
}
