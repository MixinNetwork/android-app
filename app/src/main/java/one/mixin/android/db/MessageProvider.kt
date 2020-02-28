package one.mixin.android.db

import android.annotation.SuppressLint
import android.database.Cursor
import androidx.paging.DataSource
import androidx.room.RoomSQLiteQuery
import one.mixin.android.util.chat.MixinLimitOffsetDataSource
import one.mixin.android.vo.MessageItem

@SuppressLint("RestrictedApi")
class MessageProvider {
    companion object {
        fun getMessages(conversationId: String, database: MixinDatabase) =
            object : DataSource.Factory<Int, MessageItem>() {
                override fun create(): DataSource<Int, MessageItem> {
                    val sql = """
                        SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId,
                        u.full_name AS userFullName, u.identity_number AS userIdentityNumber, u.app_id AS appId, m.category AS type,
                        m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus, m.media_waveform AS mediaWaveform,
                        m.name AS mediaName, m.media_mime_type AS mediaMimeType, m.media_size AS mediaSize, m.media_width AS mediaWidth, m.media_height AS mediaHeight,
                        m.thumb_image AS thumbImage, m.thumb_url AS thumbUrl, m.media_url AS mediaUrl, m.media_duration AS mediaDuration, m.quote_message_id as quoteId,
                        m.quote_content as quoteContent, u1.full_name AS participantFullName, m.action AS actionName, u1.user_id AS participantUserId,
                        s.snapshot_id AS snapshotId, s.type AS snapshotType, s.amount AS snapshotAmount, a.symbol AS assetSymbol, s.asset_id AS assetId,
                        a.icon_url AS assetIcon, st.asset_url AS assetUrl, st.asset_width AS assetWidth, st.asset_height AS assetHeight, st.sticker_id AS stickerId,
                        st.name AS assetName, st.asset_type AS assetType, h.site_name AS siteName, h.site_title AS siteTitle, h.site_description AS siteDescription,
                        h.site_image AS siteImage, m.shared_user_id AS sharedUserId, su.full_name AS sharedUserFullName, su.identity_number AS sharedUserIdentityNumber,
                        su.avatar_url AS sharedUserAvatarUrl, su.is_verified AS sharedUserIsVerified, su.app_id AS sharedUserAppId, mm.mentions AS mentions, mm.has_read as mentionRead, 
                        c.name AS groupName
                        FROM messages m
                        INNER JOIN users u ON m.user_id = u.user_id
                        LEFT JOIN users u1 ON m.participant_id = u1.user_id
                        LEFT JOIN snapshots s ON m.snapshot_id = s.snapshot_id
                        LEFT JOIN assets a ON s.asset_id = a.asset_id
                        LEFT JOIN stickers st ON st.sticker_id = m.sticker_id
                        LEFT JOIN hyperlinks h ON m.hyperlink = h.hyperlink
                        LEFT JOIN users su ON m.shared_user_id = su.user_id
                        LEFT JOIN conversations c ON m.conversation_id = c.conversation_id
                        LEFT JOIN message_mentions mm ON m.id = mm.message_id
                        WHERE m.conversation_id = ? 
                        ORDER BY m.created_at DESC 
                    """
                    val statement = RoomSQLiteQuery.acquire(sql, 1)
                    val argIndex = 1
                    statement.bindString(argIndex, conversationId)

                    val countSql = "SELECT COUNT(*) FROM messages WHERE conversation_id = ?"
                    val countStatement = RoomSQLiteQuery.acquire(countSql, 1)
                    countStatement.bindString(argIndex, conversationId)
                    return object : MixinLimitOffsetDataSource<MessageItem>(database, statement, countStatement, false, "messages", "users", "snapshots", "assets", "stickers", "hyperlinks", "conversations", "message_mentions") {
                        override fun convertRows(cursor: Cursor?): MutableList<MessageItem> {
                            cursor ?: return ArrayList()
                            val cursorIndexOfMessageId = cursor.getColumnIndexOrThrow("messageId")
                            val cursorIndexOfConversationId = cursor.getColumnIndexOrThrow("conversationId")
                            val cursorIndexOfUserId = cursor.getColumnIndexOrThrow("userId")
                            val cursorIndexOfUserFullName = cursor.getColumnIndexOrThrow("userFullName")
                            val cursorIndexOfUserIdentityNumber = cursor.getColumnIndexOrThrow("userIdentityNumber")
                            val cursorIndexOfAppId = cursor.getColumnIndexOrThrow("appId")
                            val cursorIndexOfType = cursor.getColumnIndexOrThrow("type")
                            val cursorIndexOfContent = cursor.getColumnIndexOrThrow("content")
                            val cursorIndexOfCreatedAt = cursor.getColumnIndexOrThrow("createdAt")
                            val cursorIndexOfStatus = cursor.getColumnIndexOrThrow("status")
                            val cursorIndexOfMediaStatus = cursor.getColumnIndexOrThrow("mediaStatus")
                            val cursorIndexOfMediaWaveform = cursor.getColumnIndexOrThrow("mediaWaveform")
                            val cursorIndexOfMediaName = cursor.getColumnIndexOrThrow("mediaName")
                            val cursorIndexOfMediaMimeType = cursor.getColumnIndexOrThrow("mediaMimeType")
                            val cursorIndexOfMediaSize = cursor.getColumnIndexOrThrow("mediaSize")
                            val cursorIndexOfMediaWidth = cursor.getColumnIndexOrThrow("mediaWidth")
                            val cursorIndexOfMediaHeight = cursor.getColumnIndexOrThrow("mediaHeight")
                            val cursorIndexOfThumbImage = cursor.getColumnIndexOrThrow("thumbImage")
                            val cursorIndexOfThumbUrl = cursor.getColumnIndexOrThrow("thumbUrl")
                            val cursorIndexOfMediaUrl = cursor.getColumnIndexOrThrow("mediaUrl")
                            val cursorIndexOfMediaDuration = cursor.getColumnIndexOrThrow("mediaDuration")
                            val cursorIndexOfQuoteId = cursor.getColumnIndexOrThrow("quoteId")
                            val cursorIndexOfQuoteContent = cursor.getColumnIndexOrThrow("quoteContent")
                            val cursorIndexOfParticipantFullName = cursor.getColumnIndexOrThrow("participantFullName")
                            val cursorIndexOfActionName = cursor.getColumnIndexOrThrow("actionName")
                            val cursorIndexOfParticipantUserId = cursor.getColumnIndexOrThrow("participantUserId")
                            val cursorIndexOfSnapshotId = cursor.getColumnIndexOrThrow("snapshotId")
                            val cursorIndexOfSnapshotType = cursor.getColumnIndexOrThrow("snapshotType")
                            val cursorIndexOfSnapshotAmount = cursor.getColumnIndexOrThrow("snapshotAmount")
                            val cursorIndexOfAssetSymbol = cursor.getColumnIndexOrThrow("assetSymbol")
                            val cursorIndexOfAssetId = cursor.getColumnIndexOrThrow("assetId")
                            val cursorIndexOfAssetIcon = cursor.getColumnIndexOrThrow("assetIcon")
                            val cursorIndexOfAssetUrl = cursor.getColumnIndexOrThrow("assetUrl")
                            val cursorIndexOfAssetWidth = cursor.getColumnIndexOrThrow("assetWidth")
                            val cursorIndexOfAssetHeight = cursor.getColumnIndexOrThrow("assetHeight")
                            val cursorIndexOfStickerId = cursor.getColumnIndexOrThrow("stickerId")
                            val cursorIndexOfAssetName = cursor.getColumnIndexOrThrow("assetName")
                            val cursorIndexOfAssetType = cursor.getColumnIndexOrThrow("assetType")
                            val cursorIndexOfSiteName = cursor.getColumnIndexOrThrow("siteName")
                            val cursorIndexOfSiteTitle = cursor.getColumnIndexOrThrow("siteTitle")
                            val cursorIndexOfSiteDescription = cursor.getColumnIndexOrThrow("siteDescription")
                            val cursorIndexOfSiteImage = cursor.getColumnIndexOrThrow("siteImage")
                            val cursorIndexOfSharedUserId = cursor.getColumnIndexOrThrow("sharedUserId")
                            val cursorIndexOfSharedUserFullName = cursor.getColumnIndexOrThrow("sharedUserFullName")
                            val cursorIndexOfSharedUserIdentityNumber = cursor.getColumnIndexOrThrow("sharedUserIdentityNumber")
                            val cursorIndexOfSharedUserAvatarUrl = cursor.getColumnIndexOrThrow("sharedUserAvatarUrl")
                            val cursorIndexOfSharedUserIsVerified = cursor.getColumnIndexOrThrow("sharedUserIsVerified")
                            val cursorIndexOfSharedUserAppId = cursor.getColumnIndexOrThrow("sharedUserAppId")
                            val cursorIndexOfGroupName = cursor.getColumnIndexOrThrow("groupName")
                            val cursorIndexOfMentions = cursor.getColumnIndexOrThrow("mentions")
                            val cursorIndexOfMentionRead = cursor.getColumnIndexOrThrow("mentionRead")
                            val res = ArrayList<MessageItem>(cursor.count)
                            while (cursor.moveToNext()) {
                                val item: MessageItem
                                val tmpMessageId: String = cursor.getString(cursorIndexOfMessageId)
                                val tmpConversationId: String = cursor.getString(cursorIndexOfConversationId)
                                val tmpUserId: String = cursor.getString(cursorIndexOfUserId)
                                val tmpUserFullName: String = cursor.getString(cursorIndexOfUserFullName)
                                val tmpUserIdentityNumber: String = cursor.getString(cursorIndexOfUserIdentityNumber)
                                val tmpAppId: String? = cursor.getString(cursorIndexOfAppId)
                                val tmpType: String = cursor.getString(cursorIndexOfType)
                                val tmpContent: String? = cursor.getString(cursorIndexOfContent)
                                val tmpCreatedAt: String = cursor.getString(cursorIndexOfCreatedAt)
                                val tmpStatus: String = cursor.getString(cursorIndexOfStatus)
                                val tmpMediaStatus: String? = cursor.getString(cursorIndexOfMediaStatus)
                                val tmpMediaWaveform: ByteArray? = cursor.getBlob(cursorIndexOfMediaWaveform)
                                val tmpMediaName: String? = cursor.getString(cursorIndexOfMediaName)
                                val tmpMediaMimeType: String? = cursor.getString(cursorIndexOfMediaMimeType)
                                val tmpMediaSize: Long? = if (cursor.isNull(cursorIndexOfMediaSize)) {
                                    null
                                } else {
                                    cursor.getLong(cursorIndexOfMediaSize)
                                }
                                val tmpMediaWidth: Int? = if (cursor.isNull(cursorIndexOfMediaWidth)) {
                                    null
                                } else {
                                    cursor.getInt(cursorIndexOfMediaWidth)
                                }
                                val tmpMediaHeight: Int? = if (cursor.isNull(cursorIndexOfMediaHeight)) {
                                    null
                                } else {
                                    cursor.getInt(cursorIndexOfMediaHeight)
                                }
                                val tmpThumbImage: String? = cursor.getString(cursorIndexOfThumbImage)
                                val tmpThumbUrl: String? = cursor.getString(cursorIndexOfThumbUrl)
                                val tmpMediaUrl: String? = cursor.getString(cursorIndexOfMediaUrl)
                                val tmpMediaDuration: String? = cursor.getString(cursorIndexOfMediaDuration)
                                val tmpQuoteId: String? = cursor.getString(cursorIndexOfQuoteId)
                                val tmpQuoteContent: String? = cursor.getString(cursorIndexOfQuoteContent)
                                val tmpParticipantFullName: String? = cursor.getString(cursorIndexOfParticipantFullName)
                                val tmpActionName: String? = cursor.getString(cursorIndexOfActionName)
                                val tmpParticipantUserId: String? = cursor.getString(cursorIndexOfParticipantUserId)
                                val tmpSnapshotId: String? = cursor.getString(cursorIndexOfSnapshotId)
                                val tmpSnapshotType: String? = cursor.getString(cursorIndexOfSnapshotType)
                                val tmpSnapshotAmount: String? = cursor.getString(cursorIndexOfSnapshotAmount)
                                val tmpAssetSymbol: String? = cursor.getString(cursorIndexOfAssetSymbol)
                                val tmpAssetId: String? = cursor.getString(cursorIndexOfAssetId)
                                val tmpAssetIcon: String? = cursor.getString(cursorIndexOfAssetIcon)
                                val tmpAssetUrl: String? = cursor.getString(cursorIndexOfAssetUrl)
                                val tmpAssetWidth: Int? = if (cursor.isNull(cursorIndexOfAssetWidth)) {
                                    null
                                } else {
                                    cursor.getInt(cursorIndexOfAssetWidth)
                                }
                                val tmpAssetHeight: Int? = if (cursor.isNull(cursorIndexOfAssetHeight)) {
                                    null
                                } else {
                                    cursor.getInt(cursorIndexOfAssetHeight)
                                }
                                val tmpStickerId: String? = cursor.getString(cursorIndexOfStickerId)
                                val tmpAssetName: String? = cursor.getString(cursorIndexOfAssetName)
                                val tmpAssetType: String? = cursor.getString(cursorIndexOfAssetType)
                                val tmpSiteName: String? = cursor.getString(cursorIndexOfSiteName)
                                val tmpSiteTitle: String? = cursor.getString(cursorIndexOfSiteTitle)
                                val tmpSiteDescription: String? = cursor.getString(cursorIndexOfSiteDescription)
                                val tmpSiteImage: String? = cursor.getString(cursorIndexOfSiteImage)
                                val tmpSharedUserId: String? = cursor.getString(cursorIndexOfSharedUserId)
                                val tmpSharedUserFullName: String? = cursor.getString(cursorIndexOfSharedUserFullName)
                                val tmpSharedUserIdentityNumber: String? = cursor.getString(cursorIndexOfSharedUserIdentityNumber)
                                val tmpSharedUserAvatarUrl: String? = cursor.getString(cursorIndexOfSharedUserAvatarUrl)
                                val tmpSharedUserIsVerified: Boolean?
                                val tmp: Int? = if (cursor.isNull(cursorIndexOfSharedUserIsVerified)) {
                                    null
                                } else {
                                    cursor.getInt(cursorIndexOfSharedUserIsVerified)
                                }
                                tmpSharedUserIsVerified = if (tmp == null) null else tmp != 0
                                val tmpSharedUserAppId: String? = cursor.getString(cursorIndexOfSharedUserAppId)
                                val tmpGroupName: String? = cursor.getString(cursorIndexOfGroupName)
                                val tmpMentions: String? = cursor.getString(cursorIndexOfMentions)
                                val tmp_1 = if (cursor.isNull(cursorIndexOfMentionRead)) {
                                null
                                } else {
                                   cursor.getInt(cursorIndexOfMentionRead)
                                }
                                val tmpMentionRead = if (tmp_1 == null) null else tmp_1 != 0
                                item = MessageItem(tmpMessageId, tmpConversationId, tmpUserId, tmpUserFullName, tmpUserIdentityNumber, tmpType, tmpContent,
                                    tmpCreatedAt, tmpStatus, tmpMediaStatus, null, tmpMediaName, tmpMediaMimeType, tmpMediaSize, tmpThumbUrl, tmpMediaWidth,
                                    tmpMediaHeight, tmpThumbImage, tmpMediaUrl, tmpMediaDuration, tmpParticipantFullName, tmpParticipantUserId, tmpActionName, tmpSnapshotId,
                                    tmpSnapshotType, tmpSnapshotAmount, tmpAssetId, tmpAssetType, tmpAssetSymbol, tmpAssetIcon, tmpAssetUrl, tmpAssetHeight, tmpAssetWidth,
                                    null, tmpStickerId, tmpAssetName, tmpAppId, tmpSiteName, tmpSiteTitle, tmpSiteDescription, tmpSiteImage, tmpSharedUserId,
                                    tmpSharedUserFullName, tmpSharedUserIdentityNumber, tmpSharedUserAvatarUrl, tmpSharedUserIsVerified, tmpSharedUserAppId,
                                    tmpMediaWaveform, tmpQuoteId, tmpQuoteContent, tmpGroupName, tmpMentions, tmpMentionRead)
                                res.add(item)
                            }
                            return res
                        }
                    }
                }
            }
    }
}
