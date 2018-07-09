package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_about.*
import kotlinx.android.synthetic.main.view_title.view.*
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size
import one.mixin.android.R
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.common.BaseFragment

class AboutFragment : BaseFragment() {
    companion object {
        const val TAG = "AboutFragment"

        fun newInstance() = AboutFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_about, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val versionName = context!!.packageManager.getPackageInfo(context!!.packageName, 0).versionName
        title_view.setSubTitle(getString(R.string.app_name), getString(R.string.about_version, versionName))
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        twitter.setOnClickListener { context?.openUrl("https://twitter.com/MixinMessenger") }
        facebook.setOnClickListener { context?.openUrl("https://fb.com/MixinMessenger") }
        terms.setOnClickListener { context?.openUrl(getString(R.string.landing_terms_url)) }
        privacy.setOnClickListener { context?.openUrl(getString(R.string.landing_privacy_policy_url)) }
        konfetti_view.post {
            konfetti_view.build()
                .addColors(resources.getColor(R.color.lt_yellow, null), resources.getColor(R.color.lt_orange, null),
                    resources.getColor(R.color.lt_purple, null), resources.getColor(R.color.lt_pink, null))
                .setDirection(0.0, 359.0)
                .setSpeed(1f, 5f)
                .setFadeOutEnabled(true)
                .setTimeToLive(2000L)
                .addShapes(Shape.RECT, Shape.CIRCLE)
                .addSizes(Size(12))
                .setPosition(-50f, konfetti_view.width + 50f, -50f, -50f)
                .stream(300, 5000L)
        }
        konfetti_view.postDelayed({
            konfetti_view.build()
                .addColors(resources.getColor(R.color.lt_yellow, null), resources.getColor(R.color.lt_orange, null),
                    resources.getColor(R.color.lt_pink, null), resources.getColor(R.color.dk_cyan, null),
                    resources.getColor(R.color.dk_green, null))
                .setDirection(0.0, 359.0)
                .setSpeed(1f, 8f)
                .setFadeOutEnabled(true)
                .setTimeToLive(4000)
                .addShapes(Shape.RECT, Shape.CIRCLE)
                .addSizes(Size(12), Size(16, 6f))
                .setPosition(konfetti_view.x + konfetti_view.width / 2, konfetti_view.y + konfetti_view.height / 3)
                .burst(100)
        }, 1000)
    }
}
