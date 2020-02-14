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
import one.mixin.android.Constants.Download.AUTO_DOWNLOAD_DOCUMENT
import one.mixin.android.Constants.Download.AUTO_DOWNLOAD_MOBILE
import one.mixin.android.Constants.Download.AUTO_DOWNLOAD_PHOTO
import one.mixin.android.Constants.Download.AUTO_DOWNLOAD_ROAMING
import one.mixin.android.Constants.Download.AUTO_DOWNLOAD_VIDEO
import one.mixin.android.Constants.Download.AUTO_DOWNLOAD_WIFI
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.autoDownloadDocument
import one.mixin.android.extension.autoDownloadPhoto
import one.mixin.android.extension.autoDownloadVideo
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getAutoDownloadMobileValue
import one.mixin.android.extension.getAutoDownloadRoamingValue
import one.mixin.android.extension.getAutoDownloadWifiValue
import one.mixin.android.extension.putInt
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
        storage_mobile.setOnClickListener { showMenu(AUTO_DOWNLOAD_MOBILE, requireContext().getAutoDownloadMobileValue(), R.string.setting_data_mobile) }
        storage_wifi.setOnClickListener {
            showMenu(AUTO_DOWNLOAD_WIFI, requireContext().getAutoDownloadWifiValue(), R.string
                .setting_data_wifi)
        }
        storage_roaming.setOnClickListener {
            showMenu(AUTO_DOWNLOAD_ROAMING, requireContext().getAutoDownloadRoamingValue(), R.string
                .setting_data_roaming)
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        storage_mobile_info.text = getInfo(requireContext().getAutoDownloadMobileValue())
        storage_wifi_info.text = getInfo(requireContext().getAutoDownloadWifiValue())
        storage_roaming_info.text = getInfo(requireContext().getAutoDownloadRoamingValue())
    }

    private fun getInfo(value: Int): String {
        val list = mutableListOf<String>()
        if (autoDownloadPhoto(value)) list.add(getString(R.string.setting_data_photo))
        if (autoDownloadVideo(value)) list.add(getString(R.string.setting_data_video))
        if (autoDownloadDocument(value)) list.add(getString(R.string.setting_data_documents))
        val divide = getString(R.string.divide)
        if (list.isEmpty()) return getString(R.string.setting_data_noting)
        val str = StringBuffer()
        list.forEachIndexed { index, s ->
            if (index != 0) {
                str.append(divide)
            }
            str.append(s)
        }
        return str.toString()
    }

    private var menuDialog: AlertDialog? = null
    private fun showMenu(key: String, value: Int, @StringRes titleId: Int) {
        menuDialog?.dismiss()
        val menuView = View.inflate(requireContext(), R.layout.view_stotage_data, null).apply {
            this.check_photo.apply {
                setName(R.string.setting_data_photo)
                isChecked = autoDownloadPhoto(value)
            }
            this.check_video.apply {
                setName(R.string.setting_data_video)
                isChecked = autoDownloadVideo(value)
            }
            this.check_document.apply {
                setName(R.string.setting_data_documents)
                isChecked = autoDownloadDocument(value)
            }
        }
        menuDialog = alertDialogBuilder()
            .setTitle(titleId)
            .setView(menuView)
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                var value = 0
                if (menuView.check_photo.isChecked) {
                    value += (AUTO_DOWNLOAD_PHOTO)
                }
                if (menuView.check_video.isChecked) {
                    value += (AUTO_DOWNLOAD_VIDEO)
                }
                if (menuView.check_document.isChecked) {
                    value += (AUTO_DOWNLOAD_DOCUMENT)
                }
                defaultSharedPreferences.putInt(key, value)
                refresh()
                dialog.dismiss()
            }.create().apply {
                this.window?.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
            }
        menuDialog?.show()
    }
}
