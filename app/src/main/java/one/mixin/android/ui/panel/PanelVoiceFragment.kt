package one.mixin.android.ui.panel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_panel_voice.*
import one.mixin.android.R

class PanelVoiceFragment : Fragment() {
    companion object {
        const val TAG = "PanelVoiceFragment"

        fun newInstance() = PanelVoiceFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_panel_voice, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        voice_iv.setOnClickListener {
            onVoiceCallback?.onVoiceClick()
        }
    }

    fun setHeight(height: Int) {
        view?.updateLayoutParams<ViewGroup.LayoutParams> {
            this.height = height
        }
    }

    var onVoiceCallback: OnVoiceCallback? = null

    interface OnVoiceCallback {
        fun onVoiceClick()
    }
}