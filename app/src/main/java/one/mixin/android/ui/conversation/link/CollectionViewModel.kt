package one.mixin.android.ui.conversation.link

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.repository.TokenRepository
import one.mixin.android.vo.safe.SafeCollection
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import one.mixin.android.vo.InscriptionItem

@HiltViewModel
class CollectionViewModel
@Inject internal constructor(
    val tokenRepository: TokenRepository,
) : ViewModel() {

    fun inscriptionItemsFlowByCollectionHash(collectionHash: String): Flow<List<InscriptionItem>> = tokenRepository.inscriptionItemsFlowByCollectionHash(collectionHash)

    fun collectionFlowByHash(collectionHash: String): Flow<SafeCollection?> = tokenRepository.collectionFlowByHash(collectionHash)
}