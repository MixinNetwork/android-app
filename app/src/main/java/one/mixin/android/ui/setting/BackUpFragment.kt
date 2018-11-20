package one.mixin.android.ui.setting

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.drive.demo.backup.Result
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.drive.Drive
import com.google.android.gms.drive.DriveResourceClient
import kotlinx.android.synthetic.main.fragment_backup.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.landing.RestoreActivity
import one.mixin.android.util.Session
import one.mixin.android.util.backup.DataBaseBackupManager
import timber.log.Timber
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

        val account = GoogleSignIn.getAccountForScopes(requireContext(), Drive.SCOPE_FILE, Drive.SCOPE_APPFOLDER)
        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        // Todo maybe have other funcation
        updateUI(account)
        sign_out.setOnClickListener {
            googleSignInClient.revokeAccess().addOnSuccessListener {
                Timber.d("sign out success")
                updateUI(null)
            }
        }
        sign_in.setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
        }
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        backup.setOnClickListener {
            // todo
            dp.show()
            DataBaseBackupManager.getManager(driveResourceClient!!, "mixin.db",
                Session.getAccount()!!.identity_number, { requireContext().getDatabasePath("mixin.db") }, Constants.DataBase.MINI_VERSION, Constants.DataBase.CURRENT_VERSION)
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
                        }
                        else -> {
                            toast("???")
                        }
                    }
                    dp.dismiss()
                }
        }
        findBackup.setOnClickListener { it ->
            DataBaseBackupManager.getManager(driveResourceClient!!, "mixin.db",
                Session.getAccount()!!.identity_number, { requireContext().getDatabasePath("mixin.db") }, Constants.DataBase.MINI_VERSION, Constants.DataBase.CURRENT_VERSION)
                .findBackup { result, metaData ->
                    when (result) {
                        Result.NOT_FOUND -> {
                            toast("file not found")
                        }
                        Result.FAILURE -> {
                            toast("failure")
                        }
                        Result.SUCCESS -> {
                            toast("find ${metaData?.title}")
                            RestoreActivity.show(requireContext())
                        }
                        else -> {
                            toast("???")
                        }
                    }
                }
        }
    }

    private val dp by lazy {
        indeterminateProgressDialog(message = "备份数据库，请勿关闭...") {
            setCancelable(false)
        }
    }

    private var driveResourceClient: DriveResourceClient? = null
    private fun updateUI(account: GoogleSignInAccount?) {
        if (account != null && account.displayName != null) {
            backup.visibility = View.VISIBLE
            sign_out.visibility = View.VISIBLE
            findBackup.visibility = View.VISIBLE
            sign_in.visibility = View.GONE
            if (isAdded) driveResourceClient = Drive.getDriveResourceClient(requireActivity(), account)
        } else {
            backup.visibility = View.GONE
            sign_out.visibility = View.GONE
            findBackup.visibility = View.GONE
            sign_in.visibility = View.VISIBLE
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