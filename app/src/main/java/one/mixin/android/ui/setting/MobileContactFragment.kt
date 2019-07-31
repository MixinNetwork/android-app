package one.mixin.android.ui.setting

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDisposable
import ir.mirrajabi.rxcontacts.Contact
import ir.mirrajabi.rxcontacts.RxContacts
import kotlinx.android.synthetic.main.fragment_setting_mobile_contact.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.createContactsRequests
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseViewModelFragment
import org.jetbrains.anko.textColorResource

class MobileContactFragment : BaseViewModelFragment<SettingViewModel>() {
    companion object {
        const val TAG = "MobileContactFragment"
        fun newInstance() = MobileContactFragment()
    }

    init {
        lifecycleScope.launchWhenCreated {
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
                    setUpdate()
                    return@handleMixinResponse false
                }
            )
        }
    }

    override fun getModelClass() = SettingViewModel::class.java

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_setting_mobile_contact, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
    }

    private fun setDelete() {
        if (!isAdded) return
        op_tv.setText(R.string.setting_mobile_contact_delete)
        op_tv.textColorResource = R.color.colorRed
        op_tv.setOnClickListener {
            deleteContacts()
        }
    }

    private fun setUpdate() {
        if (!isAdded) return
        op_tv.setText(R.string.setting_mobile_contact_update)
        op_tv.textColorResource = R.color.colorDarkBlue
        op_tv.setOnClickListener {
            RxPermissions(requireActivity())
                .request(Manifest.permission.READ_CONTACTS)
                .autoDisposable(stopScope)
                .subscribe { granted ->
                    if (granted) {
                        RxContacts.fetch(requireContext())
                            .toSortedList(Contact::compareTo)
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
        handleMixinResponse(
            invokeNetwork = { viewModel.deleteContacts() },
            switchContext = Dispatchers.IO,
            successBlock = { setUpdate() }
        )
    }

    private fun updateContacts(contacts: List<Contact>) = lifecycleScope.launch {
        val mutableList = createContactsRequests(contacts)
        if (!isAdded) return@launch
        if (mutableList.isEmpty()) {
            toast(R.string.setting_mobile_contact_empty)
            return@launch
        }
        handleMixinResponse(
            invokeNetwork = { viewModel.syncContacts(mutableList) },
            switchContext = Dispatchers.IO,
            successBlock = { setDelete() }
        )
    }
}
