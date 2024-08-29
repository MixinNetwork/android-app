package one.mixin.android.ui.transfer

import android.database.SQLException
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.converter.DepositEntryListConverter
import one.mixin.android.db.converter.SafeDepositConverter
import one.mixin.android.db.converter.SafeWithdrawalConverter
import one.mixin.android.db.converter.WithdrawalMemoPossibilityConverter
import one.mixin.android.vo.App
import one.mixin.android.vo.Asset
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ExpiredMessage
import one.mixin.android.db.converter.ListConverter
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageMention
import one.mixin.android.vo.Participant
import one.mixin.android.vo.PinMessage
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.SafeSnapshot
import one.mixin.android.vo.safe.Token
import timber.log.Timber

class TransferInserter {
    var primaryId: String? = null // Save the currently inserted primary key id
        private set(value) {
            field = value
            Timber.e("Insert primaryId $value")
        }
    private var assistanceId: String? = null
        private set(value) {
            if (field != value) {
                Timber.e("Insert assistanceId $value")
            }
            field = value
        }

    private val writableDatabase by lazy {
        requireNotNull(MixinDatabase.getWritableDatabase())
    }

    fun insertMessages(messages: List<Message>) {
        if (messages.isEmpty()) return
        val sql =
            "INSERT OR IGNORE INTO messages (id, conversation_id, user_id, category, content, media_url, media_mime_type, media_size, media_duration, media_width, media_height, media_hash, thumb_image, thumb_url, media_key, media_digest, media_status, status, created_at, action, participant_id, snapshot_id, hyperlink, name, album_id, sticker_id, shared_user_id, media_waveform, media_mine_type, quote_message_id, quote_content, caption) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"

        val statement = writableDatabase.compileStatement(sql)

        writableDatabase.beginTransaction()
        try {
            for (message in messages) {
                statement.bindString(1, message.messageId)
                statement.bindString(2, message.conversationId)
                statement.bindString(3, message.userId)
                statement.bindString(4, message.category)

                val content = message.content
                if (content != null) {
                    statement.bindString(5, content)
                } else {
                    statement.bindNull(5)
                }

                val mediaUrl = message.mediaUrl
                if (mediaUrl != null) {
                    statement.bindString(6, mediaUrl)
                } else {
                    statement.bindNull(6)
                }

                val mediaMimeType = message.mediaMimeType
                if (mediaMimeType != null) {
                    statement.bindString(7, mediaMimeType)
                } else {
                    statement.bindNull(7)
                }

                val mediaSize = message.mediaSize
                if (mediaSize != null) {
                    statement.bindLong(8, mediaSize)
                } else {
                    statement.bindNull(8)
                }

                val mediaDuration = message.mediaDuration
                if (mediaDuration != null) {
                    statement.bindString(9, mediaDuration)
                } else {
                    statement.bindNull(9)
                }

                val mediaWidth = message.mediaWidth
                if (mediaWidth != null) {
                    statement.bindLong(10, mediaWidth.toLong())
                } else {
                    statement.bindNull(10)
                }

                val mediaHeight = message.mediaHeight
                if (mediaHeight != null) {
                    statement.bindLong(11, mediaHeight.toLong())
                } else {
                    statement.bindNull(11)
                }

                val mediaHash = message.mediaHash
                if (mediaHash != null) {
                    statement.bindString(12, mediaHash)
                } else {
                    statement.bindNull(12)
                }

                val thumbImage = message.thumbImage
                if (thumbImage != null) {
                    statement.bindString(13, thumbImage)
                } else {
                    statement.bindNull(13)
                }

                val thumbUrl = message.thumbUrl
                if (thumbUrl != null) {
                    statement.bindString(14, thumbUrl)
                } else {
                    statement.bindNull(14)
                }

                val mediaKey = message.mediaKey
                if (mediaKey != null) {
                    statement.bindBlob(15, mediaKey)
                } else {
                    statement.bindNull(15)
                }

                val mediaDigest = message.mediaDigest
                if (mediaDigest != null) {
                    statement.bindBlob(16, mediaDigest)
                } else {
                    statement.bindNull(16)
                }

                val mediaStatus = message.mediaStatus
                if (mediaStatus == null) {
                    statement.bindNull(17)
                } else {
                    statement.bindString(17, mediaStatus)
                }
                statement.bindString(18, message.status)
                statement.bindString(19, message.createdAt)
                val action = message.action
                if (action == null) {
                    statement.bindNull(20)
                } else {
                    statement.bindString(20, action)
                }

                val participantId = message.participantId
                if (participantId != null) {
                    statement.bindString(21, participantId)
                } else {
                    statement.bindNull(21)
                }

                val snapshotId = message.snapshotId
                if (snapshotId != null) {
                    statement.bindString(22, snapshotId)
                } else {
                    statement.bindNull(22)
                }

                val hyperlink = message.hyperlink
                if (hyperlink != null) {
                    statement.bindString(23, hyperlink)
                } else {
                    statement.bindNull(23)
                }

                val name = message.name
                if (name != null) {
                    statement.bindString(24, name)
                } else {
                    statement.bindNull(24)
                }

                val albumId = message.albumId
                if (albumId != null) {
                    statement.bindString(25, albumId)
                } else {
                    statement.bindNull(25)
                }

                val stickerId = message.stickerId
                if (stickerId != null) {
                    statement.bindString(26, stickerId)
                } else {
                    statement.bindNull(26)
                }

                val sharedUserId = message.sharedUserId
                if (sharedUserId != null) {
                    statement.bindString(27, sharedUserId)
                } else {
                    statement.bindNull(27)
                }

                val mediaWaveform = message.mediaWaveform
                if (mediaWaveform != null) {
                    statement.bindBlob(28, mediaWaveform)
                } else {
                    statement.bindNull(28)
                }

                statement.bindNull(29)

                val quoteMessageId = message.quoteMessageId
                if (quoteMessageId != null) {
                    statement.bindString(30, quoteMessageId)
                } else {
                    statement.bindNull(30)
                }

                val quoteContent = message.quoteContent
                if (quoteContent != null) {
                    statement.bindString(31, quoteContent)
                } else {
                    statement.bindNull(31)
                }

                val caption = message.caption
                if (caption != null) {
                    statement.bindString(32, caption)
                } else {
                    statement.bindNull(32)
                }

                statement.executeInsert()
            }
            writableDatabase.setTransactionSuccessful()
            primaryId = messages.last().messageId
            assistanceId = null
        } catch (e: SQLException) {
            Timber.e(e)
        } finally {
            writableDatabase.endTransaction()
            statement.close()
        }
    }

