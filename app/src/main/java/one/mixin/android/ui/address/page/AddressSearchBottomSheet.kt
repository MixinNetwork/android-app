package one.mixin.android.ui.address.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import one.mixin.android.ui.address.component.SearchTextField
import one.mixin.android.vo.Address

@Composable
fun AddressSearchBottomSheet(
    addresses: List<Address>,
    modalSheetState: ModalBottomSheetState? = null,
    onAddClick: () -> Unit,
    onDeleteStateChange: (Boolean) -> Unit,
) {
    LocalContext.current
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

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(filteredAddresses) { address ->
                AddressListItem(
                    address = address,
                    isDeleteMode = isDeleteMode,
                    onDeleteClick = { /* Handle delete */ }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    isDeleteMode = !isDeleteMode
                    onDeleteStateChange(isDeleteMode)
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_address_more),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {
                    scope.launch {
                        modalSheetState?.hide()
                    }
                    onAddClick()
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
    isDeleteMode: Boolean,
    onDeleteClick: (Address) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            HighlightedText(
                text = address.label,
                highlight = "",
                style = TextStyle(
                    fontSize = 16.sp,
                    color = MixinAppTheme.colors.textPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            HighlightedText(
                text = address.destination,
                highlight = "",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist
                )
            )
        }

        if (isDeleteMode) {
            IconButton(
                onClick = { onDeleteClick(address) }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.tipError
                )
            }
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
