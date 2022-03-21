package one.mixin.android.widget.picker

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import one.mixin.android.R
import one.mixin.android.databinding.ViewTimeIntervalPickerBinding

class TimeIntervalPicker : LinearLayout {

    private var binding: ViewTimeIntervalPickerBinding =
        ViewTimeIntervalPickerBinding.inflate(LayoutInflater.from(context), this)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setBackgroundResource(R.drawable.bg_round_gray_14dp)
        binding.apply {
            unitPicker.data = unitList
            unitPicker.setOnItemSelectedListener { _, _, position ->
                numberPicker.data = numberList[position]
            }
        }
    }

    fun getTimeInterval(): Long {
        return when (binding.unitPicker.currentItemPosition) {
            1 -> 60L
            2 -> 3600L
            3 -> 86400L
            4 -> 604800L
            else -> 1L
        } * numberList[binding.unitPicker.currentItemPosition][binding.numberPicker.currentItemPosition].toInt()
    }

    fun initTimeInterval(timeInterval: Long?) {
        if (timeInterval == null || timeInterval <= 0) {
            binding.apply {
                numberPicker.data = numberList[0]
                numberPicker.setSelectedItemPosition(0, false)
                unitPicker.setSelectedItemPosition(0, false)
            }
        } else {
            val (unitIndex, numberIndex) = toTimeIntervalIndex(timeInterval)
            binding.apply {
                numberPicker.data = numberList[unitIndex]
                numberPicker.setSelectedItemPosition(numberIndex, false)
                unitPicker.setSelectedItemPosition(unitIndex, false)
            }
        }
    }

    private val unitList by lazy { timeIntervalUnits.toList() }
}