    fun insertIgnore(conversation: Conversation) {
        val stmt =
            writableDatabase.compileStatement("INSERT OR IGNORE INTO `conversations` (`conversation_id`, `owner_id`, `category`, `name`, `icon_url`, `announcement`, `code_url`, `pay_type`, `created_at`, `pin_time`, `last_message_id`, `last_read_message_id`, `unseen_message_count`, `status`, `draft`, `mute_until`, `last_message_created_at`, `expire_in`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        try {
            stmt.bindString(1, conversation.conversationId)

            if (conversation.ownerId == null) {
                stmt.bindNull(2)
            } else {
                stmt.bindString(2, conversation.ownerId)
            }
            if (conversation.category == null) {
                stmt.bindNull(3)
            } else {
                stmt.bindString(3, conversation.category)
            }
            if (conversation.name == null) {
                stmt.bindNull(4)
            } else {
                stmt.bindString(4, conversation.name)
            }
            if (conversation.iconUrl == null) {
                stmt.bindNull(5)
            } else {
                stmt.bindString(5, conversation.iconUrl)
            }
            if (conversation.announcement == null) {
                stmt.bindNull(6)
            } else {
                stmt.bindString(6, conversation.announcement)
            }
            if (conversation.codeUrl == null) {
                stmt.bindNull(7)
            } else {
                stmt.bindString(7, conversation.codeUrl)
            }
            if (conversation.payType == null) {
                stmt.bindNull(8)
            } else {
                stmt.bindString(8, conversation.payType)
            }
            stmt.bindString(9, conversation.createdAt)
            if (conversation.pinTime == null) {
                stmt.bindNull(10)
            } else {
                stmt.bindString(10, conversation.pinTime)
            }
            if (conversation.lastMessageId == null) {
                stmt.bindNull(11)
            } else {
                stmt.bindString(11, conversation.lastMessageId)
            }
            if (conversation.lastReadMessageId == null) {
                stmt.bindNull(12)
            } else {
                stmt.bindString(12, conversation.lastReadMessageId)
            }
            if (conversation.unseenMessageCount == null) {
                stmt.bindNull(13)
            } else {
                stmt.bindLong(13, conversation.unseenMessageCount.toLong())
            }
            stmt.bindLong(14, conversation.status.toLong())
            if (conversation.draft == null) {
                stmt.bindNull(15)
            } else {
                stmt.bindString(15, conversation.draft)
            }
            if (conversation.muteUntil == null) {
                stmt.bindNull(16)
            } else {
                stmt.bindString(16, conversation.muteUntil)
            }
            if (conversation.lastMessageCreatedAt == null) {
                stmt.bindNull(17)
            } else {
                stmt.bindString(17, conversation.lastMessageCreatedAt)
            }
            if (conversation.expireIn == null) {
                stmt.bindNull(18)
            } else {
                stmt.bindLong(18, conversation.expireIn)
            }
            stmt.executeInsert()
            primaryId = conversation.conversationId
            assistanceId = null
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            stmt.close()
        }
    }

