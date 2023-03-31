package one.mixin.android.ui.transfer.vo

import androidx.lifecycle.LiveData

class TransferStatusLiveData : LiveData<TransferStatus>() {
    var value: TransferStatus = TransferStatus.INITIALIZING
        set(value) {
            if (value == field) return
            field = value
            postValue(value)
        }
}
