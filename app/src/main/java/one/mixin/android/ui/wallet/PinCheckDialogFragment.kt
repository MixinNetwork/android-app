package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatDialogFragment
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_pin_check.view.*
import one.mixin.android.Constants.KEYS
import one.mixin.android.R
import one.mixin.android.di.Injectable
import one.mixin.android.extension.displaySize
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.vibrate
import one.mixin.android.util.ErrorHandler
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView
import javax.inject.Inject

class PinCheckDialogFragment : AppCompatDialogFragment(), Injectable {

    companion object {
        const val TAG = "PinCheckDialogFragment"
        const val POS_PIN = 0
        const val POS_PB = 1
        const val POS_TIP = 2

        fun newInstance() = PinCheckDialogFragment()
    }

    private lateinit var contentView: View

    private val scopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val pinCheckViewModel: PinCheckViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(PinCheckViewModel::class.java)
    }

    private val disposable = CompositeDisposable()

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_pin_check, null)
        dialog.setContentView(contentView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.pin.setListener(object : PinView.OnPinListener {
            override fun onUpdate(index: Int) {
                if (index != contentView.pin.getCount()) return
                contentView.pin_va?.displayedChild = POS_PB
                pinCheckViewModel.verifyPin(contentView.pin.code()).autoDisposable(scopeProvider).subscribe({ r ->
                    contentView.pin_va?.displayedChild = POS_PIN
                    if (r.isSuccess) {
                        context?.updatePinCheck()
                        dismiss()
                    } else {
                        if (r.errorCode == ErrorHandler.PIN_INCORRECT) {
                            contentView.pin.error(getString(R.string.error_pin_incorrect))
                        } else if (r.errorCode == ErrorHandler.TOO_MANY_REQUEST) {
                            contentView.pin_va?.displayedChild = POS_TIP
                            contentView.tip_va?.showNext()
                            val transY = contentView.height / 2 - contentView.top_ll.translationY * 2
                            contentView.top_ll.animate().translationY(transY).start()
                            contentView.keyboard.animate().translationY(contentView.keyboard.height.toFloat()).start()
                        }
                        ErrorHandler.handleMixinError(r.errorCode)
                    }
                }, { t ->
                    contentView.pin_va?.displayedChild = POS_PIN
                    ErrorHandler.handleError(t)
                })
            }
        })
        contentView.got_it_tv.setOnClickListener { activity?.finish() }
        contentView.keyboard.setKeyboardKeys(KEYS)
        contentView.keyboard.setOnClickKeyboardListener(mKeyboardListener)
        contentView.keyboard.animate().translationY(0f).start()
    }

    override fun onStart() {
        super.onStart()
        val displaySize = context!!.displaySize()
        dialog.window.setLayout(displaySize.x, MATCH_PARENT)
        dialog.window.setBackgroundDrawableResource(R.drawable.bg_transparent_dialog)
        dialog.window.setGravity(Gravity.BOTTOM)

        // reset top layout position
        contentView.post {
            val keyboardHeight = contentView.keyboard.height
            val h = contentView.height
            val topHeight = contentView.top_ll.height
            val margin = (h - keyboardHeight - topHeight) / 2
            if (margin > 0) {
                contentView.top_ll.translationY = margin.toFloat()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dialog.setOnKeyListener { _, i, _ ->
            if (i == KeyEvent.KEYCODE_BACK) {
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposable.dispose()
    }

    private val mKeyboardListener: Keyboard.OnClickKeyboardListener = object : Keyboard.OnClickKeyboardListener {
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
    }
}