    fun insertIgnore(participant: Participant) {
        val stmt =
            writableDatabase.compileStatement("INSERT OR IGNORE INTO `participants` (`conversation_id`, `user_id`, `role`, `created_at`) VALUES (?, ?, ?, ?)")
        try {
            stmt.bindString(1, participant.conversationId)
            stmt.bindString(1, participant.conversationId)
            stmt.bindString(2, participant.userId)
            stmt.bindString(3, participant.role)
            stmt.bindString(4, participant.createdAt)
            stmt.executeInsert()
            primaryId = participant.userId
            assistanceId = participant.conversationId
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            stmt.close()
        }
    }

    fun insertIgnore(user: User) {
        val stmt =
            writableDatabase.compileStatement("INSERT OR IGNORE INTO `users` (`user_id`, `identity_number`, `relationship`, `biography`, `full_name`, `avatar_url`, `phone`, `is_verified`, `created_at`, `mute_until`, `has_pin`, `app_id`, `is_scam`, `is_deactivated`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        try {
            stmt.bindString(1, user.userId)
            stmt.bindString(2, user.identityNumber)
            stmt.bindString(3, user.relationship)
            stmt.bindString(4, user.biography)
            if (user.fullName == null) {
                stmt.bindNull(5)
            } else {
                stmt.bindString(5, user.fullName)
            }
            if (user.avatarUrl == null) {
                stmt.bindNull(6)
            } else {
                stmt.bindString(6, user.avatarUrl)
            }
            if (user.phone == null) {
                stmt.bindNull(7)
            } else {
                stmt.bindString(7, user.phone)
            }
            val isVerified =
                if (user.isVerified == null) {
                    null
                } else if (user.isVerified) {
                    1
                } else {
                    0
                }
            if (isVerified == null) {
                stmt.bindNull(8)
            } else {
                stmt.bindLong(8, isVerified.toLong())
            }
            if (user.createdAt == null) {
                stmt.bindNull(9)
            } else {
                stmt.bindString(9, user.createdAt)
            }
            val muteUntil = user.muteUntil
            if (muteUntil == null) {
                stmt.bindNull(10)
            } else {
                stmt.bindString(10, muteUntil)
            }
            val hasPin =
                if (user.hasPin == null) {
                    null
                } else if (user.hasPin) {
                    1
                } else {
                    0
                }
            if (hasPin == null) {
                stmt.bindNull(11)
            } else {
                stmt.bindLong(11, hasPin.toLong())
            }
            val appId = user.appId
            if (appId == null) {
                stmt.bindNull(12)
            } else {
                stmt.bindString(12, appId)
            }
            val isScam = user.isScam
            val userIsScam =
                if (isScam == null) {
                    null
                } else if (isScam) {
                    1
                } else {
                    0
                }
            if (userIsScam == null) {
                stmt.bindNull(13)
            } else {
                stmt.bindLong(13, userIsScam.toLong())
            }
            val isDeactivated = user.isDeactivated
            val userIsDeactivated =
                if (isDeactivated == null) {
                    null
                } else if (isDeactivated) {
                    1
                } else {
                    0
                }
            if (userIsDeactivated == null) {
                stmt.bindNull(14)
            } else {
                stmt.bindLong(14, userIsDeactivated.toLong())
            }
            stmt.executeInsert()
            primaryId = user.userId
            assistanceId = null
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            stmt.close()
        }
    }

