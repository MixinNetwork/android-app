package one.mixin.android.ui.sticker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.ActivityStickerShopBinding
import one.mixin.android.extension.dp
import one.mixin.android.ui.common.BaseActivity
import kotlin.math.abs

@AndroidEntryPoint
class StickerShopActivity : BaseActivity() {

    companion object {
        const val TAG = "StickerShopActivity"

        fun show(context: Activity) {
            Intent(context, StickerShopActivity::class.java).run {
                context.startActivity(this)
            }
            context.overridePendingTransition(R.anim.slide_in_bottom, R.anim.stay)
        }
    }

    private lateinit var binding: ActivityStickerShopBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStickerShopBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.stickerVp.adapter = StickerAdapter()
        binding.stickerVp.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        binding.stickerVp.clipToPadding = false
        binding.stickerVp.clipChildren = false
        binding.stickerVp.offscreenPageLimit = 3
        val compositePageTransformer = CompositePageTransformer().apply {
            addTransformer(MarginPageTransformer(6.dp))
            addTransformer { page, position ->
                page.scaleY = 0.9f + (1f - abs(position)) * 0.1f
            }
        }
        binding.stickerVp.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                // Todo
            }
        })
        binding.stickerVp.setPageTransformer(compositePageTransformer)
        binding.titleView.leftIb.setOnClickListener {
            finish()
        }
        binding.titleView.rightIb.setOnClickListener {
            // Todo
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom)
    }

    class StickerAdapter : RecyclerView.Adapter<StickerHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerHolder {
            return StickerHolder(
                ImageView(
                    parent.context
                ).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            )
        }

        override fun onBindViewHolder(holder: StickerHolder, position: Int) {
            if (position % 2 == 0) {
                holder.bind(R.drawable.bg_demo1)
            } else {
                holder.bind(R.drawable.bg_demo2)
            }
        }

        override fun getItemCount(): Int = 4
    }

    class StickerHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView) {
        fun bind(@DrawableRes imageRes: Int) {
            imageView.setImageResource(imageRes)
        }
    }
}
