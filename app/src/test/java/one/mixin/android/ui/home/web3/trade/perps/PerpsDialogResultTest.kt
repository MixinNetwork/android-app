package one.mixin.android.ui.home.web3.trade.perps

import android.app.Application
import android.os.Bundle
import android.os.Parcel
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.api.response.perps.toPosition
import one.mixin.android.ui.common.VerifyBottomSheetDialogFragment
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import kotlin.test.assertEquals
import kotlin.test.fail

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [29])
class PerpsDialogResultTest {
    @Test
    fun restoredMenuReturnsEachActionToItsPositionOnce() {
        for (action in listOf(
            PerpsAdjustBottomSheetDialogFragment.ACTION_ADD_MARGIN,
            PerpsAdjustBottomSheetDialogFragment.ACTION_REDUCE_MARGIN,
            PerpsAdjustBottomSheetDialogFragment.ACTION_ADD_POSITION,
        )) {
            assertRestoredResult(
                original = PerpsAdjustBottomSheetDialogFragment.newInstance("position"),
                requestKey = PerpsAdjustBottomSheetDialogFragment.resultKey("position"),
                send = { fragment ->
                    ReflectionHelpers.callInstanceMethod<Unit>(fragment, "selectAction", ClassParameter.from(String::class.java, action))
                },
            ) { result ->
                assertEquals(action, result.getString(PerpsAdjustBottomSheetDialogFragment.RESULT_ACTION))
            }
        }
    }

    @Test
    fun restoredReductionReturnsTheApprovedAmountOnce() {
        val position = PerpsPositionItem("position", "market", "long", "1", "100", 10, margin = "10")
        assertRestoredResult(
            original = PerpsCloseBottomSheetDialogFragment.newInstance(position.toPosition(), reduceMarginAmount = "3.125"),
            requestKey = PerpsCloseBottomSheetDialogFragment.RESULT_MARGIN_CONFIRMED,
            send = { fragment ->
                val verification = restoreArguments(VerifyBottomSheetDialogFragment.newInstance()) as VerifyBottomSheetDialogFragment
                ReflectionHelpers.callInstanceMethod<Unit>(fragment, "bindPinVerification", ClassParameter.from(VerifyBottomSheetDialogFragment::class.java, verification))
                ReflectionHelpers.getField<(String) -> Unit>(verification, "onPinSuccess").invoke("")
            },
        ) { result ->
            assertEquals("position", result.getString(PerpsCloseBottomSheetDialogFragment.RESULT_POSITION_ID))
            assertEquals("3.125", result.getString(PerpsCloseBottomSheetDialogFragment.RESULT_AMOUNT))
        }
    }

    private fun assertRestoredResult(original: Fragment, requestKey: String, send: (Fragment) -> Unit, check: (Bundle) -> Unit) {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).create()
        val activity = controller.get()
        val manager = activity.supportFragmentManager
        val restored = restoreArguments(original)
        ReflectionHelpers.setField(restored, "mFragmentManager", manager)
        try {
            send(restored)
            manager.setFragmentResultListener(PerpsAdjustBottomSheetDialogFragment.resultKey("other-position"), activity) { _, _ ->
                fail("A result must not reach another position")
            }
            var deliveries = 0
            manager.setFragmentResultListener(requestKey, activity) { _, result ->
                deliveries++
                check(result)
            }
            assertEquals(0, deliveries)
            controller.start().resume()
            assertEquals(1, deliveries)
            manager.clearFragmentResultListener(requestKey)
            manager.setFragmentResultListener(requestKey, activity) { _, _ -> deliveries++ }
            assertEquals(1, deliveries)
        } finally {
            controller.pause().stop().destroy()
        }
    }

    private fun restoreArguments(original: Fragment): Fragment {
        val restored = FragmentFactory().instantiate(original.javaClass.classLoader!!, original.javaClass.name)
        val parcel = Parcel.obtain()
        try {
            original.requireArguments().writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            restored.arguments = Bundle.CREATOR.createFromParcel(parcel)
        } finally {
            parcel.recycle()
        }
        return restored
    }
}
