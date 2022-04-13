package one.mixin.android.ui.setting

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import ir.mirrajabi.rxcontacts.Contact
import ir.mirrajabi.rxcontacts.RxContacts
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Account.PREF_DELETE_MOBILE_CONTACTS
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.createContactsRequests
import one.mixin.android.databinding.FragmentSettingMobileContactBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.textColorResource
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class MobileContactFragment : BaseFragment(R.layout.fragment_setting_mobile_contact) {
    companion object {
        const val TAG = "MobileContactFragment"
        fun newInstance() = MobileContactFragment()
    }

    private val viewModel by viewModels<SettingViewModel>()
    private val binding by viewBinding(FragmentSettingMobileContactBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener { activity?.onBackPressed() }

        lifecycleScope.launch {
            if (viewDestroyed()) return@launch

            binding.opPb.isVisible = true
            handleMixinResponse(
                invokeNetwork = { viewModel.getContacts() },
                successBlock = { response ->
                    if (response.data.isNullOrEmpty()) {
                        setUpdate()
                    } else {
                        setDelete()
                    }
                },
                failureBlock = {
                    setUpdate()
                    return@handleMixinResponse false
                },
                exceptionBlock = {
                    hidePb()
                    setUpdate()
                    return@handleMixinResponse false
                },
                doAfterNetworkSuccess = {
                    hidePb()
                }
            )
        }
    }

    private fun hidePb() {
        if (viewDestroyed()) return
        binding.opPb.isVisible = false
    }

    private fun setDelete() {
        if (viewDestroyed()) return
        binding.apply {
            opTv.setText(R.string.setting_mobile_contact_delete)
            opTv.textColorResource = R.color.colorRed
            opRl.setOnClickListener {
                alertDialogBuilder()
                    .setMessage(R.string.setting_mobile_contact_warning)
                    .setPositiveButton(R.string.action_delete) { dialog, _ ->
                        deleteContacts()
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.action_cancel) { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }

    private fun setUpdate() {
        if (viewDestroyed()) return
        binding.apply {
            opTv.setText(R.string.setting_mobile_contact_upload)
            opTv.textColorResource = R.color.colorDarkBlue
            opRl.setOnClickListener {
                RxPermissions(requireActivity())
                    .request(Manifest.permission.READ_CONTACTS)
                    .autoDispose(stopScope)
                    .subscribe { granted ->
                        if (granted) {
                            opPb.isVisible = true
                            opRl.isEnabled = false
                            RxContacts.fetch(requireContext())
                                .toSortedList(Contact::compareTo)
                                .autoDispose(stopScope)
                                .subscribe(
                                    { contacts ->
                                        opRl.isEnabled = true
                                        updateContacts(contacts)
                                    },
                                    {
                                        if (!viewDestroyed()) {
                                            binding.opPb.isVisible = false
                                            opRl.isEnabled = true
                                        }
                                    }
                                )
                        } else {
                            context?.openPermissionSetting()
                        }
                    }
            }
        }
    }

    private fun deleteContacts() = lifecycleScope.launch {
        if (viewDestroyed()) return@launch

        binding.opPb.isVisible = true
        handleMixinResponse(
            invokeNetwork = { viewModel.deleteContacts() },
            successBlock = {
                defaultSharedPreferences.putBoolean(PREF_DELETE_MOBILE_CONTACTS, true)
                setUpdate()
            },
            exceptionBlock = {
                binding.opPb.isVisible = false
                return@handleMixinResponse false
            },
            doAfterNetworkSuccess = {
                binding.opPb.isVisible = false
            }
        )
    }

    private fun updateContacts(contacts: List<Contact>) = lifecycleScope.launch {
        if (viewDestroyed()) return@launch

        binding.opPb.isVisible = true
        val mutableList = createContactsRequests(contacts)
        if (mutableList.isEmpty()) {
            binding.opPb.isVisible = false
            toast(R.string.setting_mobile_contact_empty)
            return@launch
        }
        handleMixinResponse(
            invokeNetwork = { viewModel.syncContacts(mutableList) },
            successBlock = {
                defaultSharedPreferences.putBoolean(PREF_DELETE_MOBILE_CONTACTS, false)
                setDelete()
            },
            exceptionBlock = {
                binding.opPb.isVisible = false
                return@handleMixinResponse false
            },
            doAfterNetworkSuccess = {
                binding.opPb.isVisible = false
            }
        )
    }
}
