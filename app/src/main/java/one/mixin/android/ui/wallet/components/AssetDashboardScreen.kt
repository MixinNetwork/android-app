package one.mixin.android.ui.wallet.components

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.reactivex.android.schedulers.AndroidSchedulers
import one.mixin.android.Constants.Account.PREF_HAS_USED_ADD_WALLET
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.web3.vo.isImported
import one.mixin.android.db.web3.vo.isOwner
import one.mixin.android.db.web3.vo.isWatch
import one.mixin.android.event.WalletRefreshedEvent
import one.mixin.android.extension.openUrl
import one.mixin.android.session.Session
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.WalletCategory

const val PREF_NAME = "wallet_info_card"
const val KEY_HIDE_PRIVACY_WALLET_INFO = "hide_privacy_wallet_info"
const val KEY_HIDE_COMMON_WALLET_INFO = "hide_common_wallet_info"
const val KEY_HIDE_SAFE_WALLET_INFO = "hide_safe_wallet_info"
const val KEY_SAFE_CATEGORY_BADGE_SEEN = "safe_category_badge_seen"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AssetDashboardScreen(
    onWalletCardClick: (destination: WalletDestination) -> Unit,
    onAddWalletClick: () -> Unit,
    onUpgradePlan: () -> Unit,
) {
    val viewModel: AssetDistributionViewModel = hiltViewModel()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) }
    val hidePrivacyWalletInfo = remember { mutableStateOf(prefs.getBoolean(KEY_HIDE_PRIVACY_WALLET_INFO, false)) }
    val hideCommonWalletInfo = remember { mutableStateOf(prefs.getBoolean(KEY_HIDE_COMMON_WALLET_INFO, false)) }
    val hideSafeWalletInfo = remember { mutableStateOf(prefs.getBoolean(KEY_HIDE_SAFE_WALLET_INFO, false)) }
    val hasSeenSafeCategoryBadge = remember { mutableStateOf(prefs.getBoolean(KEY_SAFE_CATEGORY_BADGE_SEEN, false)) }
    val addWalletClicked = remember { mutableStateOf(prefs.getBoolean(PREF_HAS_USED_ADD_WALLET, false)) }
    val wallets by viewModel.wallets.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    var refreshTrigger by remember { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        val disposable = RxBus.listen(WalletRefreshedEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                refreshTrigger++
            }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            disposable.dispose()
        }
    }

    LaunchedEffect(refreshTrigger) {
        viewModel.loadWallets()
    }

    MixinAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.Wallets),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MixinAppTheme.colors.textPrimary,
                    modifier = Modifier
                        .height(56.dp)
                        .wrapContentHeight(Alignment.CenterVertically)
                )
                Spacer(modifier = Modifier.weight(1f))
                Box {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_black_24dp),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.icon,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                if (!addWalletClicked.value) {
                                    addWalletClicked.value = true
                                    prefs.edit { putBoolean(PREF_HAS_USED_ADD_WALLET, true) }
                                }
                                onAddWalletClick()
                            }
                    )
                    if (!addWalletClicked.value) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(8.dp)
                                .background(color = Color.Red, shape = CircleShape)
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                TotalAssetsCard()
                Spacer(modifier = Modifier.height(20.dp))

                val hasImported = wallets.any { it.isImported() }
                val hasWatch = wallets.any { it.isWatch() }
                val hasSafe = wallets.any { it.category == WalletCategory.MIXIN_SAFE.value }

                WalletCategoryFilter(
                    selectedCategory = selectedCategory,
                    hasImported = hasImported,
                    hasWatch = hasWatch,
                    hasSafe = hasSafe,
                    showSafeBadge = hasSafe && !hasSeenSafeCategoryBadge.value,
                    onCategorySelected = {
                        if (it == WalletCategory.MIXIN_SAFE.value) {
                            prefs.edit { putBoolean(KEY_SAFE_CATEGORY_BADGE_SEEN, true) }
                            hasSeenSafeCategoryBadge.value = true
                        }
                        selectedCategory = it
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Privacy wallet - always show if no filter or "all" selected
                if (selectedCategory == null) {
                    WalletCard(
                        destination = WalletDestination.Privacy,
                        onClick = { onWalletCardClick.invoke(WalletDestination.Privacy) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (selectedCategory == WalletCategory.MIXIN_SAFE.value && wallets.any { it.category == WalletCategory.MIXIN_SAFE.value }.not()) {
                    if (Session.getAccount()?.membership?.isMembership() == true) {
                        CreateSafeCard(
                            onCreateClick = {
                                context.openUrl(context.getString(R.string.safe_create_guideline_url))
                            },
                        )
                    } else {
                        UpgradeSafeCard(
                            onUpgradeClick = {
                                onUpgradePlan.invoke()
                            },
                            onLearnMoreClick = {
                                context.openUrl(context.getString(R.string.safe_learn_more_url))
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                wallets.forEach { wallet ->
                    val shouldShow = when (selectedCategory) {
                        null -> true // Show all
                        WalletCategory.MIXIN_SAFE.value -> wallet.category == WalletCategory.MIXIN_SAFE.value
                        WalletCategory.CLASSIC.value -> wallet.category == WalletCategory.CLASSIC.value
                        "import" -> wallet.isImported()
                        "watch" -> wallet.isWatch()
                        else -> true
                    }

                    if (!shouldShow) return@forEach

                    if (wallet.category == WalletCategory.MIXIN_SAFE.value) {
                        WalletCard(
                            name = wallet.name,
                            destination = WalletDestination.Safe(wallet.id, wallet.isOwner(), wallet.safeChainId,wallet.safeUrl),
                            onClick = { onWalletCardClick.invoke(WalletDestination.Safe(wallet.id, wallet.isOwner(), wallet.safeChainId,wallet.safeUrl)) }
                        )
                    } else if (wallet.isWatch()) {
                        WalletCard(
                            name = wallet.name,
                            destination = WalletDestination.Watch(wallet.id, wallet.category),
                            onClick = { onWalletCardClick.invoke(WalletDestination.Watch(wallet.id, wallet.category)) })
                    } else if (wallet.isImported()) {
                        WalletCard(
                            name = wallet.name,
                            hasLocalPrivateKey = wallet.hasLocalPrivateKey,
                            destination = WalletDestination.Import(wallet.id, wallet.category),
                            onClick = { onWalletCardClick.invoke(WalletDestination.Import(wallet.id, wallet.category)) },
                        )
                    } else {
                        WalletCard(
                            name = wallet.name,
                            destination = WalletDestination.Classic(wallet.id),
                            onClick = { onWalletCardClick.invoke(WalletDestination.Classic(wallet.id)) }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (!hidePrivacyWalletInfo.value || !hideCommonWalletInfo.value || !hideSafeWalletInfo.value) {
                    Spacer(modifier = Modifier.weight(1f))
                    WalletInfoCard(
                        hidePrivacyWalletInfo = hidePrivacyWalletInfo.value,
                        hideCommonWalletInfo = hideCommonWalletInfo.value,
                        hideSafeWalletInfo = hideSafeWalletInfo.value,
                        onPrivacyClose = {
                            hidePrivacyWalletInfo.value = true
                            prefs.edit { putBoolean(KEY_HIDE_PRIVACY_WALLET_INFO, true) }
                        },
                        onCommonClose = {
                            hideCommonWalletInfo.value = true
                            prefs.edit { putBoolean(KEY_HIDE_COMMON_WALLET_INFO, true) }
                        },
                        onSafeClose = {
                            hideSafeWalletInfo.value = true
                            prefs.edit { putBoolean(KEY_HIDE_SAFE_WALLET_INFO, true) }
                        }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WalletInfoCard(
    hidePrivacyWalletInfo: Boolean,
    hideCommonWalletInfo: Boolean,
    hideSafeWalletInfo: Boolean,
    onPrivacyClose: () -> Unit,
    onCommonClose: () -> Unit,
    onSafeClose: () -> Unit,
) {
    val context = LocalContext.current

    // Build list of visible pages
    val pages = buildList {
        if (!hidePrivacyWalletInfo) add("privacy")
        if (!hideCommonWalletInfo) add("common")
        if (!hideSafeWalletInfo) add("safe")
    }

    val pageCount = pages.size
    if (pageCount == 0) return

    val pagerState = rememberPagerState(initialPage = 0) { pageCount }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                when (pages[page]) {
                    "privacy" -> PrivacyWalletInfo(
                        onLearnMoreClick = {
                            context.openUrl(context.getString(R.string.url_privacy_wallet))
                        },
                        onClose = onPrivacyClose
                    )

                    "common" -> CommonWalletInfo(
                        onLearnMoreClick = {
                            context.openUrl(context.getString(R.string.url_classic_wallet))
                        },
                        onClose = onCommonClose
                    )

                    "safe" -> SafeWalletInfo(
                        onLearnMoreClick = {
                            context.openUrl(context.getString(R.string.safe_learn_more_url))
                        },
                        onClose = onSafeClose
                    )
                }
            }

            if (pageCount > 1) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    repeat(pageCount) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MixinAppTheme.colors.accent else MixinAppTheme.colors.borderColor
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyWalletInfo(
    onLearnMoreClick: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.Whats_Privacy_Wallet),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MixinAppTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )

            Image(
                painter = painterResource(id = R.drawable.ic_close_grey),
                contentDescription = stringResource(R.string.Close),
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onClose() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.Privacy_Wallet_Description),
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textMinor,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_safe),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onLearnMoreClick() }
        ) {
            Text(
                text = stringResource(R.string.Learn_More),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MixinAppTheme.colors.accent
            )
        }
    }
}

@Composable
fun SafeWalletInfo(
    onLearnMoreClick: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.Whats_Safe_Wallet),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MixinAppTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )

            Image(
                painter = painterResource(id = R.drawable.ic_close_grey),
                contentDescription = stringResource(R.string.Close),
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onClose() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.Safe_Wallet_Description),
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textMinor,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_privacy),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onLearnMoreClick() }
        ) {
            Text(
                text = stringResource(R.string.Learn_More),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MixinAppTheme.colors.accent
            )
        }
    }
}

@Composable
fun CommonWalletInfo(
    onLearnMoreClick: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.Whats_Common_Wallet),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MixinAppTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )

            Image(
                painter = painterResource(id = R.drawable.ic_close_grey),
                contentDescription = stringResource(R.string.Close),
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onClose() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.Common_Wallet_Description),
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textMinor,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_common),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onLearnMoreClick() }
        ) {
            Text(
                text = stringResource(R.string.Learn_More),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MixinAppTheme.colors.accent
            )
        }
    }
}

