package one.mixin.android.ui.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.ui.common.BaseViewModelFragment

class AudioFragment : BaseViewModelFragment<SharedMediaViewModel>() {
    companion object {
        const val TAG = "AudioFragment"

        fun newInstance() = AudioFragment()
    }

    override fun getModelClass() = SharedMediaViewModel::class.java

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.layout_recycler_view, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view as RecyclerView
        view.layoutManager = LinearLayoutManager(requireContext())
    }
}
