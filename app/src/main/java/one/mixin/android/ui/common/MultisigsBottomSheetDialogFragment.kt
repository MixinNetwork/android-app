package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import java.math.BigDecimal
import kotlinx.android.synthetic.main.fragment_multisigs_bottom_sheet.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_round_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.BIOMETRIC_PIN_CHECK
import one.mixin.android.Constants.KEYS
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.response.MultisigsAction
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.putLong
import one.mixin.android.extension.toast
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.vibrate
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.MultisigsBiometricItem
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.User
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView

class MultisigsBottomSheetDialogFragment : BiometricBottomSheetDialogFragment() {
    companion object {
        const val TAG = "MultisigsBottomSheetDialogFragment"

        inline fun <reified T : BiometricItem> newInstance(t: T) =
            MultisigsBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_BIOMETRIC_ITEM, t)
            }
    }

    private val t: MultisigsBiometricItem by lazy {
        arguments!!.getParcelable<MultisigsBiometricItem>(ARGS_BIOMETRIC_ITEM)!!
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_multisigs_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.title_view.right_iv.setOnClickListener { dismiss() }
        if (t.action == MultisigsAction.cancel.name) {
            contentView.title.text = getString(R.string.multisig_revoke_transaction)
            contentView.arrow_iv.setImageResource(R.drawable.ic_multisigs_arrow_ban)
        } else {
            contentView.title.text = getString(R.string.multisig_transaction)
            contentView.arrow_iv.setImageResource(R.drawable.ic_multisigs_arrow_right)
        }
        contentView.sub_title.text = t.memo

        contentView.asset_icon.bg.loadImage(t.asset.iconUrl, R.drawable.ic_avatar_place_holder)
        contentView.asset_icon.badge.loadImage(t.asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        contentView.balance.text = t.amount.numberFormat() + " " + t.asset.symbol
        contentView.balance_as.text = "≈ ${(BigDecimal(t.amount) *
            t.asset.priceFiat()).numberFormat2()} ${Fiats.currency}"

        lifecycleScope.launch {
            val users = withContext(Dispatchers.IO) {
                bottomViewModel.findMultiUsers(t.senders, t.receivers)
            }
            if (users.isNotEmpty()) {
                val senders = arrayListOf<User>()
                val receivers = arrayListOf<User>()
                users.forEach { u ->
                    if (u.userId in t.senders) {
                        senders.add(u)
                    }
                    if (u.userId in t.receivers) {
                        receivers.add(u)
                    }
                }
                contentView.senders_view.addUserList(senders)
                contentView.receivers_view.addUserList(receivers)

                contentView.senders_view.setOnClickListener {
                    showUserList(senders, true)
                }
                contentView.receivers_view.setOnClickListener {
                    showUserList(receivers, false)
                }
            }
        }
        contentView.pin.setListener(object : PinView.OnPinListener {
            override fun onUpdate(index: Int) {
                if (index == contentView.pin.getCount()) {
                    onPinCorrect(contentView.pin.code())
                }
            }
        })
        initKeyboard()
    }

    private fun showUserList(userList: ArrayList<User>, isSender: Boolean) {
        val title = getString(if (isSender) R.string.multisig_senders else R.string.multisig_receivers)
        UserListBottomSheetDialogFragment.newInstance(userList, title)
            .showNow(parentFragmentManager, UserListBottomSheetDialogFragment.TAG)
    }

    private fun initKeyboard() {
        contentView.keyboard.setKeyboardKeys(KEYS)
        contentView.keyboard.setOnClickKeyboardListener(object : Keyboard.OnClickKeyboardListener {
            override fun onKeyClick(position: Int, value: String) {
                context?.vibrate(longArrayOf(0, 30))
                if (position == 11) {
                    contentView.pin.delete()
                } else {
                    contentView.pin.append(value)
                }
            }

            override fun onLongClick(position: Int, value: String) {
                context?.vibrate(longArrayOf(0, 30))
                if (position == 11) {
                    contentView.pin.clear()
                } else {
                    contentView.pin.append(value)
                }
            }
        })
    }

    override fun onPinCorrect(pin: String) {
        lifecycleScope.launch {
            if (!isAdded) return@launch

            contentView.pin_va?.displayedChild = POS_PB
            handleMixinResponse(
                invokeNetwork = {
                    when {
                        t.action == MultisigsAction.sign.name -> {
                            bottomViewModel.signMultisigs(t.requestId, pin)
                        }
                        else -> {
                            bottomViewModel.unlockMultisigs(t.requestId, pin)
                        }
                    }
                },
                switchContext = Dispatchers.IO,
                successBlock = {
                    defaultSharedPreferences.putLong(
                        BIOMETRIC_PIN_CHECK,
                        System.currentTimeMillis()
                    )
                    context?.updatePinCheck()

                    dismiss()
                    callback.notNullWithElse({ action -> action.onSuccess() }, {
                        toast(R.string.successful)
                    })
                },
                doAfterNetworkSuccess = {
                    contentView.pin_va?.displayedChild = POS_PIN
                },
                failureBlock = {
                    contentView.pin?.clear()
                    if (it.errorCode == ErrorHandler.TOO_MANY_REQUEST) {
                        toast(R.string.error_pin_check_too_many_request)
                    } else {
                        ErrorHandler.handleMixinError(it.errorCode, it.errorDescription)
                    }
                    return@handleMixinResponse true
                },
                exceptionBlock = {
                    contentView.pin?.clear()
                    contentView.pin_va?.displayedChild = POS_PIN
                    return@handleMixinResponse false
                }
            )
        }
    }
}
