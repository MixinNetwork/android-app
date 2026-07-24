package one.mixin.android.fts

import androidx.room3.RoomRawQuery
import one.mixin.android.codegen.annotation.GeneratedQueryProvider
import one.mixin.android.codegen.annotation.GeneratedRoomRawQuery

@GeneratedQueryProvider(generatedName = "FtsQueryGenerated")
interface FtsQuerySpec {
    @GeneratedRoomRawQuery(
        sql = """
            SELECT message_id, conversation_id, user_id, count(message_id)
            FROM messages_metas
            WHERE doc_id IN (SELECT docid FROM messages_fts WHERE content MATCH '{{content}}')
            GROUP BY conversation_id
            ORDER BY max(created_at) DESC
            LIMIT 999
        """,
    )
    fun rawSearch(content: String): RoomRawQuery

    @GeneratedRoomRawQuery(
        sql = """
            SELECT message_id
            FROM messages_metas
            WHERE conversation_id = '{{conversationId}}'
            AND doc_id IN (SELECT docid FROM messages_fts WHERE content MATCH '{{query}}')
            ORDER BY created_at DESC, rowid DESC
        """,
    )
    fun messageIdsByConversation(
        conversationId: String,
        query: String,
    ): RoomRawQuery

    @GeneratedRoomRawQuery(
        sql = """
            SELECT message_id
            FROM messages_metas
            WHERE conversation_id = '{{conversationId}}'
            AND doc_id IN (SELECT docid FROM messages_fts WHERE content MATCH '{{query}}')
            ORDER BY created_at DESC, rowid DESC
            LIMIT {{limit}} OFFSET {{offset}}
        """,
    )
    fun messageIdsByConversationPage(
        conversationId: String,
        query: String,
        limit: Int,
        offset: Int,
    ): RoomRawQuery
}
