package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.fragment_storage.title_view
import kotlinx.android.synthetic.main.fragment_storage_data.*
import kotlinx.android.synthetic.main.view_stotage_data.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.ui.common.BaseFragment

class SettingDataStorageFragment : BaseFragment() {
    companion object {
        const val TAG = "SettingDataStorageFragment"

        fun newInstance(): SettingDataStorageFragment {
            return SettingDataStorageFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_storage_data, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        storage_rl.setOnClickListener {
            requireActivity().addFragment(this@SettingDataStorageFragment,
                SettingStorageFragment.newInstance(), SettingStorageFragment.TAG)
        }
        storage_mobile.setOnClickListener { showMenu(R.string.setting_data_mobile) }
        storage_wifi.setOnClickListener { showMenu(R.string.setting_data_wifi) }
        storage_roaming.setOnClickListener { showMenu(R.string.setting_data_roaming) }
    }

    private var menuDialog: AlertDialog? = null
    private fun showMenu(@StringRes titleId: Int) {
        menuDialog?.dismiss()
        val menuView = View.inflate(requireContext(), R.layout.view_stotage_data, null).apply {
            this.check_photo.setName(R.string.setting_data_photo)
            this.check_audio.setName(R.string.setting_data_audio)
            this.check_video.setName(R.string.setting_data_video)
            this.check_document.setName(R.string.setting_data_documents)
        }
        menuDialog = AlertDialog.Builder(requireContext(), R.style.MixinAlertDialogTheme)
            .setTitle(titleId)
            .setView(menuView)
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }.create().apply {
                this.window?.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
            }
        menuDialog?.show()
    }


}