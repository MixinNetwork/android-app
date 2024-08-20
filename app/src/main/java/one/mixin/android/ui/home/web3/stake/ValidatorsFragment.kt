package one.mixin.android.ui.home.web3.stake

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import one.mixin.android.api.response.web3.Validator
import one.mixin.android.ui.common.BaseFragment

@AndroidEntryPoint
class ValidatorsFragment : BaseFragment() {
    companion object {
        const val TAG = "ValidatorsFragment"

        fun newInstance() = ValidatorsFragment()
    }

    private val stakeViewModel by viewModels<StakeViewModel>()

    private var validators: MutableList<Validator> = SnapshotStateList()
    private var filterValidators: MutableList<Validator> = SnapshotStateList()

    private val textInputFlow = MutableStateFlow("")
    private var searchText: String by mutableStateOf("")
    private var isLoading by mutableStateOf(false)

    @OptIn(FlowPreview::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        lifecycleScope.launch {
            loadValidators()

            textInputFlow
                .debounce(500L)
                .distinctUntilChanged()
                .collectLatest { text ->
                    searchValidator(text)
                }
        }
        return ComposeView(inflater.context).apply {
            setContent {
                ValidatorsPage(
                    searchText = searchText,
                    isLoading = isLoading,
                    validators = validators,
                    filterValidators = filterValidators,
                    onClick = { v ->
                        activity?.onBackPressedDispatcher?.onBackPressed()
                        onSelect?.invoke(v)
                    },
                    onInputChanged = {
                        searchText = it
                        textInputFlow.value = it
                    },
                ) {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
            }
        }
    }

    private suspend fun searchValidator(query: String) {
        if (query.isBlank()) {
            filterValidators.clear()
            return
        }
        isLoading = true
        val result = stakeViewModel.searchStakeValidators(query.trim())
        isLoading = false
        filterValidators.clear()
        if (!result.isNullOrEmpty()) {
            filterValidators.addAll(result)
        }
    }

    private suspend fun loadValidators() {
        validators.clear()
        val vs = stakeViewModel.getStakeValidators(null)
        if (vs.isNullOrEmpty()) {
            return loadValidators()
        }
        validators.addAll(vs)
    }

    private var onSelect: ((Validator) -> Unit)? = null

    fun setOnSelect(action: (Validator) -> Unit): ValidatorsFragment {
        onSelect = action
        return this
    }
}