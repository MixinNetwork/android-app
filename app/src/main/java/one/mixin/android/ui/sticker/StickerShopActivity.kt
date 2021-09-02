package one.mixin.android.ui.sticker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
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
import one.mixin.android.databinding.ItemStickerProductBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadSticker
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
        binding.stickerRv.adapter = StickerProductAdapter(demoData)
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

    class StickerProductAdapter(val list: List<String>) :
        RecyclerView.Adapter<StickerProductHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerProductHolder {
            return StickerProductHolder(
                ItemStickerProductBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: StickerProductHolder, position: Int) {
            holder.bind(
                listOf(
                    list[position * 4],
                    list[position * 4 + 1],
                    list[position * 4 + 2],
                    list[position * 4 + 3]
                )
            )
        }

        override fun getItemCount(): Int = 4
    }


    class StickerProductHolder(private val itemStickerProductBinding: ItemStickerProductBinding) :
        RecyclerView.ViewHolder(itemStickerProductBinding.root) {
        fun bind(list: List<String>) {
            itemStickerProductBinding.chatSticker1.loadSticker(list[0], "")
            itemStickerProductBinding.chatSticker2.loadSticker(list[1], "")
            itemStickerProductBinding.chatSticker3.loadSticker(list[2], "")
            itemStickerProductBinding.chatSticker4.loadSticker(list[3], "")
        }
    }

    // Todo delete
    private val demoData = listOf(
        "https://moments.shou.tv/1535971684-a05110912d1a737dc2ae7c7dfbb9cddea3fd6a8ce7cf1478cb7f68dd9b6ef63f.webp",
        "https://moments.shou.tv/1535971689-c22858525c430dfbe8a645afbefb81cbc13fc1c1d7b4b56968c84a58fd26a2c0.webp",
        "https://moments.shou.tv/1535971682-54ae3983cc7a1300032ea9709bf8ef6cdd2aa245721a6603f99b6869014acb00.webp",
        "https://moments.shou.tv/1535971687-695330641110137cd32523b635217f5c9895353a0b56a45247e33d31a2490696.webp",
        "https://moments.shou.tv/1535971684-99da0298bd2466d5362e60af272f6cc6281040055d6b4afc4ae04a31f249dbe3.webp",
        "https://moments.shou.tv/1535971688-b785c8879225516cd5f70a4cf9d5e7ef7915ea7bab643d3644b2a96078a85ed7.webp",
        "https://moments.shou.tv/1535971686-5ba4ae0736f5f678cc21813ab94f7ac260c4b44284cee6bbfe916b203b3e488b.webp",
        "https://moments.shou.tv/1535971677-1636ff0f5abc1dd08f7bd1f239924d5b6dffde7f5c16d9d765f6d4968849e7d0.webp",
        "https://moments.shou.tv/1535971686-2ff7ad95744482aa23497722e2ea2a44380059c8a63d7b31d742462683194324.webp",
        "https://moments.shou.tv/1535971681-670e1a86bc1374fc30b83e6c5147f908c14a26e6442c34bf64e6cf399aca40f9.webp",
        "https://moments.shou.tv/1535971681-91d75e2b389570490e736c7d6d3dcdba3ee31d520bdfd60bc34e23983e900be9.webp",
        "https://moments.shou.tv/1535971683-b57cdeddff588345c7aba93ff637269d0c04d7b88065e02955a06111ab9779a7.webp",
        "https://moments.shou.tv/1535971684-a05110912d1a737dc2ae7c7dfbb9cddea3fd6a8ce7cf1478cb7f68dd9b6ef63f.webp",
        "https://moments.shou.tv/1535971689-c22858525c430dfbe8a645afbefb81cbc13fc1c1d7b4b56968c84a58fd26a2c0.webp",
        "https://moments.shou.tv/1535971682-54ae3983cc7a1300032ea9709bf8ef6cdd2aa245721a6603f99b6869014acb00.webp",
        "https://moments.shou.tv/1535971687-695330641110137cd32523b635217f5c9895353a0b56a45247e33d31a2490696.webp"
    )
}
