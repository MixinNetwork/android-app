package one.mixin.android.ui.conversation

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import one.mixin.android.R
import one.mixin.android.launchFragmentInHiltContainer
import one.mixin.android.ui.TestRegistry
import one.mixin.android.ui.forward.ForwardActivity.Companion.ARGS_RESULT
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.ShareCategory
import one.mixin.android.webrtc.SelectItem
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ConversationFragmentTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
    }

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
            assertTrue(this.selectItem == expect)

            requireActivity().finish()
        }
    }
}