    fun insertIgnore(app: App) {
        val stmt =
            writableDatabase.compileStatement("INSERT OR IGNORE INTO `apps` (`app_id`, `app_number`, `home_uri`, `redirect_uri`, `name`, `icon_url`, `category`, `description`, `app_secret`, `capabilities`, `creator_id`, `resource_patterns`, `updated_at`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        try {
            stmt.bindString(1, app.appId)
            stmt.bindString(2, app.appNumber)
            stmt.bindString(3, app.homeUri)
            stmt.bindString(4, app.redirectUri)
            stmt.bindString(5, app.name)
            stmt.bindString(6, app.iconUrl)
            if (app.category == null) {
                stmt.bindNull(7)
            } else {
                stmt.bindString(7, app.category)
            }
            stmt.bindString(8, app.description)
            stmt.bindString(9, app.appSecret)
            val capabilities = app.capabilities
            if (capabilities == null) {
                stmt.bindNull(10)
            } else {
                stmt.bindString(10, ListConverter.fromList(capabilities))
            }
            stmt.bindString(11, app.creatorId)
            val resourcePatterns = app.resourcePatterns
            if (resourcePatterns == null) {
                stmt.bindNull(12)
            } else {
                stmt.bindString(12, ListConverter.fromList(resourcePatterns))
            }
            if (app.updatedAt == null) {
                stmt.bindNull(13)
            } else {
                stmt.bindString(13, app.updatedAt)
            }
            stmt.executeInsert()
            primaryId = app.appId
            assistanceId = null
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            stmt.close()
        }
    }

    private val depositEntryListConverter by lazy { DepositEntryListConverter() }
    private val withdrawalMemoPossibilityConverter by lazy { WithdrawalMemoPossibilityConverter() }

