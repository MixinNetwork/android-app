package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.BIOMETRIC_INTERVAL
import one.mixin.android.Constants.BIOMETRIC_INTERVAL_DEFAULT
import one.mixin.android.R
import one.mixin.android.databinding.FragmentBiometricTimeBinding
import one.mixin.android.databinding.ItemBiometricTimeBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putLong
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.wallet.PinBiometricsBottomSheetDialogFragment
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class BiometricTimeFragment : BaseFragment(R.layout.fragment_biometric_time) {
    companion object {
        const val TAG = "BiometricTimeFragment"

        val VALUES = floatArrayOf(.25f, .5f, 1f, 2f, 6f, 12f, 24f)

        const val X_HOUR = 1000 * 60 * 60
        const val DEFAULT_SELECTED_POS = 3

        fun newInstance() = BiometricTimeFragment()
    }

    private val values: ArrayList<String> by lazy {
        val strings = arrayListOf<String>()
        VALUES.forEach { v ->
            if (v < 1) {
                strings.add(getString(R.string.wallet_pin_pay_interval_minutes, (v * 60).toInt()))
            } else {
                strings.add(getString(R.string.wallet_pin_pay_interval_hours, v.toInt()))
            }
        }
        strings
    }
    private val adapter by lazy { TimeAdapter(values) }

    private val binding by viewBinding(FragmentBiometricTimeBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            title.leftIb.setOnClickListener { activity?.onBackPressed() }
            lv.adapter = adapter
            val footer = layoutInflater.inflate(R.layout.view_biometric_footer, lv, false)
            lv.addFooterView(footer)
            lv.setOnItemClickListener { _, _, i, _ ->
                if (i >= VALUES.size || adapter.selectedPos == i) return@setOnItemClickListener

                val bottomSheet =
                    PinBiometricsBottomSheetDialogFragment.newInstance(false)
                bottomSheet.callback = object : BiometricBottomSheetDialogFragment.Callback() {
                    override fun onSuccess() {
                        val intervalMillis = (VALUES[i] * X_HOUR).toLong()
                        defaultSharedPreferences.putLong(BIOMETRIC_INTERVAL, intervalMillis)
                        adapter.selectedPos = i

                        val pinSettingFragment = parentFragmentManager.findFragmentByTag(PinSettingFragment.TAG)
                        (pinSettingFragment as? PinSettingFragment)?.setTimeDesc()
                        activity?.onBackPressed()
                    }
                }
                bottomSheet.showNow(parentFragmentManager, PinBiometricsBottomSheetDialogFragment.TAG)
            }
        }
        setSelectedPos()
    }

    private fun setSelectedPos() {
        val interval = defaultSharedPreferences.getLong(BIOMETRIC_INTERVAL, BIOMETRIC_INTERVAL_DEFAULT)
        val intervalHour = interval.toFloat() / X_HOUR
        VALUES.forEachIndexed { index, v ->
            if (v == intervalHour) {
                adapter.selectedPos = index
                return
            }
        }
        if (adapter.selectedPos == -1) {
            adapter.selectedPos = DEFAULT_SELECTED_POS
        }
    }

    class TimeAdapter(private val values: ArrayList<String>) : BaseAdapter() {
        var selectedPos = -1
            set(value) {
                if (value == field) return

                field = value
                notifyDataSetChanged()
            }

        override fun getView(pos: Int, contentView: View?, parent: ViewGroup): View {
            var view = contentView
            var vh = TimeHolder()
            if (view == null) {
                val itemBinding = ItemBiometricTimeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                view = itemBinding.root
                vh.timeTv = itemBinding.timeTv
                vh.timeIv = itemBinding.timeIv
                view.tag = vh
            } else {
                vh = view.tag as TimeHolder
            }
            vh.timeTv.text = values[pos]
            if (selectedPos == pos) {
                vh.timeIv.visibility = VISIBLE
            } else {
                vh.timeIv.visibility = GONE
            }
            return view
        }

        override fun getItem(pos: Int) = values[pos]

        override fun getItemId(pos: Int): Long = 0L

        override fun getCount(): Int = values.size
    }

    class TimeHolder {
        lateinit var timeTv: TextView
        lateinit var timeIv: View
    }
}
