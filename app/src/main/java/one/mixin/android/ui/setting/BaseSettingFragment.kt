package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import one.mixin.android.databinding.ViewTitleBinding
import one.mixin.android.ui.common.BaseFragment

abstract class BaseSettingFragment<T : ViewBinding> : BaseFragment() {

    private var _binding: T? = null
    protected val binding get() = requireNotNull(_binding)
    protected var _titleBinding: ViewTitleBinding? = null
    protected val titleBinding get() = requireNotNull(_titleBinding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = bind(inflater, container)
        return binding.root
    }

    abstract fun bind(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): T

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _titleBinding = null
    }
}
