package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentBotSignAppBottomSheetBinding
import one.mixin.android.databinding.ItemBotSignAppBinding
import one.mixin.android.db.property.PropertyHelper.findValueByKey
import one.mixin.android.db.property.PropertyHelper.updateKeyValue
import one.mixin.android.extension.loadImage
import one.mixin.android.repository.UserRepository
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.App
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SearchView
import javax.inject.Inject

@AndroidEntryPoint
class BotSignAppBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "BotSignAppBottomSheetDialogFragment"

        fun newInstance() = BotSignAppBottomSheetDialogFragment()
    }

    @Inject
    lateinit var userRepository: UserRepository

    private val binding by viewBinding(FragmentBotSignAppBottomSheetBinding::inflate)
    private val selectedAppIds = mutableSetOf<String>()
    private var apps = emptyList<App>()
    private var appsLoaded = false

    private val adapter by lazy {
        BotSignAppAdapter(selectedAppIds) { app ->
            if (!selectedAppIds.add(app.appId)) {
                selectedAppIds.remove(app.appId)
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)

        binding.titleView.titleTv.setText(R.string.Bot_sign_debug_apps)
        binding.titleView.rightIv.setOnClickListener { dismiss() }
        binding.searchView.listener =
            object : SearchView.OnSearchViewListener {
                override fun afterTextChanged(s: Editable?) {
                    filterApps(s?.toString().orEmpty())
                }

                override fun onSearch() = Unit
            }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.resetButton.setOnClickListener {
            selectedAppIds.clear()
            adapter.notifyDataSetChanged()
        }
        binding.applyButton.setOnClickListener {
            saveSelection()
        }

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val apps = userRepository.getApps().sortedBy { it.name }
                val selectedAppIds = apps
                    .filter { findValueByKey(Constants.Debug.botSignDebugAppKey(it.appId), false) }
                    .mapTo(mutableSetOf()) { it.appId }
                apps to selectedAppIds
            }
            if (!isAdded) return@launch

            apps = result.first
            selectedAppIds.clear()
            selectedAppIds.addAll(result.second)
            appsLoaded = true
            filterApps(binding.searchView.et.text.toString())
            binding.progressBar.isGone = true
        }
    }

    private fun filterApps(keyword: String) {
        if (!appsLoaded) return

        val filteredApps = if (keyword.isBlank()) {
            apps
        } else {
            apps.filter { app ->
                app.name.contains(keyword, ignoreCase = true) ||
                    app.appNumber.contains(keyword, ignoreCase = true)
            }
        }
        adapter.submitList(filteredApps)
        binding.recyclerView.isVisible = filteredApps.isNotEmpty()
        binding.empty.isVisible = filteredApps.isEmpty()
    }

    private fun saveSelection() {
        val selected = selectedAppIds.toSet()
        lifecycleScope.launch(Dispatchers.IO) {
            apps.forEach { app ->
                updateKeyValue(
                    Constants.Debug.botSignDebugAppKey(app.appId),
                    selected.contains(app.appId),
                )
            }
            withContext(Dispatchers.Main) {
                dismiss()
            }
        }
    }
}

private class BotSignAppAdapter(
    private val selectedAppIds: Set<String>,
    private val onAppClick: (App) -> Unit,
) : ListAdapter<App, BotSignAppViewHolder>(App.DIFF_CALLBACK) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BotSignAppViewHolder {
        return BotSignAppViewHolder(
            ItemBotSignAppBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )
    }

    override fun onBindViewHolder(
        holder: BotSignAppViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position), selectedAppIds) { app ->
            onAppClick(app)
            if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(holder.bindingAdapterPosition)
            }
        }
    }
}

private class BotSignAppViewHolder(
    private val binding: ItemBotSignAppBinding,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(
        app: App,
        selectedAppIds: Set<String>,
        onAppClick: (App) -> Unit,
    ) {
        binding.avatar.loadImage(app.iconUrl)
        binding.nameTv.text = app.name
        binding.appNumberTv.text = app.appNumber
        binding.cb.isClickable = false
        binding.cb.isChecked = selectedAppIds.contains(app.appId)
        itemView.setOnClickListener {
            if (bindingAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
            onAppClick(app)
        }
    }
}
