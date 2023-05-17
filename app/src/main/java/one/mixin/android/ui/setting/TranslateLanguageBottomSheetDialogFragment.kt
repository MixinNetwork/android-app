package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.nl.translate.TranslateLanguage
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.databinding.FragmentSearchListBottomSheetBinding
import one.mixin.android.databinding.ItemLanguageBinding
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.extension.putString
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.TranslateManager
import one.mixin.android.util.getLanguageOrDefault
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SearchView

@AndroidEntryPoint
class TranslateLanguageBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "TranslateLanguageBottomSheetDialogFragment"

        fun newInstance() = TranslateLanguageBottomSheetDialogFragment()
    }

    private val binding by viewBinding(FragmentSearchListBottomSheetBinding::inflate)

    private val adapter = LanguageAdapter { language ->
        defaultSharedPreferences.putString(Constants.Account.PREF_TRANSLATE_TARGET_LANG, language.code)
        callback?.onLanguageClick(language)
        dismiss()
    }
    private val languages = TranslateLanguage.getAllLanguages().map { TranslateManager.Language(it) }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        context?.let { c ->
            val topOffset = c.statusBarHeight() + c.appCompatActionBarHeight()
            binding.root.heightOffset = topOffset
        }
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)

        binding.apply {
            closeIv.setOnClickListener { dismiss() }
            searchEt.listener = object : SearchView.OnSearchViewListener {
                override fun afterTextChanged(s: Editable?) {
                    filter(s.toString())
                }

                override fun onSearch() {
                }
            }
            binding.rv.adapter = adapter
            adapter.submitList(languages)
        }
    }

    private fun filter(s: String) {
        adapter.submitList(
            if (s.isNotBlank()) {
                languages.filter {
                    it.nameInCurrentLanguage.containsIgnoreCase(s) || it.nameInSelfLanguage.containsIgnoreCase(s)
                }.sortedByDescending {
                    it.nameInCurrentLanguage.equalsIgnoreCase(s) || it.nameInSelfLanguage.containsIgnoreCase(s)
                }
            } else {
                languages
            },
        )
    }

    var callback: Callback? = null

    interface Callback {
        fun onLanguageClick(language: TranslateManager.Language)
    }

    class LanguageAdapter(private val callback: (TranslateManager.Language) -> Unit) : ListAdapter<TranslateManager.Language, LanguageHolder>(TranslateManager.Language.DIFF_CALLBACK) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            LanguageHolder(ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: LanguageHolder, position: Int) {
            getItem(position)?.let { holder.bind(it, callback) }
        }
    }

    class LanguageHolder(private val itemBinding: ItemLanguageBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(language: TranslateManager.Language, callback: (TranslateManager.Language) -> Unit) {
            itemBinding.apply {
                if (language.code == root.context.defaultSharedPreferences.getString(Constants.Account.PREF_TRANSLATE_TARGET_LANG, getLanguageOrDefault())) {
                    checkIv.isVisible = true
                } else {
                    checkIv.isInvisible = true
                }
                name.text = language.nameInCurrentLanguage
                desc.text = language.nameInSelfLanguage
            }
            itemView.setOnClickListener { callback(language) }
        }
    }
}