@Composable
fun UpgradeSafeCard(
    onUpgradeClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(16.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.Upgrade_Plan),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MixinAppTheme.colors.textPrimary,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.Upgrade_Safe_Description),
                    fontSize = 14.sp,
                    lineHeight = 17.5.sp,
                    color = MixinAppTheme.colors.textMinor,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_safe),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MixinAppTheme.colors.backgroundWindow,
                    shape = RoundedCornerShape(16.dp)
                ),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onUpgradeClick() }
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = stringResource(R.string.Upgrade),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MixinAppTheme.colors.accent,
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(bottomStart = 16.dp, topStart = 16.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))

            }
            Spacer(
                modifier = Modifier
                    .width(2.dp)
                    .height(24.dp)
                    .background(Color.Black.copy(alpha = 0.05f))
                    .align(Alignment.CenterVertically)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onLearnMoreClick() }
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.Learn_More),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MixinAppTheme.colors.textPrimary,
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                )

                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_top_right_small),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.backgroundDark,
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}

@Composable
fun CreateSafeCard(
    onCreateClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(16.dp)
    ) {


        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.Create_Safe),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MixinAppTheme.colors.textPrimary,
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.Create_Safe_Description),
                    fontSize = 14.sp,
                    lineHeight = 17.5.sp,
                    color = MixinAppTheme.colors.textMinor
                )
            }

            Image(
                painter = painterResource(id = R.drawable.ic_safe),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MixinAppTheme.colors.accent,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { onCreateClick() }
                .padding(6.dp),
        ) {
            Text(
                text = stringResource(R.string.Guideline),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(6.dp),
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_top_right_small),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(8.dp)
                    .align(Alignment.TopEnd)
            )
        }
    }
}

@Preview
@Composable
fun CardPreview() {
    Column {
        CreateSafeCard {}
        Spacer(modifier = Modifier.height(8.dp))
        UpgradeSafeCard({}, {})
    }
}