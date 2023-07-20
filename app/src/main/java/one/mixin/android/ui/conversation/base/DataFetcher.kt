package one.mixin.android.ui.conversation.base

import androidx.activity.OnBackPressedDispatcher
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import one.mixin.android.vo.MessageItem

abstract class DataFetcher<T>(val db: RoomDatabase, val fetchDispatcher: CoroutineDispatcher= Dispatchers.IO) {

    abstract fun initData(): CompressedList<T>

    abstract fun loadRange(): List<T>
    abstract fun loadData(position: Int, callback: (List<MessageItem>) -> Unit)
}