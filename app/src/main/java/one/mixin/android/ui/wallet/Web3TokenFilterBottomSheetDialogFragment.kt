package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import one.mixin.android.R
import one.mixin.android.databinding.FragmentBottomSheetFilterBinding
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

class Web3TokenFilterBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "Web3TokenFilterBottomSheetDialogFragment"

        fun newInstance(
            currentType: Web3TokenFilterType,
            listener: Listener
        ) = Web3TokenFilterBottomSheetDialogFragment().apply {
            this.currentType = currentType
            this.listener = listener
        }
    }

    private val binding by viewBinding(FragmentBottomSheetFilterBinding::inflate)
    private lateinit var currentType: Web3TokenFilterType
    private var listener: Listener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // binding.titleView.setText(R.string.Filter)

        binding.allTv.setOnClickListener {
            if (currentType != Web3TokenFilterType.ALL) {
                listener?.onTypeClick(Web3TokenFilterType.ALL)
                dismiss()
            }
        }
        binding.sendTv.setOnClickListener {
            if (currentType != Web3TokenFilterType.SEND) {
                listener?.onTypeClick(Web3TokenFilterType.SEND)
                dismiss()
            }
        }
        binding.receiveTv.setOnClickListener {
            if (currentType != Web3TokenFilterType.RECEIVE) {
                listener?.onTypeClick(Web3TokenFilterType.RECEIVE)
                dismiss()
            }
        }

        when (currentType) {
            Web3TokenFilterType.ALL -> binding.allIv.visibility = View.VISIBLE
            Web3TokenFilterType.SEND -> binding.sendIv.visibility = View.VISIBLE
            Web3TokenFilterType.RECEIVE -> binding.receiveIv.visibility = View.VISIBLE
            Web3TokenFilterType.Contract -> binding.receiveIv.visibility = View.VISIBLE
        }
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    fun show(fragmentManager: FragmentManager) {
        if (isAdded) return
        super.show(fragmentManager, TAG)
    }

    interface Listener {
        fun onTypeClick(type: Web3TokenFilterType)
    }

    // override fun setupDialog(dialog: BottomSheet, style: Int) {
    //     super.setupDialog(dialog, style)
    //     contentView = binding.root
    // }
}
