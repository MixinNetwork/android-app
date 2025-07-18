package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import one.mixin.android.R
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.dp
import one.mixin.android.extension.navTo
import one.mixin.android.ui.wallet.AllWeb3TransactionsFragment
import one.mixin.android.ui.wallet.Web3FilterParams
import one.mixin.android.ui.wallet.Web3TokenFilterType

class Web3PendingView @JvmOverloads constructor(
    context: Context,
    val attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    private val pendingText: TextView
    private var pendingCount = 0

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.view_web3_pending, this, true)
        pendingText = view.findViewById(R.id.pending_text)
        
        setBackgroundResource(R.drawable.bg_round_window_btn)
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            setPadding(10.dp, 10.dp, 10.dp, 10.dp)
        }
        isVisible = false
    }

    fun setPendingCount(count: Int) {
        pendingCount = count
        if (count <= 0) {
            isVisible = false
            return
        }
        
        isVisible = true
        pendingText.text = if (count == 1) {
            context.getString(R.string.Transaction_Pending_Confirmation)
        } else {
            context.getString(R.string.Transactions_Pending_Confirmation, count)
        }
    }

    fun getPendingCount(): Int {
        return pendingCount
    }

    private var lastPendingData: LiveData<Int>? = null

    fun observePendingCount(lifecycleOwner: LifecycleOwner, pendingCountLiveData: LiveData<Int>) {
        lastPendingData?.removeObservers(lifecycleOwner)
        lastPendingData = pendingCountLiveData
        lastPendingData?.observe(lifecycleOwner){ count ->
            setPendingCount(count)
        }
    }
}
