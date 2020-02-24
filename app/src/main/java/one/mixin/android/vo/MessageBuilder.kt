package one.mixin.android.vo

import kotlin.math.abs
import one.mixin.android.extension.maxLimit

class MessageBuilder(
    val id: String,
    val conversationId: String,
    val userId: String,
    val category: String,
    val status: String,
    val createdAt: String
) {
    private var content: String? = null
    private var mediaUrl: String? = null
    private var mediaMimeType: String? = null
    private var mediaSize: Long? = null
    private var mediaDuration: String? = null
    private var thumbUrl: String? = null
    private var thumbImage: String? = null
    private var mediaWidth: Int? = null
    private var mediaHeight: Int? = null
    private var mediaHash: String? = null
    private var mediaKey: ByteArray? = null
    private var mediaDigest: ByteArray? = null
    private var mediaStatus: String? = null
    private var action: String? = null
    private var participantId: String? = null
    private var snapshotId: String? = null
    private var hyperlink: String? = null
    private var name: String? = null
    private var albumId: String? = null
    private var stickerId: String? = null
    private var sharedUserId: String? = null
    private var mediaWaveform: ByteArray? = null
    private var quoteMessageId: String? = null
    private var quoteContent: String? = null

    fun setContent(content: String?): MessageBuilder {
        this.content = content?.maxLimit()
        return this
    }

    fun setMediaUrl(mediaUrl: String?): MessageBuilder {
        this.mediaUrl = mediaUrl
        return this
    }

    fun setMediaMimeType(mediaMimeType: String?): MessageBuilder {
        this.mediaMimeType = mediaMimeType
        return this
    }

    fun setMediaSize(mediaSize: Long): MessageBuilder {
        this.mediaSize = mediaSize
        return this
    }

    fun setMediaDuration(mediaDuration: String): MessageBuilder {
        this.mediaDuration = mediaDuration
        return this
    }

    fun setThumbUrl(setThumbUrl: String?): MessageBuilder {
        this.thumbUrl = setThumbUrl
        return this
    }

    fun setMediaWidth(mediaWidth: Int?): MessageBuilder {
        this.mediaWidth = mediaWidth?.let { abs(it) }
        return this
    }

    fun setMediaHeight(mediaHeight: Int?): MessageBuilder {
        this.mediaHeight = mediaHeight?.let { abs(it) }
        return this
    }

    fun setMediaHash(mediaHash: String): MessageBuilder {
        this.mediaHash = mediaHash
        return this
    }

    fun setThumbImage(thumbImage: String?): MessageBuilder {
        this.thumbImage = thumbImage
        return this
    }

    fun setMediaKey(mediaKey: ByteArray?): MessageBuilder {
        this.mediaKey = mediaKey
        return this
    }

    fun setMediaDigest(mediaDigest: ByteArray?): MessageBuilder {
        this.mediaDigest = mediaDigest
        return this
    }

    fun setMediaStatus(mediaStatus: String): MessageBuilder {
        this.mediaStatus = mediaStatus
        return this
    }

    fun setAction(action: String?): MessageBuilder {
        this.action = action
        return this
    }

    fun setParticipantId(participantId: String?): MessageBuilder {
        this.participantId = participantId
        return this
    }

    fun setSnapshotId(snapshotId: String?): MessageBuilder {
        this.snapshotId = snapshotId
        return this
    }

    fun setHyperlink(hyperlink: String): MessageBuilder {
        this.hyperlink = hyperlink
        return this
    }

    fun setName(name: String?): MessageBuilder {
        this.name = name
        return this
    }

    fun setAlbumId(albumId: String?): MessageBuilder {
        this.albumId = albumId
        return this
    }

    fun setStickerId(stickerId: String): MessageBuilder {
        this.stickerId = stickerId
        return this
    }

    fun setSharedUserId(sharedUserId: String): MessageBuilder {
        this.sharedUserId = sharedUserId
        return this
    }

    fun setMediaWaveform(mediaWaveform: ByteArray?): MessageBuilder {
        this.mediaWaveform = mediaWaveform
        return this
    }

    fun setQuoteMessageId(quoteMessageId: String?): MessageBuilder {
        this.quoteMessageId = quoteMessageId
        return this
    }

    fun setQuoteContent(quoteContent: String?): MessageBuilder {
        this.quoteContent = quoteContent
        return this
    }

    fun build(): Message =
        Message(id, conversationId, userId, category, content, mediaUrl,
            mediaMimeType, mediaSize, mediaDuration, mediaWidth, mediaHeight, mediaHash,
            thumbImage, thumbUrl, mediaKey, mediaDigest, mediaStatus, status, createdAt,
            action, participantId, snapshotId, hyperlink, name, albumId, stickerId, sharedUserId, mediaWaveform, null, quoteMessageId, quoteContent)
}
