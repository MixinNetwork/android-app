package one.mixin.android.ui.conversation

import android.content.Intent
import one.mixin.android.R
import one.mixin.android.launchFragmentInHiltContainer
import one.mixin.android.ui.TestRegistry
import one.mixin.android.ui.forward.ForwardActivity.Companion.ARGS_RESULT
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.ShareCategory
import one.mixin.android.webrtc.SelectItem
import org.junit.Test
import java.util.UUID

class ConversationFragmentTest {

    @Test
    fun testGetForwardResult() {
        val cid = UUID.randomUUID().toString()
        val bundle = ConversationFragment.putBundle(cid, null, null, null)

        val expectedResult = Intent().apply {
            putParcelableArrayListExtra(ARGS_RESULT, arrayListOf(SelectItem(cid, null)))
        }
        val testRegistry = TestRegistry(expectedResult)
        launchFragmentInHiltContainer(ConversationFragment.newInstance(bundle, testRegistry), R.style.AppTheme_NoActionBar) {
            val list = arrayListOf(ForwardMessage<ForwardCategory>(ShareCategory.Text, "testGetForwardResult"))
            this.getForwardResult.launch(Pair(list, cid))

            val expect = expectedResult.getParcelableArrayListExtra<SelectItem>(ARGS_RESULT)?.get(0)
            assert(this.selectItem == expect)
        }
    }
}
