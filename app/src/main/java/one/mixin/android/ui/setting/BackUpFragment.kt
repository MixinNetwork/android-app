package one.mixin.android.ui.setting

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.drive.demo.backup.Result
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
import one.mixin.android.Constants.DataBase.DB_NAME
import one.mixin.android.R
import one.mixin.android.extension.dirSize
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.getMediaPath
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.Session
import one.mixin.android.util.backup.DataBaseBackupManager
import one.mixin.android.util.backup.FileBackupManager
import timber.log.Timber
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_backup, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Drive.SCOPE_FILE, Drive.SCOPE_APPFOLDER)
            .requestEmail()
            .build()

        backup_info.text = getString(R.string.backup_google_drive, "")
        val account = GoogleSignIn.getAccountForScopes(requireContext(), Drive.SCOPE_FILE, Drive.SCOPE_APPFOLDER)
        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        // Todo maybe have other funcation
        updateUI(account)
        sign_out.setOnClickListener {
            googleSignInClient.revokeAccess().addOnSuccessListener {
                updateUI(null)
            }
        }
        GlobalScope.launch {
            context?.getMediaPath()?.dirSize()?.let {
                withContext(Dispatchers.Main) {
                    backup_check.text = "${getString(R.string.backup_include)} ${it.fileSize()}"
                }
            }
        }

        backup_check_box.setOnCheckedChangeListener { _, isChecked ->
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
        sign_in.setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
        }
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        backup_bn.setOnClickListener {
            // todo
            dp.show()
            DataBaseBackupManager.getManager(driveResourceClient!!, DB_NAME,
                Session.getAccount()!!.identity_number, { requireContext().getDatabasePath(DB_NAME) }, Constants.DataBase.MINI_VERSION, Constants.DataBase.CURRENT_VERSION)
                .backup { result ->
                    when (result) {
                        Result.NOT_FOUND -> {
                            toast("file not found")
                        }
                        Result.FAILURE -> {
                            toast("backup failure")
                        }
                        Result.SUCCESS -> {
                            toast("backup success")
                            if (backup_check_box.isChecked) {
                                FileBackupManager.getManager(driveResourceClient!!, Session.getAccount()!!.identity_number).backup { result ->
                                    Timber.d("上传媒体文件结果：$result")
                                }
                            }
                            findBackUp()
                        }
                        else -> {
                            toast("???")
                        }
                    }
                    dp.dismiss()
                }
        }
        FileBackupManager.getManager(driveResourceClient!!,
            Session.getAccount()!!.identity_number).getMediaProgress().observe(this, Observer {
            if (it == null) {
                progressBar.visibility = View.GONE
                backup_bn.visibility = View.VISIBLE
            } else {
                progressBar.visibility = View.VISIBLE
                backup_bn.visibility = View.INVISIBLE
                progressBar.progress = it
            }
        })
    }

    private val dp by lazy {
        indeterminateProgressDialog(message = "备份数据库，请勿关闭...") {
            setCancelable(false)
        }
    }

    private var driveResourceClient: DriveResourceClient? = null
    private fun updateUI(account: GoogleSignInAccount?) {
        if (account != null && account.displayName != null) {
            backup_bn.visibility = View.VISIBLE
            sign_out.text = "sign out: ${account.email}"
            sign_out.visibility = View.VISIBLE
            sign_in.visibility = View.GONE
            if (isAdded) driveResourceClient = Drive.getDriveResourceClient(requireActivity(), account)
            findBackUp()
        } else {
            backup_bn.visibility = View.GONE
            sign_out.visibility = View.GONE
            sign_in.visibility = View.VISIBLE
        }
    }

    private fun findBackUp() {
        DataBaseBackupManager.getManager(driveResourceClient!!, DB_NAME,
            Session.getAccount()!!.identity_number, { requireContext().getDatabasePath(DB_NAME) }, Constants.DataBase.MINI_VERSION, Constants.DataBase.CURRENT_VERSION)
            .findBackup { result, metaData ->
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
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                updateUI(account)
            } catch (e: ApiException) {

            }
        }
    }
}