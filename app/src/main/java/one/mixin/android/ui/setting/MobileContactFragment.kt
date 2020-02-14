package one.mixin.android.ui.setting

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import ir.mirrajabi.rxcontacts.Contact
import ir.mirrajabi.rxcontacts.RxContacts
import kotlinx.android.synthetic.main.fragment_setting_mobile_contact.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Account.PREF_DELETE_MOBILE_CONTACTS
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.createContactsRequests
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseViewModelFragment
import org.jetbrains.anko.textColorResource

class MobileContactFragment : BaseViewModelFragment<SettingViewModel>() {
    companion object {
        const val TAG = "MobileContactFragment"
        fun newInstance() = MobileContactFragment()
    }

    override fun getModelClass() = SettingViewModel::class.java

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_setting_mobile_contact, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }

        lifecycleScope.launch {
            op_pb?.isVisible = true
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
                    op_pb?.isVisible = false
                    setUpdate()
                    return@handleMixinResponse false
                },
                doAfterNetworkSuccess = {
                    op_pb?.isVisible = false
                }
            )
        }
    }

    private fun setDelete() {
        if (!isAdded) return
        op_tv.setText(R.string.setting_mobile_contact_delete)
        op_tv.textColorResource = R.color.colorRed
        op_rl.setOnClickListener {
            alertDialogBuilder()
                .setMessage(R.string.setting_mobile_contact_warning)
                .setPositiveButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .setNegativeButton(R.string.conversation_delete) { dialog, _ ->
                    deleteContacts()
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun setUpdate() {
        if (!isAdded) return
        op_tv.setText(R.string.setting_mobile_contact_upload)
        op_tv.textColorResource = R.color.colorDarkBlue
        op_rl.setOnClickListener {
            RxPermissions(requireActivity())
                .request(Manifest.permission.READ_CONTACTS)
                .autoDispose(stopScope)
                .subscribe { granted ->
                    if (granted) {
                        RxContacts.fetch(requireContext())
                            .toSortedList(Contact::compareTo)
                            .autoDispose(stopScope)
                            .subscribe({ contacts ->
                                updateContacts(contacts)
                            }, {
                            })
                    } else {
                        context?.openPermissionSetting()
                    }
                }
        }
    }

    private fun deleteContacts() = lifecycleScope.launch {
        op_pb.isVisible = true
        handleMixinResponse(
            invokeNetwork = { viewModel.deleteContacts() },
            switchContext = Dispatchers.IO,
            successBlock = {
                defaultSharedPreferences.putBoolean(PREF_DELETE_MOBILE_CONTACTS, true)
                setUpdate()
            },
            exceptionBlock = {
                op_pb?.isVisible = false
                return@handleMixinResponse false
            },
            doAfterNetworkSuccess = {
                op_pb?.isVisible = false
            }
        )
    }

    private fun updateContacts(contacts: List<Contact>) = lifecycleScope.launch {
        op_pb.isVisible = true
        val mutableList = createContactsRequests(contacts)
        if (!isAdded) return@launch
        if (mutableList.isEmpty()) {
            op_pb.isVisible = false
            toast(R.string.setting_mobile_contact_empty)
            return@launch
        }
        handleMixinResponse(
            invokeNetwork = { viewModel.syncContacts(mutableList) },
            switchContext = Dispatchers.IO,
            successBlock = {
                defaultSharedPreferences.putBoolean(PREF_DELETE_MOBILE_CONTACTS, false)
                setDelete()
            },
            exceptionBlock = {
                op_pb?.isVisible = false
                return@handleMixinResponse false
            },
            doAfterNetworkSuccess = {
                op_pb?.isVisible = false
            }
        )
    }
}
