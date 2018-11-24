package one.mixin.android.ui.setting

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.drive.Drive
import com.google.android.gms.drive.DriveResourceClient
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.fragment_backup.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.Constants.BackUp.BACKUP_MEDIA
import one.mixin.android.Constants.BackUp.BACKUP_PERIOD
import one.mixin.android.Constants.DataBase.DB_NAME
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dirSize
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.getMediaPath
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putInt
import one.mixin.android.extension.toast
import one.mixin.android.job.BackupJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.Session
import one.mixin.android.util.backup.DataBaseBackupManager
import one.mixin.android.util.backup.Result
import java.util.Date
import javax.inject.Inject

class BackUpFragment : BaseFragment() {
    companion object {
        const val TAG = "AuthenticationsFragment"
        private const val RC_SIGN_IN = 9001
        fun newInstance() = BackUpFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var jobManager: MixinJobManager

    private var account: GoogleSignInAccount? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_backup, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Drive.SCOPE_FILE, Drive.SCOPE_APPFOLDER)
            .requestEmail()
            .build()

        backup_info.text = getString(R.string.backup_google_drive, "")
        account = GoogleSignIn.getAccountForScopes(requireContext(), Drive.SCOPE_FILE, Drive.SCOPE_APPFOLDER)
        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        // Todo maybe have other functions
        updateUI()
        GlobalScope.launch {
            context?.getMediaPath()?.dirSize()?.let {
                withContext(Dispatchers.Main) {
                    backup_check.text = "${getString(R.string.backup_include)} ${it.fileSize()}"
                }
            }
        }
        backup_check_box.isChecked = defaultSharedPreferences.getBoolean(BACKUP_MEDIA, false)
        backup_check_box.setOnCheckedChangeListener { _, isChecked ->
            defaultSharedPreferences.putBoolean(BACKUP_MEDIA, isChecked)
            if (isChecked) {
                RxPermissions(requireActivity())
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .subscribe({ granted ->
                        if (!granted) {
                            backup_check_box.isChecked = false
                            context?.openPermissionSetting()
                        }
                    }, {
                        backup_check_box.isChecked = false
                    })
            }
        }
        backup_account.setOnClickListener {
            if (BackupJob.backupLiveData.ing) {
                toast(R.string.backup_notification_title)
            } else if (account == null || account?.isExpired == true) {
                startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
            } else {
                googleSignInClient.signOut().addOnSuccessListener {
                    account = null
                    updateUI()
                    startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
                }
            }
        }
        backup_auto.setOnClickListener {
            showBackupDialog()
        }
        backup_auto_tv.text = options[defaultSharedPreferences.getInt(BACKUP_PERIOD, 0)]
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        backup_bn.setOnClickListener {
            jobManager.addJobInBackground(BackupJob(true))
        }
    }

    private val options by lazy {
        requireContext().resources.getStringArray(R.array.backup_dialog_list)
    }

    private fun showBackupDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.backup_dialog_title)

        val checkedItem = defaultSharedPreferences.getInt(BACKUP_PERIOD, 0)
        builder.setSingleChoiceItems(options, checkedItem) { dialog, which ->
            backup_auto_tv.text = options[which]
            defaultSharedPreferences.putInt(BACKUP_PERIOD, which)
            dialog.dismiss()
        }
        builder.setNegativeButton(android.R.string.cancel) { _, _ ->

        }
        val dialog = builder.create()
        dialog.show()
    }

    private var driveResourceClient: DriveResourceClient? = null
        set(value) {
            if (field != value) {
                field = value
                if (value != null) {
                    BackupJob.backupLiveData.observe(this, Observer {
                        if (it) {
                            backup_bn.visibility = View.INVISIBLE
                            progressGroup.visibility = View.VISIBLE
                        } else {
                            backup_bn.visibility = View.VISIBLE
                            progressGroup.visibility = View.GONE
                            if (BackupJob.backupLiveData.result == Result.SUCCESS) {
                                findBackUp()
                            }
                        }
                    })
                }
            }
        }

    private fun updateUI() {
        if (isAdded) {
            if (account != null && !account!!.isExpired) {
                backup_bn.visibility = View.VISIBLE
                if (isAdded) driveResourceClient = Drive.getDriveResourceClient(requireActivity(), account!!)
                backup_account_email.text = account!!.email
                findBackUp()
            } else {
                backup_bn.visibility = View.GONE
            }
        }
    }

    private fun findBackUp() {
        if (isAdded) {
            DataBaseBackupManager.getManager(driveResourceClient!!, DB_NAME,
                Session.getAccount()!!.identity_number, { requireContext().getDatabasePath(DB_NAME) }, Constants.DataBase.MINI_VERSION, Constants.DataBase.CURRENT_VERSION)
                .findBackup { result, metaData ->
                    if (isAdded) {
                        when (result) {
                            Result.NOT_FOUND -> {
                                backup_info.text = getString(R.string.backup_google_drive, "从未备份")
                            }
                            Result.FAILURE -> {
                                backup_info.text = getString(R.string.backup_google_drive, "错误")
                            }
                            Result.SUCCESS -> {
                                val time = metaData!!.createdDate.run {
                                    val now = Date().time
                                    val createTime = metaData.createdDate.time
                                    DateUtils.getRelativeTimeSpanString(createTime, now, when {
                                        ((now - createTime) < 60000L) -> DateUtils.SECOND_IN_MILLIS
                                        ((now - createTime) < 3600000L) -> DateUtils.MINUTE_IN_MILLIS
                                        ((now - createTime) < 86400000L) -> DateUtils.HOUR_IN_MILLIS
                                        else -> DateUtils.DAY_IN_MILLIS
                                    })
                                }
                                backup_info.text = getString(R.string.backup_google_drive, time)
                            }
                            Result.NOT_SUPPORT -> {
                                backup_info.text = getString(R.string.backup_google_drive, "不支持版本")
                            }
                            else -> {
                            }
                        }
                    }
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                account = task.getResult(ApiException::class.java)
                updateUI()
            } catch (e: ApiException) {
                account = null
                updateUI()
            }
        }
    }
}