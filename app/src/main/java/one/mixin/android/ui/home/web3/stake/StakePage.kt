package one.mixin.android.ui.home.web3.stake

import PageScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import one.mixin.android.R

@Composable
fun StakePage(
    pop: () -> Unit,
) {
    PageScaffold(
        title = stringResource(id = R.string.Stake),
        verticalScrollable = true,
        pop = pop,
    ) {
    }
}