package one.mixin.android.ui.address.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.timeAgoDate
import one.mixin.android.ui.address.component.SearchTextField
import one.mixin.android.vo.Address

@Composable
fun AddressSearchBottomSheet(
    addresses: List<Address>,
    modalSheetState: ModalBottomSheetState? = null,
    onAddClick: () -> Unit,
    onDeleteAddress: (Address) -> Unit,
    onDeleteStateChange: (Boolean) -> Unit,
) {
    var isDeleteMode by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val filteredAddresses = remember(searchText, addresses) {
        if (searchText.isBlank()) {
            addresses
        } else {
            addresses.filter { address ->
                address.label.contains(searchText, ignoreCase = true) ||
                    address.destination.contains(searchText, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MixinAppTheme.colors.background,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.weight(1f)
            )

            if (searchText.isNotEmpty() || modalSheetState != null) {
                Text(
                    text = stringResource(R.string.Cancel),
                    color = MixinAppTheme.colors.textBlue,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clickable {
                            if (searchText.isNotEmpty()) {
                                searchText = ""
                            } else if (modalSheetState != null) {
                                scope.launch {
                                    modalSheetState.hide()
                                }
                            }
                        })
            }
        }

        if (addresses.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                Row(modifier = Modifier.clickable {
                    scope.launch {
                        modalSheetState?.hide()
                        onAddClick()
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add_blue_24dp),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.Add_address),
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textBlue
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(filteredAddresses) { address ->
                    AddressListItem(
                        address = address,
                        query = searchText,
                        isDeleteMode = isDeleteMode,
                        onDeleteClick = onDeleteAddress
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            var expanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_address_more),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.textPrimary
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .background(
                            color = MixinAppTheme.colors.background,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    DropdownMenuItem(
                        onClick = {
                            isDeleteMode = !isDeleteMode
                            onDeleteStateChange(isDeleteMode)
                            expanded = false
                        },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            stringResource(R.string.Delete_address),
                            color = MixinAppTheme.colors.tipError
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_addr_remove),
                            contentDescription = null,
                            tint = MixinAppTheme.colors.tipError
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {
                    scope.launch {
                        modalSheetState?.hide()
                        onAddClick()
                    }
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_address_add),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun AddressListItem(
    address: Address,
    query: String,
    isDeleteMode: Boolean,
    onDeleteClick: (Address) -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDeleteMode) {
            IconButton(
                onClick = { onDeleteClick(address) }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete_red),
                    contentDescription = null,
                    tint = Color.Unspecified,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HighlightedText(
                    text = address.label,
                    highlight = query,
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = MixinAppTheme.colors.textPrimary
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = address.updatedAt.timeAgoDate(context),
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            HighlightedText(
                text = address.destination,
                highlight = query,
                style = TextStyle(
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist
                )
            )
        }
    }
}

@Composable
private fun HighlightedText(
    text: String,
    highlight: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    if (highlight.isBlank()) {
        Text(text = text, style = style, modifier = modifier)
        return
    }

    val annotatedString = buildAnnotatedString {
        var startIndex = 0
        var currentIndex = text.indexOf(highlight, ignoreCase = true)
        while (currentIndex >= 0) {
            append(text.substring(startIndex, currentIndex))
            withStyle(SpanStyle(color = MixinAppTheme.colors.textBlue)) {
                append(text.substring(currentIndex, currentIndex + highlight.length))
            }
            startIndex = currentIndex + highlight.length
            currentIndex = text.indexOf(highlight, startIndex, ignoreCase = true)
        }
        if (startIndex < text.length) {
            append(text.substring(startIndex))
        }
    }

    Text(
        text = annotatedString,
        style = style,
        modifier = modifier
    )
}
