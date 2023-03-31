package one.mixin.android.ui.transfer.vo

import androidx.lifecycle.LiveData

class TransferStatusLiveData : LiveData<TransferStatus>() {
    var status: TransferStatus = TransferStatus.INITIALIZING
        set(value) {
            if (value == field) return
            field = value
            postValue(value)
        }
}