    fun insertIgnore(asset: Asset) {
        val stmt =
            writableDatabase.compileStatement("INSERT OR IGNORE INTO `assets` (`asset_id`, `symbol`, `name`, `icon_url`, `balance`, `destination`, `tag`, `price_btc`, `price_usd`, `chain_id`, `change_usd`, `change_btc`, `confirmations`, `asset_key`, `reserve`, `deposit_entries`, `withdrawal_memo_possibility`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        try {
            stmt.bindString(1, asset.assetId)
            stmt.bindString(2, asset.symbol)
            stmt.bindString(3, asset.name)
            stmt.bindString(4, asset.iconUrl)
            stmt.bindString(5, asset.balance)
            stmt.bindString(6, asset.destination)
            if (asset.tag == null) {
                stmt.bindNull(7)
            } else {
                stmt.bindString(7, asset.tag)
            }
            stmt.bindString(8, asset.priceBtc)
            stmt.bindString(9, asset.priceUsd)
            stmt.bindString(10, asset.chainId)
            stmt.bindString(11, asset.changeUsd)
            stmt.bindString(12, asset.changeBtc)
            stmt.bindLong(13, asset.confirmations.toLong())
            if (asset.assetKey == null) {
                stmt.bindNull(14)
            } else {
                stmt.bindString(14, asset.assetKey)
            }
            if (asset.reserve == null) {
                stmt.bindNull(15)
            } else {
                stmt.bindString(15, asset.reserve)
            }
            val depositEntries = asset.depositEntries
            if (depositEntries == null) {
                stmt.bindNull(16)
            } else {
                stmt.bindString(16, depositEntryListConverter.converterDate(depositEntries))
            }
            val withdrawalMemoPossibility = asset.withdrawalMemoPossibility
            if (depositEntries == null) {
                stmt.bindNull(17)
            } else {
                val withdrawalMemoPossibilityString = withdrawalMemoPossibilityConverter.converterDate(withdrawalMemoPossibility)
                if (withdrawalMemoPossibilityString == null) {
                    stmt.bindNull(17)
                } else {
                    stmt.bindString(17, withdrawalMemoPossibilityString)
                }
            }
            stmt.executeInsert()
            primaryId = asset.assetId
            assistanceId = null
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            stmt.close()
        }
    }

    fun insertIgnore(token: Token) {
        val stmt =
            writableDatabase.compileStatement("INSERT OR IGNORE INTO `tokens` (`asset_id`,`kernel_asset_id`,`symbol`,`name`,`icon_url`,`price_btc`,`price_usd`,`chain_id`,`change_usd`,`change_btc`,`confirmations`,`asset_key`,`dust`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)")
        try {
            stmt.bindString(1, token.assetId)
            stmt.bindString(2, token.asset)
            stmt.bindString(3, token.symbol)
            stmt.bindString(4, token.name)
            stmt.bindString(5, token.iconUrl)
            stmt.bindString(6, token.priceBtc)
            stmt.bindString(7, token.priceUsd)
            stmt.bindString(8, token.chainId)
            stmt.bindString(9, token.changeUsd)
            stmt.bindString(10, token.changeBtc)
            stmt.bindLong(11, token.confirmations.toLong())
            stmt.bindString(12, token.assetKey)
            stmt.bindString(13, token.dust)
            stmt.executeInsert()
            primaryId = token.assetId
            assistanceId = null
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            stmt.close()
        }
    }

    fun insertIgnore(snapshot: Snapshot) {
        val stmt =
            writableDatabase.compileStatement("INSERT OR IGNORE INTO `snapshots` (`snapshot_id`, `type`, `asset_id`, `amount`, `created_at`, `opponent_id`, `trace_id`, `transaction_hash`, `sender`, `receiver`, `memo`, `confirmations`, `snapshot_hash`, `opening_balance`, `closing_balance`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        try {
            stmt.bindString(1, snapshot.snapshotId)
            stmt.bindString(2, snapshot.type)
            stmt.bindString(3, snapshot.assetId)
            stmt.bindString(4, snapshot.amount)
            stmt.bindString(5, snapshot.createdAt)
            if (snapshot.opponentId == null) {
                stmt.bindNull(6)
            } else {
                stmt.bindString(6, snapshot.opponentId)
            }
            if (snapshot.traceId == null) {
                stmt.bindNull(7)
            } else {
                stmt.bindString(7, snapshot.traceId)
            }
            if (snapshot.transactionHash == null) {
                stmt.bindNull(8)
            } else {
                stmt.bindString(8, snapshot.transactionHash)
            }
            if (snapshot.sender == null) {
                stmt.bindNull(9)
            } else {
                stmt.bindString(9, snapshot.sender)
            }
            if (snapshot.receiver == null) {
                stmt.bindNull(10)
            } else {
                stmt.bindString(10, snapshot.receiver)
            }
            if (snapshot.memo == null) {
                stmt.bindNull(11)
            } else {
                stmt.bindString(11, snapshot.memo)
            }
            if (snapshot.confirmations == null) {
                stmt.bindNull(12)
            } else {
                stmt.bindLong(12, snapshot.confirmations.toLong())
            }
            if (snapshot.snapshotHash == null) {
                stmt.bindNull(13)
            } else {
                stmt.bindString(13, snapshot.snapshotHash)
            }
            if (snapshot.openingBalance == null) {
                stmt.bindNull(14)
            } else {
                stmt.bindString(14, snapshot.openingBalance)
            }
            if (snapshot.closingBalance == null) {
                stmt.bindNull(15)
            } else {
                stmt.bindString(15, snapshot.closingBalance)
            }
            stmt.executeInsert()
            primaryId = snapshot.snapshotId
            assistanceId = null
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            stmt.close()
        }
    }

