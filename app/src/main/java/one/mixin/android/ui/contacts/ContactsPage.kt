package one.mixin.android.ui.contacts

import android.icu.text.AlphabeticIndex
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.compose.UserAvatarImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.vo.User
import one.mixin.android.widget.NameTextView

private val contactIndex by lazy { AlphabeticIndex<Any>(Locale.SIMPLIFIED_CHINESE).addLabels(Locale.ENGLISH).buildImmutableIndex() }
private val contactLetters = ('A'..'Z').map { it.toString() } + "#"

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
    val sectionOffsets = remember(groups, newChat) {
        var offset = if (newChat) 2 else 0
        groups.mapValues { (_, users) -> offset.also { offset += users.size + 1 } }
    }
    val scrollToLetter: (String) -> Unit = { letter ->
        sectionOffsets[letter]?.let { offset -> scope.launch { listState.scrollToItem(offset) } }
    }
    MixinAppTheme {
        val nameColor = MixinAppTheme.colors.textMinor.toArgb()
        Column(Modifier.fillMaxSize().background(MixinAppTheme.colors.background)) {
            ContactPageHeader(stringResource(if (newChat) R.string.new_chat else R.string.My_Contacts), onBack, newChat)
            ContactsSearchField(query, { query = it }, stringResource(R.string.contacts_search_hint), newChat)
            BoxWithConstraints(Modifier.weight(1f)) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    if (onNewGroup != null && onAddContact != null) {
                        item { ContactAction(R.drawable.ic_contacts_new_group, R.string.New_Group, onNewGroup) }
                        item { ContactAction(R.drawable.ic_contacts_add_contact, R.string.Add_Contact, onAddContact) }
                    }
                    if (groups.isEmpty()) {
                        item { Text(stringResource(R.string.NO_RESULTS), color = MixinAppTheme.colors.textAssist, modifier = Modifier.padding(24.dp)) }
                    }
                    groups.forEach { (initial, users) ->
                        item(key = "header:$initial") {
                            ContactSectionHeader(initial)
                        }
                        items(users, key = { it.userId }) { user ->
                            Row(Modifier.fillMaxWidth().clickable { onContact(user) }.height(70.dp).padding(start = 20.dp, end = 40.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                                UserAvatarImage(user, 50.dp)
                                Spacer(Modifier.width(16.dp))
                                AndroidView(factory = { NameTextView(it).apply { textView.textSize = 16f } }, update = { it.setName(user); it.setTextColor(nameColor) }, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                if (groups.isNotEmpty()) {
                    val railTop = minOf(if (newChat) 200.dp else 60.dp, (maxHeight - 378.dp).coerceAtLeast(0.dp))
                    Column(
                        Modifier.align(Alignment.TopEnd).padding(top = railTop, end = 8.dp)
                            .height((maxHeight - railTop).coerceAtMost(378.dp))
                            .pointerInput(sectionOffsets) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    down.consume()
                                    var previousLetter: String? = null
                                    fun selectLetter(y: Float) {
                                        val letter = contactLetterAt(y, size.height)
                                        if (letter != previousLetter) {
                                            previousLetter = letter
                                            scrollToLetter(letter)
                                        }
                                    }
                                    selectLetter(down.position.y)
                                    drag(down.id) { change ->
                                        change.consume()
                                        selectLetter(change.position.y)
                                    }
                                }
                            },
                    ) {
                        contactLetters.forEach { initial ->
                            Text(initial, color = Color(0xFFB8BDC7), fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(32.dp).weight(1f).semantics {
                                    onClick { scrollToLetter(initial); true }
                                })
                        }
                    }
                }
            }
        }
    }
}

internal fun contactLetterAt(y: Float, height: Int): String =
    contactLetters[(y / height.coerceAtLeast(1) * contactLetters.size).toInt().coerceIn(contactLetters.indices)]

@Composable
internal fun ContactPageHeader(title: String, onBack: () -> Unit, close: Boolean = false) {
    Row(Modifier.fillMaxWidth().height(if (close) 70.dp else 64.dp).padding(end = if (close) 5.dp else 0.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!close) IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_back), stringResource(R.string.Cancel), tint = MixinAppTheme.colors.textPrimary) }
        Text(title, color = if (close) MixinAppTheme.colors.textMinor else MixinAppTheme.colors.textPrimary, fontSize = 18.sp, lineHeight = 21.sp, letterSpacing = if (close) (-0.4).sp else 0.sp, fontWeight = if (close) FontWeight.W600 else FontWeight.W500,
            modifier = Modifier.weight(1f).padding(start = if (close) 16.dp else 0.dp), textAlign = if (close) TextAlign.Start else TextAlign.Center)
        if (close) {
            IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_circle_close), stringResource(R.string.Close), tint = Color.Unspecified, modifier = Modifier.size(26.dp)) }
        } else Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun ContactAction(icon: Int, title: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).height(70.dp).padding(start = 20.dp, end = 16.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), null, tint = Color.Unspecified, modifier = Modifier.size(50.dp))
        Spacer(Modifier.width(16.dp))
        Text(stringResource(title), color = MixinAppTheme.colors.accent, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 0.sp, modifier = Modifier.weight(1f))
        Icon(painterResource(R.drawable.ic_profile_arrow), null, tint = Color.Unspecified, modifier = Modifier.size(30.dp))
    }
}

@Composable
internal fun ContactSectionHeader(title: String) {
    Text(title, color = MixinAppTheme.colors.textMinor, fontSize = 14.sp, lineHeight = 17.sp, letterSpacing = 0.sp,
        modifier = Modifier.fillMaxWidth().height(47.dp).padding(start = 20.dp, top = 10.dp))
}

@Composable
internal fun ContactsSearchField(value: String, onValueChange: (String) -> Unit, hint: String, newChat: Boolean = false) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = if (newChat) 0.dp else 10.dp, bottom = 20.dp)
            .background(MixinAppTheme.colors.backgroundGrayLight, RoundedCornerShape(20.dp)),
        textStyle = TextStyle(color = MixinAppTheme.colors.textPrimary, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
        singleLine = true,
        cursorBrush = SolidColor(MixinAppTheme.colors.accent),
        decorationBox = { innerTextField ->
            Row(Modifier.height(40.dp).padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_contacts_search), null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(14.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(hint, color = Color(0xFFBCBEC3), fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 0.sp, maxLines = 1)
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(40.dp)) {
                        Icon(painterResource(R.drawable.ic_float_close), stringResource(R.string.Clear), tint = Color.Unspecified, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Spacer(Modifier.width(14.dp))
                }
            }
        },
    )
}
