package one.mixin.android.fts5

import android.content.Context

class FtsDbHelper(val context: Context) : SqlHelper(
        context, "fts.db", 1,
    listOf(
        "CREATE VIRTUAL TABLE IF NOT EXISTS `messages_fts` USING FTS4(content)",
        "CREATE TABLE IF NOT EXISTS `metas` (`doc_id` INTEGER NOT NULL, `message_id` TEXT NOT NULL, `conversation_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, PRIMARY KEY(`message_id`), FOREIGN KEY(`doc_id`) REFERENCES `messages_fts`(DOCID)) WITHOUT ROWID;"
    )

    fun 
)