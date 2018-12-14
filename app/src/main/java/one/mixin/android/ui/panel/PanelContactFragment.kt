package one.mixin.android.ui.panel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import one.mixin.android.R

class PanelContactFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_panel_contact, container, false)

    companion object {
        const val TAG = "PanelContactFragment"

        fun newInstance() = PanelContactFragment()
    }
}