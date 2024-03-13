package one.mixin.android.ui.home.web3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import one.mixin.android.databinding.FragmentPolygonBinding
import one.mixin.android.ui.common.BaseFragment

class PolygonFragment : BaseFragment() {
    companion object {
        const val TAG = "PolygonFragment"
    }

    private var _binding: FragmentPolygonBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPolygonBinding.inflate(inflater, container, false)
        return binding.root
    }

}