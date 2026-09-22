package one.mixin.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.UserAvatarImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.dpToPx
import one.mixin.android.ui.contacts.ContactPageHeader
import one.mixin.android.ui.contacts.ContactSectionHeader
import one.mixin.android.ui.contacts.ContactsSearchField
import one.mixin.android.ui.contacts.contactInitial
import one.mixin.android.ui.home.bot.Bot
import one.mixin.android.ui.home.bot.INTERNAL_LINK_DESKTOP_ID
import one.mixin.android.ui.home.bot.INTERNAL_REFERRAL_ID
import one.mixin.android.ui.home.bot.InternalBots
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.BotInterface
import one.mixin.android.vo.ExploreApp
import one.mixin.android.vo.User
import one.mixin.android.widget.NameTextView
import one.mixin.android.widget.lottie.RLottieDrawable
import one.mixin.android.widget.lottie.RLottieImageView

@Composable
internal fun MorePage(
    user: User?,
    contacts: List<User>,
    favorites: List<ExploreApp>,
    botCount: Int,
    isDesktopLogin: Boolean,
    clickedBotIds: Set<String>,
    onProfile: () -> Unit,
    onQr: () -> Unit,
    onContacts: () -> Unit,
    onContact: (User) -> Unit,
    onBots: () -> Unit,
    onBot: (BotInterface) -> Unit,
) {
    MixinAppTheme {
        val profileNameColor = MixinAppTheme.colors.textMinor.toArgb()
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MixinAppTheme.colors.background),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
        ) {
            user?.let {
                item {
                    Row(
                        Modifier.moreCard().clickable(onClick = onProfile).padding(start = 16.dp, end = 8.dp).heightIn(min = 68.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        UserAvatarImage(it, 42.dp)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            AndroidView(factory = { context -> NameTextView(context) }, update = { view -> view.setName(it); view.setTextColor(profileNameColor) })
                            Spacer(Modifier.height(4.dp))
                            Text(stringResource(R.string.contact_mixin_id, it.identityNumber), color = MixinAppTheme.colors.textAssist, fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 0.sp)
                        }
                        Box(Modifier.width(62.dp).height(48.dp)) {
                            Icon(painterResource(R.drawable.ic_more_arrow), null, tint = Color.Unspecified, modifier = Modifier.size(30.dp).align(Alignment.CenterEnd))
                            IconButton(onClick = onQr, modifier = Modifier.size(48.dp).align(Alignment.CenterStart)) {
                                Icon(painterResource(R.drawable.ic_more_qr), stringResource(R.string.My_QR_Code), tint = Color.Unspecified, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            items(InternalBots.chunked(2)) { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { bot ->
                        MoreActionCard(bot, isDesktopLogin, bot.id !in clickedBotIds && bot.id in ExploreFragment.SHOW_DOT_BOT_IDS, Modifier.weight(1f).fillMaxHeight()) { onBot(bot) }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            if (contacts.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Column(Modifier.moreCard()) {
                        MoreSectionHeader(stringResource(R.string.contacts_title), contacts.size, onContacts)
                        contacts.take(3).forEachIndexed { index, contact ->
                            Row(Modifier.fillMaxWidth().clickable { onContact(contact) }.padding(horizontal = 16.dp).height(50.dp), verticalAlignment = Alignment.CenterVertically) {
                                UserAvatarImage(contact, 42.dp)
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    AndroidView(factory = { NameTextView(it).apply { textView.textSize = 16f } }, update = { it.setName(contact) })
                                    Spacer(Modifier.height(4.dp))
                                    Text(contact.identityNumber, color = MixinAppTheme.colors.textAssist, fontSize = 14.sp, lineHeight = 17.sp, letterSpacing = 0.sp)
                                }
                            }
                            if (index < minOf(contacts.size, 3) - 1) Spacer(Modifier.height(20.dp))
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
            if (botCount > 0) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.moreCard()) {
                        MoreSectionHeader(stringResource(R.string.bots_title), botCount, onBots)
                        favorites.forEach { app -> BotRow(app, true, 42.dp) { onBot(app) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.moreCard() = fillMaxWidth().clip(RoundedCornerShape(8.dp))
    .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor, borderWidth = 1.dp)

@Composable
private fun MoreActionCard(bot: Bot, loggedIn: Boolean, showDot: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val desktop = bot.id == INTERNAL_LINK_DESKTOP_ID
    val icon = if (desktop && loggedIn) R.drawable.ic_more_desktop_logged else bot.icon
    Row(modifier.moreCard().clickable(onClick = onClick).heightIn(min = 77.dp).padding(horizontal = 10.dp, vertical = 12.dp)) {
        if (bot.id == INTERNAL_REFERRAL_ID) {
            AndroidView(
                factory = { context ->
                    RLottieImageView(context).apply {
                        val size = context.dpToPx(24f)
                        setAnimation(
                            RLottieDrawable(R.raw.referral, "referral", size, size).apply {
                                setAutoRepeat(1)
                                setAutoRepeatCount(Int.MAX_VALUE)
                            },
                        )
                        playAnimation()
                    }
                },
                modifier = Modifier.size(24.dp),
                onRelease = { view ->
                    val drawable = view.animatedDrawable
                    view.clearAnimationDrawable()
                    drawable?.recycle(true)
                },
            )
        } else {
            Icon(painterResource(icon), null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(bot.name), color = MixinAppTheme.colors.textPrimary, fontSize = 14.sp, lineHeight = 17.sp, letterSpacing = 0.sp, modifier = Modifier.weight(1f))
                if (showDot) Box(Modifier.padding(start = 4.dp).size(5.dp).background(MixinAppTheme.colors.accent, CircleShape))
            }
            Spacer(Modifier.height(4.dp))
            Text(stringResource(if (desktop && loggedIn) R.string.Logined else bot.description), color = MixinAppTheme.colors.textAssist, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun MoreSectionHeader(title: String, count: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).height(56.dp).padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = MixinAppTheme.colors.textPrimary, fontSize = 14.sp, lineHeight = 17.sp, letterSpacing = 0.sp, modifier = Modifier.weight(1f))
        Text(count.toString(), color = MixinAppTheme.colors.textAssist, fontSize = 14.sp, lineHeight = 17.sp, letterSpacing = 0.sp)
        MoreArrow()
    }
}

@Composable
private fun MoreArrow() {
    Icon(painterResource(R.drawable.ic_more_arrow), null, tint = Color.Unspecified, modifier = Modifier.size(30.dp))
}

@Composable
private fun BotRow(app: ExploreApp, external: Boolean = false, avatarSize: Dp = 50.dp, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).height(70.dp).padding(start = if (external) 16.dp else 20.dp, end = if (external) 8.dp else 20.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        CoilImage(app.iconUrl, R.drawable.ic_avatar_place_holder, Modifier.size(avatarSize).clip(CircleShape))
        Spacer(Modifier.width(if (external) 14.dp else 16.dp))
        Column(Modifier.weight(1f)) {
            AndroidView(factory = { NameTextView(it).apply { textView.textSize = 16f } }, update = { it.setName(app) })
            Spacer(Modifier.height(4.dp))
            Text(app.appNumber, color = MixinAppTheme.colors.textAssist, fontSize = 14.sp, lineHeight = 17.sp, letterSpacing = 0.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (external) Icon(painterResource(R.drawable.ic_more_link), null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
    }
}

@Composable
internal fun BotsPage(favorites: List<ExploreApp>, apps: List<ExploreApp>, onBack: () -> Unit, onEdit: () -> Unit, onFavoriteClick: (ExploreApp) -> Unit, onBotClick: (ExploreApp) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val matches: (ExploreApp) -> Boolean = { it.name.contains(query.trim(), true) || it.appNumber.contains(query.trim(), true) }
    val favoriteIds = favorites.map { it.appId }.toSet()
    val groups = apps.filter { it.appId !in favoriteIds && matches(it) }.groupBy { contactInitial(it.name) }.toSortedMap(compareBy { if (it == "#") "[" else it })
    MixinAppTheme {
        Column(Modifier.fillMaxSize().background(MixinAppTheme.colors.background)) {
            ContactPageHeader(stringResource(R.string.bots_title), onBack)
            ContactsSearchField(query, { query = it }, stringResource(R.string.setting_auth_search_hint))
            LazyColumn(Modifier.weight(1f)) {
                item { ContactSectionHeader(stringResource(R.string.Favorite)) }
                items(favorites.filter(matches), key = { "favorite:${it.appId}" }) { app -> BotRow(app) { onFavoriteClick(app) } }
                item {
                    Row(Modifier.fillMaxWidth().clickable(onClick = onEdit).height(70.dp).padding(start = 20.dp, end = 20.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_favorite_edit), null, tint = Color.Unspecified, modifier = Modifier.size(50.dp))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.My_favorite_bots), color = MixinAppTheme.colors.textPrimary, fontSize = 16.sp, lineHeight = 19.sp, letterSpacing = 0.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(stringResource(R.string.add_or_remove_favorite_bots), color = MixinAppTheme.colors.textAssist, fontSize = 14.sp, lineHeight = 17.sp, letterSpacing = 0.sp)
                        }
                    }
                }
                groups.forEach { (initial, group) ->
                    item(key = "header:$initial") { ContactSectionHeader(initial) }
                    items(group, key = { it.appId }) { app -> BotRow(app) { onBotClick(app) } }
                }
            }
        }
    }
}
