package one.mixin.android.ui.contacts

import android.icu.text.AlphabeticIndex
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.compose.UserAvatarImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.address.component.SearchTextField
import one.mixin.android.vo.User
import one.mixin.android.widget.NameTextView

private val contactIndex by lazy { AlphabeticIndex<Any>(Locale.SIMPLIFIED_CHINESE).addLabels(Locale.ENGLISH).buildImmutableIndex() }

internal fun contactInitial(name: String?): String {
    val index = contactIndex
    return index.getBucket(index.getBucketIndex(name.orEmpty().trim()))?.label?.takeIf { it.length == 1 && it[0] in 'A'..'Z' } ?: "#"
}

@Composable
internal fun ContactsPage(
    contacts: List<User>,
    onBack: () -> Unit,
    onContact: (User) -> Unit,
    onNewGroup: (() -> Unit)? = null,
    onAddContact: (() -> Unit)? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val newChat = onNewGroup != null
    val groups = remember(contacts, query) {
        val keyword = query.trim()
        contacts.filter { it.fullName.orEmpty().contains(keyword, true) || it.identityNumber.contains(keyword, true) || it.phone.orEmpty().contains(keyword, true) }
            .groupBy { contactInitial(it.fullName) }
            .toSortedMap(compareBy { if (it == "#") "[" else it })
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    MixinAppTheme {
        Column(Modifier.fillMaxSize().background(MixinAppTheme.colors.background)) {
            ContactPageHeader(stringResource(if (newChat) R.string.new_chat else R.string.contacts_title), onBack, newChat)
            SearchTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), hint = stringResource(R.string.contacts_search_hint))
            Box(Modifier.weight(1f)) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    if (onNewGroup != null && onAddContact != null) {
                        item { ContactAction(R.drawable.ic_new_group, R.string.New_Group, onNewGroup) }
                        item { ContactAction(R.drawable.ic_add_contact, R.string.Add_Contact, onAddContact) }
                    }
                    if (groups.isEmpty()) {
                        item { Text(stringResource(R.string.NO_RESULTS), color = MixinAppTheme.colors.textAssist, modifier = Modifier.padding(24.dp)) }
                    }
                    groups.forEach { (initial, users) ->
                        item(key = "header:$initial") {
                            Text(initial, color = MixinAppTheme.colors.textPrimary, fontSize = 14.sp, modifier = Modifier.padding(20.dp))
                        }
                        items(users, key = { it.userId }) { user ->
                            Row(Modifier.fillMaxWidth().clickable { onContact(user) }.padding(start = 20.dp, end = 40.dp).height(70.dp), verticalAlignment = Alignment.CenterVertically) {
                                UserAvatarImage(user, 50.dp)
                                Spacer(Modifier.width(20.dp))
                                AndroidView(factory = { NameTextView(it).apply { textView.textSize = 16f } }, update = { it.setName(user) }, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                if (groups.isNotEmpty()) {
                    Column(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) {
                        (('A'..'Z').map { it.toString() } + "#").forEach { initial ->
                            Text(initial, color = MixinAppTheme.colors.textAssist, fontSize = 10.sp, lineHeight = 14.sp,
                                modifier = Modifier.clickable {
                                    var offset = if (newChat) 2 else 0
                                    for ((label, users) in groups) {
                                        if (label == initial) {
                                            val targetIndex = offset
                                            scope.launch { listState.scrollToItem(targetIndex) }
                                            break
                                        }
                                        offset += users.size + 1
                                    }
                                }.padding(horizontal = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ContactPageHeader(title: String, onBack: () -> Unit, close: Boolean = false) {
    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!close) IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_back), stringResource(R.string.Cancel), tint = MixinAppTheme.colors.textPrimary) }
        Text(title, color = MixinAppTheme.colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.W600,
            modifier = Modifier.weight(1f).padding(start = if (close) 16.dp else 0.dp), textAlign = if (close) androidx.compose.ui.text.style.TextAlign.Start else androidx.compose.ui.text.style.TextAlign.Center)
        if (close) {
            IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_circle_close), stringResource(R.string.Close), tint = androidx.compose.ui.graphics.Color.Unspecified, modifier = Modifier.size(26.dp)) }
        } else Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun ContactAction(icon: Int, title: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).height(70.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), null, tint = androidx.compose.ui.graphics.Color.Unspecified, modifier = Modifier.size(50.dp))
        Spacer(Modifier.width(20.dp))
        Text(stringResource(title), color = MixinAppTheme.colors.accent, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(painterResource(R.drawable.ic_arrow_gray_right), null, tint = MixinAppTheme.colors.iconGray)
    }
}
