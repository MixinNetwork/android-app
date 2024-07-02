package one.mixin.android.ui.home.inscription.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.request.ImageRequest
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.vo.User

@Composable
fun OwnerScreen(hash: String) {
    val viewModel = hiltViewModel<Web3ViewModel>()
    var isLoading by remember { mutableStateOf(true) }
    var owners by remember { mutableStateOf(emptyList<User>()) }
    var owner by remember { mutableStateOf("") }

    LaunchedEffect(hash) {
        viewModel.getOwner(hash)?.let {
            owners = it.first ?: emptyList()
            owner = it.second ?: ""
        }
        isLoading = false
    }
    Box(modifier = Modifier.height(20.dp))
    Text(
        text = stringResource(id = R.string.collectible_owner).uppercase(),
        fontSize = 16.sp,
        color = Color(0xFF999999),
    )
    Box(modifier = Modifier.height(8.dp))
    if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.height(18.dp))
    } else if (owner.isNotEmpty()) {
        SelectionContainer {
            Text(text = owner, fontSize = 16.sp, color = Color.White)
        }
    } else {
        WrappingRow{
            owners.forEach {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoilImage(
                        model =
                        ImageRequest.Builder(LocalContext.current)
                            .data(it.avatarUrl)
                            .build(),
                        modifier = Modifier
                            .width(18.dp)
                            .height(18.dp)
                            .clip(CircleShape),
                        placeholder = R.drawable.ic_avatar_place_holder,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${it.fullName} (${it.identityNumber})", fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }

}