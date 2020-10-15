package one.mixin.android.ui.web

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_web_demo.view.*
import one.mixin.android.R
import one.mixin.android.ui.web.WebActivity.Companion.webViews

class WebFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        layoutInflater.inflate(R.layout.fragment_web_demo, container, false)

    private lateinit var container: ViewGroup
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view.container
        view.container.addView(webViews[requireArguments().getInt("index")], 0, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        view.close.setOnClickListener {
            parentFragmentManager.beginTransaction().remove(this).commit()
        }
    }

    override fun onDestroyView() {
        container.removeViewAt(0)
        super.onDestroyView()
    }
}