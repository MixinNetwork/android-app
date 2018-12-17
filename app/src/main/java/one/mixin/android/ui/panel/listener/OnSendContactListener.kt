package one.mixin.android.ui.panel.listener

import one.mixin.android.vo.ForwardMessage

interface OnSendContactsListener {
    fun onSendContacts(messages: ArrayList<ForwardMessage>)
}