    private val safeDepositConverter = SafeDepositConverter()

    private val safeWithdrawalConverter = SafeWithdrawalConverter()

    fun insertIgnore(safeSnapshot: SafeSnapshot) {
        val stmt =
            writableDatabase.compileStatement("INSERT OR IGNORE INTO `safe_snapshots` (`snapshot_id`,`type`,`asset_id`,`amount`,`user_id`,`opponent_id`,`memo`,`transaction_hash`,`created_at`,`trace_id`,`confirmations`,`opening_balance`,`closing_balance`,`deposit`,`withdrawal`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")
        try {
            stmt.bindString(1, safeSnapshot.snapshotId)
            stmt.bindString(2, safeSnapshot.type)
            stmt.bindString(3, safeSnapshot.assetId)
            stmt.bindString(4, safeSnapshot.amount)
            stmt.bindString(5, safeSnapshot.userId)
            stmt.bindString(6, safeSnapshot.opponentId)
            stmt.bindString(7, safeSnapshot.memo)
            stmt.bindString(8, safeSnapshot.transactionHash)
            stmt.bindString(9, safeSnapshot.createdAt)
            if (safeSnapshot.traceId == null) {
                stmt.bindNull(10)
            } else {
                stmt.bindString(10, safeSnapshot.traceId)
            }
            if (safeSnapshot.confirmations == null) {
                stmt.bindNull(11)
            } else {
                stmt.bindLong(11, safeSnapshot.confirmations.toLong())
            }
            if (safeSnapshot.openingBalance == null) {
                stmt.bindNull(12)
            } else {
                stmt.bindString(12, safeSnapshot.openingBalance)
            }
            if (safeSnapshot.closingBalance == null) {
                stmt.bindNull(13)
            } else {
                stmt.bindString(13, safeSnapshot.closingBalance)
            }
            val tmpSafeDeposit: String? = safeDepositConverter.converterData(safeSnapshot.deposit)
            if (tmpSafeDeposit == null) {
                stmt.bindNull(14)
            } else {
                stmt.bindString(14, tmpSafeDeposit)
            }
            val tmpWithdrawal: String? = safeWithdrawalConverter.converterData(safeSnapshot.withdrawal)
            if (tmpWithdrawal == null) {
                stmt.bindNull(15)
            } else {
                stmt.bindString(15, tmpWithdrawal)
            }
            stmt.executeInsert()
            primaryId = safeSnapshot.snapshotId
            assistanceId = null
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            stmt.close()
        }
    }

    fun insertIgnore(pinMessage: PinMessage) {
        val stmt =
            writableDatabase.compileStatement("INSERT OR IGNORE INTO `pin_messages` (`message_id`, `conversation_id`, `created_at`) VALUES (?, ?, ?)")
        try {
            stmt.bindString(1, pinMessage.messageId)
            stmt.bindString(2, pinMessage.conversationId)
            stmt.bindString(3, pinMessage.createdAt)
            stmt.executeInsert()
            primaryId = pinMessage.messageId
            assistanceId = null
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            stmt.close()
        }
    }

