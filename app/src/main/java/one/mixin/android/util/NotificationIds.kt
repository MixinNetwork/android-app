package one.mixin.android.util

import android.app.NotificationManager

private const val SUMMARY_NOTIFICATION_ID_MASK = 0x40000000
private const val BUBBLE_SHORTCUT_ID_PREFIX = "Bubble-"

fun conversationNotificationId(conversationId: String): Int = conversationId.hashCode()

fun conversationSummaryNotificationId(conversationId: String): Int =
    conversationNotificationId(conversationId) xor SUMMARY_NOTIFICATION_ID_MASK

fun bubbleShortcutId(conversationId: String): String = "$BUBBLE_SHORTCUT_ID_PREFIX$conversationId"

fun NotificationManager.cancelConversationNotifications(conversationId: String) {
    cancel(conversationNotificationId(conversationId))
    cancel(conversationSummaryNotificationId(conversationId))
}
