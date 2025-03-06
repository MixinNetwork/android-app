package one.mixin.android.ui.home.web3.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.ViewMarketTitleBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp

class MarketTitleView : RelativeLayout {

    private val _binding: ViewMarketTitleBinding
    private var isSevenDays: Boolean = true
    private var currentSort: MarketSort? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        _binding = ViewMarketTitleBinding.inflate(LayoutInflater.from(context), this)
        setupListeners()
        loadSortPreference()
        updatePercentageText()
    }

    fun updatePadding(horizontalPadding: Int) {
        _binding.priceOrder.updateLayoutParams<MarginLayoutParams> {
            marginEnd = horizontalPadding * 2 + 60.dp
        }
        _binding.percentageOrder.updateLayoutParams<MarginLayoutParams> {
            marginEnd = horizontalPadding
        }
        _binding.rankOrder.updateLayoutParams<MarginLayoutParams> {
            marginStart = horizontalPadding
        }
    }

    private fun setupListeners() {
        _binding.rankOrder.setOnClickListener {
            onTitleClicked(MarketSort.RANK_ASCENDING, MarketSort.RANK_DESCENDING)
        }
        _binding.priceOrder.setOnClickListener {
            onTitleClicked(MarketSort.PRICE_ASCENDING, MarketSort.PRICE_DESCENDING)
        }
        _binding.percentageOrder.setOnClickListener {
            if (isSevenDays) {
                onTitleClicked(MarketSort.SEVEN_DAYS_PERCENTAGE_ASCENDING, MarketSort.SEVEN_DAYS_PERCENTAGE_DESCENDING)
            } else {
                onTitleClicked(MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_ASCENDING, MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_DESCENDING)
            }
        }
    }

    private fun onTitleClicked(ascending: MarketSort, descending: MarketSort) {
        currentSort = if (currentSort == ascending) {
            updateSortOrder(descending)
            descending
        } else {
            updateSortOrder(ascending)
            ascending
        }
        saveSortPreference()
        currentSort?.let { callback(it) }
    }

    private fun updateSortOrder(sort: MarketSort) {
        resetAllIcons()
        when (sort) {
            MarketSort.RANK_ASCENDING, MarketSort.RANK_DESCENDING -> {
                _binding.rankIcon.isVisible = true
                _binding.rankIcon.rotation = if (sort == MarketSort.RANK_ASCENDING) 0f else 180f
            }

            MarketSort.PRICE_ASCENDING, MarketSort.PRICE_DESCENDING -> {
                _binding.priceIcon.isVisible = true
                _binding.priceIcon.rotation = if (sort == MarketSort.PRICE_ASCENDING) 0f else 180f
            }

            MarketSort.SEVEN_DAYS_PERCENTAGE_ASCENDING, MarketSort.SEVEN_DAYS_PERCENTAGE_DESCENDING -> {
                _binding.percentageIcon.isVisible = true
                _binding.percentageIcon.rotation = if (sort == MarketSort.SEVEN_DAYS_PERCENTAGE_ASCENDING) 0f else 180f
            }

            MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_ASCENDING, MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_DESCENDING -> {
                _binding.percentageIcon.isVisible = true
                _binding.percentageIcon.rotation = if (sort == MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_ASCENDING) 0f else 180f
            }
        }
    }

    private fun resetAllIcons() {
        _binding.rankIcon.isVisible = false
        _binding.priceIcon.isVisible = false
        _binding.percentageIcon.isVisible = false
    }

    private fun saveSortPreference() {
        currentSort?.let {
            sharedPreferences.edit().putInt(Constants.Account.PREF_MARKET_ORDER, it.value).apply()
        }
    }

    private fun loadSortPreference() {
        val sortValue = sharedPreferences.getInt(Constants.Account.PREF_MARKET_ORDER, 0)
        val sort = MarketSort.fromValue(sortValue)
        updateSortOrder(sort)
        currentSort = sort
    }

    fun setOnSortChangedListener(callback: (MarketSort) -> Unit) {
        this.callback = callback
    }

    fun setText(@StringRes str: Int) {
        _binding.rankTitle.setText(str)
    }

    private fun updatePercentageText() {
        _binding.percentage.text = if (isSevenDays) {
            context.getString(R.string.change_percent_period_day, 7)
        } else {
            context.getString(R.string.change_percent_period_hour, 24)
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
            callback(currentSort!!)
        }
    }

    private val sharedPreferences by lazy { context.defaultSharedPreferences }
    private lateinit var callback: (MarketSort) -> Unit
}