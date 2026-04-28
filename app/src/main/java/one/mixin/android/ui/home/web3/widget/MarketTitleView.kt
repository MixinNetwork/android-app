package one.mixin.android.ui.home.web3.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.annotation.StringRes
import androidx.core.view.updateLayoutParams
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.ViewMarketTitleBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp

class MarketTitleView : RelativeLayout {
    companion object {
        private const val SORT_DEFAULT = -1
    }

    private val _binding: ViewMarketTitleBinding
    private var isSevenDays: Boolean = true
    private var currentSort: MarketSort = MarketSort.RANK_ASCENDING

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        _binding = ViewMarketTitleBinding.inflate(LayoutInflater.from(context), this)
        setupListeners()
        loadSortPreference()
        updatePercentageText()
    }

    fun updatePadding(horizontalPadding: Int) {
        _binding.percentageOrder.updateLayoutParams<MarginLayoutParams> {
            marginEnd = horizontalPadding
        }
        _binding.pricePercentageDivider.updateLayoutParams<MarginLayoutParams> {
            marginEnd = 10.dp
        }
        _binding.priceOrder.updateLayoutParams<MarginLayoutParams> {
            marginEnd = 10.dp
        }
        _binding.rankOrder.updateLayoutParams<MarginLayoutParams> {
            marginStart = horizontalPadding
        }
    }

    private fun setupListeners() {
        _binding.rankOrder.setOnClickListener {
            currentSort =
                when (currentSort) {
                    MarketSort.RANK_ASCENDING -> MarketSort.RANK_DESCENDING
                    else -> MarketSort.RANK_ASCENDING
                }
            updateSortOrder(currentSort)
            saveSortPreference()
            callback(currentSortOrDefault())
        }
        _binding.priceOrder.setOnClickListener {
            onTitleClicked(MarketSort.PRICE_DESCENDING, MarketSort.PRICE_ASCENDING)
        }
        _binding.percentageOrder.setOnClickListener {
            if (isSevenDays) {
                onTitleClicked(MarketSort.SEVEN_DAYS_PERCENTAGE_DESCENDING, MarketSort.SEVEN_DAYS_PERCENTAGE_ASCENDING)
            } else {
                onTitleClicked(MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_DESCENDING, MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_ASCENDING)
            }
        }
    }

    private fun onTitleClicked(descending: MarketSort, ascending: MarketSort) {
        currentSort =
            when (currentSort) {
                descending -> ascending
                ascending -> MarketSort.RANK_ASCENDING
                else -> descending
            }
        updateSortOrder(currentSort)
        saveSortPreference()
        callback(currentSortOrDefault())
    }

    private fun updateSortOrder(sort: MarketSort) {
        resetAllIcons()
        when (sort) {
            MarketSort.RANK_ASCENDING, MarketSort.RANK_DESCENDING -> {
                _binding.rankIcon.setImageResource(
                    if (sort == MarketSort.RANK_ASCENDING) R.drawable.ic_perps_sort_asc else R.drawable.ic_perps_sort_desc
                )
            }

            MarketSort.PRICE_ASCENDING, MarketSort.PRICE_DESCENDING -> {
                _binding.priceIcon.setImageResource(
                    if (sort == MarketSort.PRICE_ASCENDING) R.drawable.ic_perps_sort_asc else R.drawable.ic_perps_sort_desc
                )
            }

            MarketSort.SEVEN_DAYS_PERCENTAGE_ASCENDING, MarketSort.SEVEN_DAYS_PERCENTAGE_DESCENDING -> {
                _binding.percentageIcon.setImageResource(
                    if (sort == MarketSort.SEVEN_DAYS_PERCENTAGE_ASCENDING) R.drawable.ic_perps_sort_asc else R.drawable.ic_perps_sort_desc
                )
            }

            MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_ASCENDING, MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_DESCENDING -> {
                _binding.percentageIcon.setImageResource(
                    if (sort == MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_ASCENDING) R.drawable.ic_perps_sort_asc else R.drawable.ic_perps_sort_desc
                )
            }
        }
    }

    private fun resetAllIcons() {
        resetIcon(_binding.rankIcon)
        resetIcon(_binding.priceIcon)
        resetIcon(_binding.percentageIcon)
    }

    private fun resetIcon(icon: ImageView) {
        icon.setImageResource(R.drawable.ic_perps_sort_default)
    }

    private fun saveSortPreference() {
        val editor = sharedPreferences.edit()
        if (currentSort == MarketSort.RANK_ASCENDING) {
            editor.putInt(Constants.Account.PREF_MARKET_ORDER, SORT_DEFAULT)
        } else {
            editor.putInt(Constants.Account.PREF_MARKET_ORDER, currentSort.value)
        }
        editor.apply()
    }

    private fun loadSortPreference() {
        val sortValue = sharedPreferences.getInt(Constants.Account.PREF_MARKET_ORDER, SORT_DEFAULT)
        currentSort = MarketSort.fromValueOrNull(sortValue) ?: MarketSort.RANK_ASCENDING
        updateSortOrder(currentSort)
    }

    fun setOnSortChangedListener(callback: (MarketSort) -> Unit) {
        this.callback = callback
    }

    fun setText(@StringRes str: Int) {
        _binding.rankTitle.setText(str)
    }

    fun currentSortOrDefault(): MarketSort = currentSort

    private fun updatePercentageText() {
        _binding.percentage.text = if (isSevenDays) {
            context.getString(R.string.change_percent_period_day, 7)
        } else {
            context.getString(R.string.change_period_hour, 24)
        }
    }

    fun updateSortType(topPercentage: Int) {
        isSevenDays = topPercentage == 0
        updatePercentageText()
        val newSort = when {
            isSevenDays && (currentSort == MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_ASCENDING || currentSort == MarketSort.SEVEN_DAYS_PERCENTAGE_ASCENDING) -> MarketSort.SEVEN_DAYS_PERCENTAGE_ASCENDING
            isSevenDays && (currentSort == MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_DESCENDING || currentSort == MarketSort.SEVEN_DAYS_PERCENTAGE_DESCENDING) -> MarketSort.SEVEN_DAYS_PERCENTAGE_DESCENDING
            !isSevenDays && (currentSort == MarketSort.SEVEN_DAYS_PERCENTAGE_ASCENDING || currentSort == MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_ASCENDING) -> MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_ASCENDING
            !isSevenDays && (currentSort == MarketSort.SEVEN_DAYS_PERCENTAGE_DESCENDING || currentSort == MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_DESCENDING) -> MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_DESCENDING
            else -> currentSort
        }
        if (newSort != currentSort) {
            currentSort = newSort
            updateSortOrder(currentSort)
            saveSortPreference()
            callback(currentSortOrDefault())
        }
    }

    private val sharedPreferences by lazy { context.defaultSharedPreferences }
    private lateinit var callback: (MarketSort) -> Unit
}
