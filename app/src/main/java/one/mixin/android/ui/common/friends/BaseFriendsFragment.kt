package one.mixin.android.ui.common.friends

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_friends.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.ui.common.BaseViewModelFragment
import one.mixin.android.vo.User

abstract class BaseFriendsFragment<VH : BaseFriendsViewHolder, VM : ViewModel> : BaseViewModelFragment<VM>() {

    protected lateinit var adapter: AbsFriendsAdapter<VH>

    private var users: List<User> = listOf()
        set(value) {
            field = value
            dataChange()
        }
    private var keyWord: String = ""
        set(value) {
            field = value
            dataChange()
        }

    protected val userCallback = UserItemCallback("")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        layoutInflater.inflate(R.layout.fragment_friends, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.title_tv.text = getString(getTitleResId())
        title_view.left_ib.setOnClickListener {
            search_et.hideKeyboard()
            activity?.onBackPressed()
        }
        friends_rv.adapter = adapter
        lifecycleScope.launch {
            users = getFriends()
        }

        search_et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                keyWord = s.toString()
                adapter.filter = keyWord
                userCallback.filter = keyWord
            }
        })
    }

    private fun dataChange() {
        adapter.submitList(if (keyWord.isNotBlank()) {
            users.filter {
                it.fullName?.contains(keyWord, true) == true ||
                    it.identityNumber.contains(keyWord, true)
            }.sortedByDescending { it.fullName == keyWord || it.identityNumber == keyWord }
        } else {
            users
        })
    }

    abstract fun getTitleResId(): Int
    abstract suspend fun getFriends(): List<User>
}
