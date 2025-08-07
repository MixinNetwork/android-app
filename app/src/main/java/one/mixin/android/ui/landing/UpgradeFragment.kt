package one.mixin.android.ui.landing

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.room.util.readVersion
import androidx.work.impl.WorkDatabasePathHelper.getDatabasePath
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.Constants.DataBase.CURRENT_VERSION
import one.mixin.android.Constants.DataBase.DB_NAME
import one.mixin.android.Constants.DataBase.MINI_VERSION
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.FragmentUpgradeBinding
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.WalletDatabase
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.util.database.dbDir
import one.mixin.android.util.viewBinding
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class UpgradeFragment : BaseFragment(R.layout.fragment_upgrade) {
    companion object {
        const val TAG: String = "UpgradeFragment"

        const val ARGS_TYPE = "args_type"
        const val TYPE_DB = 0

        fun newInstance(type: Int) =
            UpgradeFragment().withArgs {
                putInt(ARGS_TYPE, type)
            }
    }

    private val viewModel by viewModels<WalletViewModel>()
    private val binding by viewBinding(FragmentUpgradeBinding::bind)

    private val type: Int by lazy { requireArguments().getInt(ARGS_TYPE) }

    @Inject
    lateinit var db: MixinDatabase

    @Inject
    lateinit var walletDatabase: WalletDatabase

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        MixinApplication.get().isOnline.set(true)

        if (checkWalletUpdate()) {
            lifecycleScope.launch {
                binding.pb.isIndeterminate = true
                withContext(Dispatchers.IO) {
                    val classicWalletId = viewModel.getClassicWalletId()
                    if (classicWalletId != null) {
                        var success = false
                        val name = getString(R.string.Common_Wallet)
                        while (success.not()) {
                            success = viewModel.renameWallet(classicWalletId, name)
                            if (success.not()) delay(2000)
                        }
                        walletDatabase.runInTransaction {  }
                    }
                }
                MainActivity.show(requireContext())
                activity?.finish()
            }
        }
        if (checkNeedGo2MigrationPage()) {
            lifecycleScope.launch {
                binding.pb.isIndeterminate = true
                withContext(Dispatchers.IO) {
                    PropertyHelper.checkMigrated()
                    db.runInTransaction {  }
                }
                MainActivity.show(requireContext())
                activity?.finish()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @SuppressLint("RestrictedApi")
    private fun checkNeedGo2MigrationPage(): Boolean {
        val currentVersion =
            try {
                readVersion(requireContext().getDatabasePath(DB_NAME))
            } catch (e: Exception) {
                0
            }
        return currentVersion > MINI_VERSION && CURRENT_VERSION != currentVersion
    }

    @SuppressLint("RestrictedApi")
    private fun checkWalletUpdate(): Boolean {
        val currentVersion =
            try {
                val dir = dbDir(requireContext())
                File(dir, Constants.DataBase.WEB3_DB_NAME)
                readVersion(File(dir, Constants.DataBase.WEB3_DB_NAME))
            } catch (e: Exception) {
                0
            }
        return currentVersion <= 4
    }
}
