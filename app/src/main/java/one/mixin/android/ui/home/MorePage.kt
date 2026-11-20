package one.mixin.android.ui.home

import android.graphics.Matrix
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
import one.mixin.android.ui.contacts.ContactAlphabetIndex
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
    apps: List<ExploreApp>,
    isDesktopLogin: Boolean,
    clickedBotIds: Set<String>,
    onProfile: () -> Unit,
    onQr: () -> Unit,
    onContacts: () -> Unit,
    onContact: (User) -> Unit,
    onBots: () -> Unit,
    onBot: (BotInterface) -> Unit,
    onBotProfile: (ExploreApp) -> Unit,
) {
    val bots = remember(favorites, apps) { (favorites + apps).distinctBy { it.appId } }
    val favoriteIds = remember(favorites) { favorites.map { it.appId }.toSet() }
    val internalBotRows = remember { InternalBots.chunked(2) }
    MixinAppTheme {
        val profileNameColor = MixinAppTheme.colors.textMinor.toArgb()
        LazyColumn(
            modifier = Modifier.fillMaxSize().homeNavigationScroll().background(MixinAppTheme.colors.background),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 16.dp + homeNavigationInset()),
        ) {
            user?.let {
                item(key = "profile", contentType = "profile_card") {
                    Row(
                        Modifier.moreCard().clickable(onClick = onProfile).padding(start = 16.dp, end = 8.dp).heightIn(min = 68.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        UserAvatarImage(it, 42.dp)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            AndroidView(factory = { context -> NameTextView(context).apply { textView.textSize = 16f } }, update = { view -> view.setName(it); view.setTextColor(profileNameColor) })
                            Spacer(Modifier.height(4.dp))
                            Text(stringResource(R.string.contact_mixin_id, it.identityNumber), color = MixinAppTheme.colors.textAssist, fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 0.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onQr, modifier = Modifier.size(48.dp)) {
                                Icon(painterResource(R.drawable.ic_more_qr), stringResource(R.string.My_QR_Code), tint = Color.Unspecified, modifier = Modifier.size(24.dp))
                            }
                            Icon(painterResource(R.drawable.ic_more_arrow), null, tint = Color.Unspecified, modifier = Modifier.size(30.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            items(
                items = internalBotRows,
                key = { row -> row.joinToString(":") { it.id } },
                contentType = { "internal_bot_row" },
            ) { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { bot ->
                        MoreActionCard(bot, isDesktopLogin, bot.id !in clickedBotIds && bot.id in ExploreFragment.SHOW_DOT_BOT_IDS, Modifier.weight(1f).fillMaxHeight()) { onBot(bot) }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            item(key = "contacts_card", contentType = "contacts_card") {
                Column(Modifier.moreCard()) {
                    MoreSectionHeader(stringResource(R.string.contacts_title), contacts.size, onContacts)
                    contacts.take(3).forEach { contact ->
                        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp).clickable { onContact(contact) }.padding(horizontal = 16.dp, vertical = 8.dp).height(50.dp), verticalAlignment = Alignment.CenterVertically) {
                            UserAvatarImage(contact, 42.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                AndroidView(factory = { NameTextView(it).apply { textView.textSize = 16f } }, update = { it.setName(contact) })
                                Spacer(Modifier.height(4.dp))
                                Text(contact.identityNumber, color = MixinAppTheme.colors.textAssist, fontSize = 14.sp, lineHeight = 17.sp, letterSpacing = 0.sp)
                            }
                        }
                    }
                    if (contacts.size > 3) {
                        Text(
                            stringResource(R.string.view_all),
                            color = MixinAppTheme.colors.accent,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                                .clickable(onClick = onContacts)
                                .padding(top = 18.dp, bottom = 16.dp),
                        )
                    }
                }
            }
            item(key = "bots_card", contentType = "bots_card") {
                Spacer(Modifier.height(10.dp))
                Column(Modifier.moreCard()) {
                    MoreSectionHeader(stringResource(R.string.bots_title), bots.size, onBots)
                    bots.take(5).forEach { app ->
                        val isFavorite = app.appId in favoriteIds
                        BotRow(app, external = true, avatarSize = 42.dp, showLink = isFavorite) {
                            if (isFavorite) onBot(app) else onBotProfile(app)
                        }
                    }
                    if (bots.size > 5) {
                        Text(
                            stringResource(R.string.view_all),
                            color = MixinAppTheme.colors.accent,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                                .clickable(onClick = onBots)
                                .padding(top = 18.dp, bottom = 16.dp),
                        )
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
    Row(modifier.moreCard().clickable(onClick = onClick).heightIn(min = 77.dp).padding(horizontal = 16.dp, vertical = 12.dp)) {
        if (bot.id == INTERNAL_REFERRAL_ID) {
            AndroidView(
                factory = { context ->
                    RLottieImageView(context).apply {
                        val size = context.dpToPx(30f)
                        setAnimation(
                            RLottieDrawable(R.raw.referral, "referral", size, size).apply {
                                setAutoRepeat(1)
                                setAutoRepeatCount(Int.MAX_VALUE)
                            },
                        )
                        val offset = (context.dpToPx(24f) - size) / 2f
                        scaleType = ImageView.ScaleType.MATRIX
                        imageMatrix = Matrix().apply {
                            // Exclude the animation's asymmetric transparent padding when centering the artwork.
                            setTranslate(offset, offset + context.dpToPx(2f))
                        }
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
                Text(stringResource(bot.name), color = MixinAppTheme.colors.textPrimary, fontSize = 14.sp, lineHeight = 17.sp, letterSpacing = 0.sp, modifier = Modifier.weight(1f, fill = false))
                if (showDot) Box(
                    Modifier.align(Alignment.Top).padding(start = 2.dp).size(8.dp)
                        .background(MixinAppTheme.colors.tipError, CircleShape),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(stringResource(if (desktop && loggedIn) R.string.Logined else bot.description), color = MixinAppTheme.colors.textAssist, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun MoreSectionHeader(title: String, count: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable(onClick = onClick).height(48.dp).padding(start = 16.dp, top = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
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
private fun BotRow(app: ExploreApp, external: Boolean = false, avatarSize: Dp = 50.dp, showLink: Boolean = external, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp).clickable(onClick = onClick).padding(vertical = 8.dp).height(50.dp).padding(start = if (external) 16.dp else 20.dp, end = if (external) 8.dp else 40.dp), verticalAlignment = Alignment.CenterVertically) {
        CoilImage(app.iconUrl, R.drawable.ic_avatar_place_holder, Modifier.size(avatarSize).clip(CircleShape))
        Spacer(Modifier.width(if (external) 14.dp else 16.dp))
        Column(Modifier.weight(1f)) {
            AndroidView(
                factory = { NameTextView(it).apply { textView.textSize = 16f } },
                onReset = {},
                update = { it.setName(app) },
            )
            Spacer(Modifier.height(4.dp))
            Text(app.appNumber, color = MixinAppTheme.colors.textAssist, fontSize = 14.sp, lineHeight = 17.sp, letterSpacing = 0.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (showLink) Icon(painterResource(R.drawable.ic_more_link), null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
    }
}

@Composable
internal fun BotsPage(favorites: List<ExploreApp>, apps: List<ExploreApp>, onBack: () -> Unit, onEdit: () -> Unit, onFavoriteClick: (ExploreApp) -> Unit, onBotClick: (ExploreApp) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val favoriteIds = remember(favorites) { favorites.map { it.appId }.toSet() }
    val filteredFavorites = remember(favorites, query) {
        val keyword = query.trim()
        favorites.filter { it.name.contains(keyword, true) || it.appNumber.contains(keyword, true) }
    }
    val groups = remember(apps, favoriteIds, query) {
        val keyword = query.trim()
        apps.filter { it.appId !in favoriteIds && (it.name.contains(keyword, true) || it.appNumber.contains(keyword, true)) }
            .groupBy { contactInitial(it.name) }
            .toSortedMap(compareBy { if (it == "#") "[" else it })
    }
    val listState = rememberLazyListState()
    val sectionOffsets = remember(groups, filteredFavorites.size) {
        var offset = filteredFavorites.size + 2
        groups.mapValues { (_, group) -> offset.also { offset += group.size + 1 } }
    }
    MixinAppTheme {
        Column(Modifier.fillMaxSize().background(MixinAppTheme.colors.background)) {
            ContactPageHeader(stringResource(R.string.bots_title), onBack)
            ContactsSearchField(query, { query = it }, stringResource(R.string.setting_auth_search_hint))
            Box(Modifier.weight(1f)) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp)) {
                    item(key = "header:favorite", contentType = "section_header") { ContactSectionHeader(stringResource(R.string.Favorite)) }
                    items(
                        items = filteredFavorites,
                        key = { "favorite:${it.appId}" },
                        contentType = { "bot_row" },
                    ) { app -> BotRow(app) { onFavoriteClick(app) } }
                    item(key = "action:favorite_edit", contentType = "favorite_edit") {
                        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp).clickable(onClick = onEdit).padding(vertical = 8.dp).height(50.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        item(key = "header:$initial", contentType = "section_header") { ContactSectionHeader(initial) }
                        items(
                            items = group,
                            key = { it.appId },
                            contentType = { "bot_row" },
                        ) { app -> BotRow(app) { onBotClick(app) } }
                    }
                }
                BoxWithConstraints(Modifier.fillMaxSize().padding(top = 8.dp)) {
                    ContactAlphabetIndex(sectionOffsets, listState)
                }
            }
        }
    }
}
