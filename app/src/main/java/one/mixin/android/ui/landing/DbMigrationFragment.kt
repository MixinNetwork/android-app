package one.mixin.android.ui.landing

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.DataBase.DB_NAME
import one.mixin.android.Constants.DataBase.FTS_DB_NAME
import one.mixin.android.Constants.DataBase.PENDING_DB_NAME
import one.mixin.android.R
import one.mixin.android.databinding.FragmentUpgradeBinding
import one.mixin.android.extension.moveTo
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.viewBinding
import java.io.File

@AndroidEntryPoint
class DbMigrationFragment : BaseFragment(R.layout.fragment_upgrade) {

    companion object {
        const val TAG: String = "DbMigrationFragment"

        fun newInstance() = DbMigrationFragment()
    }

    private val binding by viewBinding(FragmentUpgradeBinding::bind)

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        lifecycleScope.launch {
            binding.pb.isIndeterminate = true
            withContext(Dispatchers.IO) {
                val context = requireContext()
                val identityNumber = Session.getAccount()?.identityNumber ?: return@withContext
                val dbDir = context.getDatabasePath(DB_NAME).parentFile
                val toDir = File(dbDir, identityNumber)
                if (!toDir.exists()) {
                    toDir.mkdirs()
                }
                dbDir.listFiles().forEach { file ->
                    if (file.name.startsWith(DB_NAME) || file.name.startsWith(FTS_DB_NAME) || file.name.startsWith(PENDING_DB_NAME)) {
                        file.moveTo(File(toDir, file.name))
                    }
                }
            }
            MainActivity.show(requireContext())
            activity?.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
