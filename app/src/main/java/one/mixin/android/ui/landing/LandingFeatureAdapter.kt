package one.mixin.android.ui.landing

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemLandingFeatureBinding

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

        fun bindItem(item: LandingFeatureItem, isFirstPage: Boolean) {
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
