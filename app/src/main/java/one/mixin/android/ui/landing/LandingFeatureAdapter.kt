package one.mixin.android.ui.landing

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemLandingFeatureBinding
import one.mixin.android.extension.dp

class LandingFeatureAdapter(
    private val items: List<LandingFeatureItem>,
) : RecyclerView.Adapter<LandingFeatureAdapter.LandingFeatureViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LandingFeatureViewHolder {
        val binding: ItemLandingFeatureBinding = ItemLandingFeatureBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return LandingFeatureViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LandingFeatureViewHolder, position: Int) {
        holder.bindItem(items[position], isFirstPage = position == 0)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class LandingFeatureViewHolder(
        private val binding: ItemLandingFeatureBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
                val height = (bottom - top - 156.dp).coerceAtLeast(0)
                if (binding.featureFl.layoutParams.height != height) {
                    binding.featureFl.updateLayoutParams<ViewGroup.MarginLayoutParams> { this.height = height }
                }
            }
        }

        fun bindItem(item: LandingFeatureItem, isFirstPage: Boolean) {
            binding.featureFl.setPadding(0, if (isFirstPage) 40.dp else 70.dp, 0, 0)
            binding.featureImage.setImageResource(item.imageResId)
            val layoutParams: FrameLayout.LayoutParams = binding.featureImage.layoutParams as FrameLayout.LayoutParams
            layoutParams.gravity = if (isFirstPage) {
                Gravity.CENTER
            } else {
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            binding.featureImage.layoutParams = layoutParams
            binding.featureTitle.text = item.title
            binding.featureDescription.text = item.description
        }
    }
}
