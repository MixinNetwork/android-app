package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentBackupMnemonicPhraseWarningBottomBinding
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.setting.SettingActivity
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class BackupMnemonicPhraseWarningBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "BackupMnemonicPhraseWarningBottomSheetDialogFragment"

        fun newInstance() =
            BackupMnemonicPhraseWarningBottomSheetDialogFragment().withArgs {

            }
    }

    private val binding by viewBinding(FragmentBackupMnemonicPhraseWarningBottomBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }
        binding.warning.text = getString(R.string.Backup_Mnemonic_Phrase_Warning, Session.getAccount()?.identityNumber ?: "")
        binding.backupNow.setOnClickListener {
            SettingActivity.showMnemonicPhrase(requireContext())
            dismissNow()
        }
        if (laterCallback == null) {
            binding.later.text = getString(R.string.Cancel)
            binding.later.setOnClickListener {
                dismissNow()
            }
        } else {
            binding.later.text = getString(R.string.Later)
            binding.later.setOnClickListener {
                laterCallback?.invoke()
                dismissNow()
            }
        }
    }

    var laterCallback: (() -> Unit)? = null
}