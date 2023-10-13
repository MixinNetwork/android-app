@file:Suppress("ktlint:standard:enum-entry-name-case", "EnumEntryName")

package one.mixin.android.repository

enum class QrCodeType {
    user, conversation, authorization, multisig_request, non_fungible_request, payment
}