    fun insertIgnore(sticker: Sticker) {
        val stmt =
            writableDatabase.compileStatement("INSERT OR IGNORE INTO `stickers` (`sticker_id`, `album_id`, `name`, `asset_url`, `asset_type`, `asset_width`, `asset_height`, `created_at`, `last_use_at`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
        try {
            stmt.bindString(1, sticker.stickerId)
            if (sticker.albumId == null) {
                stmt.bindNull(2)
            } else {
                stmt.bindString(2, sticker.albumId)
            }
            stmt.bindString(3, sticker.name)
            stmt.bindString(4, sticker.assetUrl)
            stmt.bindString(5, sticker.assetType)
            stmt.bindLong(6, sticker.assetWidth.toLong())
            stmt.bindLong(7, sticker.assetHeight.toLong())
            stmt.bindString(8, sticker.createdAt)
            val lastUserAt = sticker.lastUseAt
            if (lastUserAt == null) {
                stmt.bindNull(9)
            } else {
                stmt.bindString(9, lastUserAt)
            }
            stmt.executeInsert()
            primaryId = sticker.stickerId
            assistanceId = null
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            stmt.close()
        }
    }

    fun insertIgnore(transcriptMessage: TranscriptMessage) {
        val stmt =
            writableDatabase.compileStatement("INSERT OR IGNORE INTO `transcript_messages` (`transcript_id`, `message_id`, `user_id`, `user_full_name`, `category`, `created_at`, `content`, `media_url`, `media_name`, `media_size`, `media_width`, `media_height`, `media_mime_type`, `media_duration`, `media_status`, `media_waveform`, `thumb_image`, `thumb_url`, `media_key`, `media_digest`, `media_created_at`, `sticker_id`, `shared_user_id`, `mentions`, `quote_id`, `quote_content`, `caption`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        try {
            stmt.bindString(1, transcriptMessage.transcriptId)
            stmt.bindString(2, transcriptMessage.messageId)
            if (transcriptMessage.userId == null) {
                stmt.bindNull(3)
            } else {
                stmt.bindString(3, transcriptMessage.userId)
            }
            if (transcriptMessage.userFullName == null) {
                stmt.bindNull(4)
            } else {
                stmt.bindString(4, transcriptMessage.userFullName)
            }
            stmt.bindString(5, transcriptMessage.type)
            stmt.bindString(6, transcriptMessage.createdAt)
            if (transcriptMessage.content == null) {
                stmt.bindNull(7)
            } else {
                stmt.bindString(7, transcriptMessage.content)
            }
            val mediaUrl = transcriptMessage.mediaUrl
            if (mediaUrl == null) {
                stmt.bindNull(8)
            } else {
                stmt.bindString(8, mediaUrl)
            }
            if (transcriptMessage.mediaName == null) {
                stmt.bindNull(9)
            } else {
                stmt.bindString(9, transcriptMessage.mediaName)
            }
            if (transcriptMessage.mediaSize == null) {
                stmt.bindNull(10)
            } else {
                stmt.bindLong(10, transcriptMessage.mediaSize)
            }
            if (transcriptMessage.mediaWidth == null) {
                stmt.bindNull(11)
            } else {
                stmt.bindLong(11, transcriptMessage.mediaWidth.toLong())
            }
            if (transcriptMessage.mediaHeight == null) {
                stmt.bindNull(12)
            } else {
                stmt.bindLong(12, transcriptMessage.mediaHeight.toLong())
            }
            if (transcriptMessage.mediaMimeType == null) {
                stmt.bindNull(13)
            } else {
                stmt.bindString(13, transcriptMessage.mediaMimeType)
            }
            if (transcriptMessage.mediaDuration == null) {
                stmt.bindNull(14)
            } else {
                stmt.bindLong(14, transcriptMessage.mediaDuration)
            }
            val mediaStatus = transcriptMessage.mediaStatus
            if (mediaStatus == null) {
                stmt.bindNull(15)
            } else {
                stmt.bindString(15, mediaStatus)
            }
            if (transcriptMessage.mediaWaveform == null) {
                stmt.bindNull(16)
            } else {
                stmt.bindBlob(16, transcriptMessage.mediaWaveform)
            }
            if (transcriptMessage.thumbImage == null) {
                stmt.bindNull(17)
            } else {
                stmt.bindString(17, transcriptMessage.thumbImage)
            }
            if (transcriptMessage.thumbUrl == null) {
                stmt.bindNull(18)
            } else {
                stmt.bindString(18, transcriptMessage.thumbUrl)
            }
            if (transcriptMessage.mediaKey == null) {
                stmt.bindNull(19)
            } else {
                stmt.bindBlob(19, transcriptMessage.mediaKey)
            }
            if (transcriptMessage.mediaDigest == null) {
                stmt.bindNull(20)
            } else {
                stmt.bindBlob(20, transcriptMessage.mediaDigest)
            }
            if (transcriptMessage.mediaCreatedAt == null) {
                stmt.bindNull(21)
            } else {
                stmt.bindString(21, transcriptMessage.mediaCreatedAt)
            }
            if (transcriptMessage.stickerId == null) {
                stmt.bindNull(22)
            } else {
                stmt.bindString(22, transcriptMessage.stickerId)
            }
            if (transcriptMessage.sharedUserId == null) {
                stmt.bindNull(23)
            } else {
                stmt.bindString(23, transcriptMessage.sharedUserId)
            }
            if (transcriptMessage.mentions == null) {
                stmt.bindNull(24)
            } else {
                stmt.bindString(24, transcriptMessage.mentions)
            }
            if (transcriptMessage.quoteId == null) {
                stmt.bindNull(25)
            } else {
                stmt.bindString(25, transcriptMessage.quoteId)
            }
            val quoteContent = transcriptMessage.quoteContent
            if (quoteContent == null) {
                stmt.bindNull(26)
            } else {
                stmt.bindString(26, quoteContent)
            }
            if (transcriptMessage.caption == null) {
                stmt.bindNull(27)
            } else {
                stmt.bindString(27, transcriptMessage.caption)
            }
            stmt.executeInsert()
            primaryId = transcriptMessage.transcriptId
            assistanceId = transcriptMessage.messageId
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            stmt.close()
        }
    }

    fun insertIgnore(messageMention: MessageMention) {
        val stmt =
            writableDatabase.compileStatement("INSERT OR IGNORE INTO `message_mentions` (`message_id`, `conversation_id`, `mentions`, `has_read`) VALUES (?, ?, ?, ?)")
        try {
            stmt.bindString(1, messageMention.messageId)
            stmt.bindString(2, messageMention.conversationId)
            stmt.bindString(3, messageMention.mentions)
            val hasRead = if (messageMention.hasRead) 1 else 0
            stmt.bindLong(4, hasRead.toLong())
            stmt.executeInsert()
            primaryId = messageMention.messageId
            assistanceId = null
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            stmt.close()
        }
    }

    fun insertIgnore(expiredMessage: ExpiredMessage) {
        val stmt =
            writableDatabase.compileStatement("INSERT OR IGNORE INTO `expired_messages` (`message_id`, `expire_in`, `expire_at`) VALUES (?, ?, ?)")
        try {
            stmt.bindString(1, expiredMessage.messageId)
            stmt.bindLong(2, expiredMessage.expireIn)
            if (expiredMessage.expireAt == null) {
                stmt.bindNull(3)
            } else {
                stmt.bindLong(3, expiredMessage.expireAt)
            }
            stmt.executeInsert()
            primaryId = expiredMessage.messageId
            assistanceId = null
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            stmt.close()
        }
    }
}
