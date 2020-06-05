package one.mixin.android.ui.contacts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkManager
import com.tbruyelle.rxpermissions2.RxPermissions
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import com.uber.autodispose.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import ir.mirrajabi.rxcontacts.Contact
import ir.mirrajabi.rxcontacts.RxContacts
import kotlinx.android.synthetic.main.fragment_contacts.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.Constants.Account.PREF_DELETE_MOBILE_CONTACTS
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.enqueueOneTimeNetworkWorkRequest
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.UploadContactsJob
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.QrBottomSheetDialogFragment
import one.mixin.android.ui.common.QrBottomSheetDialogFragment.Companion.TYPE_MY_QR
import one.mixin.android.ui.common.QrBottomSheetDialogFragment.Companion.TYPE_RECEIVE_QR
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.common.profile.ProfileBottomSheetDialogFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.group.GroupActivity
import one.mixin.android.ui.setting.SettingActivity
import one.mixin.android.vo.User
import one.mixin.android.vo.UserRelationship
import one.mixin.android.worker.RefreshContactWorker
import java.util.Collections
import javax.inject.Inject

class ContactsFragment : BaseFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var jobManager: MixinJobManager

    private val contactsViewModel: ContactViewModel by viewModels { viewModelFactory }

    private val contactAdapter: ContactsAdapter by lazy {
        ContactsAdapter(requireContext(), Collections.emptyList(), 0)
    }

    companion object {
        const val TAG = "ContactsFragment"

        fun newInstance() = ContactsFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_contacts, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contact_recycler_view.adapter = contactAdapter
        contact_recycler_view.setHasFixedSize(true)
        contact_recycler_view.layoutManager = LinearLayoutManager(context)
        contact_recycler_view.addItemDecoration(StickyRecyclerHeadersDecoration(contactAdapter))
        val header = LayoutInflater.from(context).inflate(R.layout.view_contact_header, contact_recycler_view, false)
        contactAdapter.setHeader(header)
        val footer = LayoutInflater.from(context)
            .inflate(R.layout.view_contact_list_empty, contact_recycler_view, false)
        contactAdapter.setFooter(footer)
        if (!hasContactPermission()) {
            contactAdapter.showEmptyFooter()
        } else {
            contactAdapter.hideEmptyFooter()
        }
        contactAdapter.setContactListener(mContactListener)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        title_view.right_animator.setOnClickListener { SettingActivity.show(requireContext()) }

        if (hasContactPermission() &&
            !defaultSharedPreferences.getBoolean(PREF_DELETE_MOBILE_CONTACTS, false)
        ) {
            fetchContacts()
        }

        contactsViewModel.findContacts().observe(
            viewLifecycleOwner,
            Observer { users ->
                if (users != null && users.isNotEmpty()) {
                    if (!hasContactPermission()) {
                        contactAdapter.friendSize = users.size
                        contactAdapter.users = users
                    } else {
                        val newList = arrayListOf<User>().apply {
                            addAll(users)
                            addAll(contactAdapter.users.filter { it.relationship != UserRelationship.FRIEND.name })
                        }
                        contactAdapter.friendSize = users.size
                        contactAdapter.users = newList
                    }
                } else {
                    if (!hasContactPermission()) {
                        contactAdapter.users = Collections.emptyList()
                    }
                }
                contactAdapter.notifyDataSetChanged()
            }
        )
        contactsViewModel.findSelf().observe(
            viewLifecycleOwner,
            Observer { self ->
                if (self != null) {
                    contactAdapter.me = self
                    contactAdapter.notifyDataSetChanged()
                }
            }
        )
    }

    private fun hasContactPermission() =
        requireContext().checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    private fun fetchContacts() {
        RxContacts.fetch(requireContext())
            .toSortedList(Contact::compareTo)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .autoDispose(stopScope)
            .subscribe(
                { contacts ->
                    val mutableList = mutableListOf<User>()
                    for (item in contacts) {
                        item.phoneNumbers.mapTo(mutableList) {
                            User(
                                "",
                                "", "contact", "", item.displayName,
                                "", it, false, "", null
                            )
                        }
                    }
                    mutableList.addAll(0, contactAdapter.users)
                    contactAdapter.users = mutableList
                    contactAdapter.notifyDataSetChanged()
                },
                { }
            )
        jobManager.addJobInBackground(UploadContactsJob())
    }

    private val mContactListener: ContactsAdapter.ContactListener = object : ContactsAdapter.ContactListener {

        override fun onHeaderRl() {
            ProfileBottomSheetDialogFragment.newInstance().showNow(
                parentFragmentManager,
                UserBottomSheetDialogFragment.TAG
            )
        }

        override fun onNewGroup() {
            GroupActivity.show(requireContext())
        }

        override fun onAddContact() {
            activity?.addFragment(this@ContactsFragment, AddPeopleFragment.newInstance(), AddPeopleFragment.TAG)
        }

        override fun onEmptyRl() {
            RxPermissions(requireActivity())
                .request(Manifest.permission.READ_CONTACTS)
                .autoDispose(stopScope)
                .subscribe { granted ->
                    if (granted) {
                        contactAdapter.hideEmptyFooter()
                        jobManager.addJobInBackground(UploadContactsJob())
                        fetchContacts()
                        context?.let { context ->
                            WorkManager.getInstance(context).enqueueOneTimeNetworkWorkRequest<RefreshContactWorker>()
                        }
                    } else {
                        context?.openPermissionSetting()
                    }
                }
        }

        override fun onFriendItem(user: User) {
            context?.let { ctx -> ConversationActivity.show(ctx, null, user.userId) }
        }

        override fun onContactItem(user: User) {
            ContactBottomSheetDialog.newInstance(user).showNow(parentFragmentManager, ContactBottomSheetDialog.TAG)
        }

        override fun onMyQr(self: User?) {
            self?.let {
                QrBottomSheetDialogFragment.newInstance(it.userId, TYPE_MY_QR)
                    .showNow(parentFragmentManager, QrBottomSheetDialogFragment.TAG)
            }
        }

        override fun onReceiveQr(self: User?) {
            self?.let {
                QrBottomSheetDialogFragment.newInstance(it.userId, TYPE_RECEIVE_QR)
                    .showNow(parentFragmentManager, QrBottomSheetDialogFragment.TAG)
            }
        }
    }
}
