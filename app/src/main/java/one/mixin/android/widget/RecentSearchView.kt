package one.mixin.android.widget

import android.content.Context
import android.view.LayoutInflater
import android.widget.RelativeLayout
import one.mixin.android.R
import one.mixin.android.databinding.ViewRecentSearchBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImage
import one.mixin.android.vo.RecentSearch
import one.mixin.android.vo.RecentSearchType

class RecentSearchView(context: Context) : RelativeLayout(context) {
    private val binding = ViewRecentSearchBinding.inflate(LayoutInflater.from(context), this)

    init {
        setBackgroundResource(R.drawable.bg_recent_search)
        val p = 6.dp
        setPadding(p, p, p, p)
    }

    fun loadData(recentSearch: RecentSearch){
        binding.apply {
            if (recentSearch.type == RecentSearchType.LINK) {
                icon.setImageResource(R.drawable.ic_link_place_holder)
            } else {
                icon.loadImage(recentSearch.iconUrl, R.drawable.ic_avatar_place_holder)
            }
            titleTv.text = recentSearch.title
            subtitleTv.text = recentSearch.subTitle
        }
    }
}
