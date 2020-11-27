package one.mixin.android.ui.common.friends

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import one.mixin.android.databinding.FragmentFriendsBinding
import one.mixin.android.databinding.ViewTitleBinding
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.vo.User

abstract class BaseFriendsFragment<VH : BaseFriendsViewHolder> : BaseFragment() {

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

    private var _binding: FragmentFriendsBinding? = null
    protected val binding get() = requireNotNull(_binding)
    private var _titleBinding: ViewTitleBinding? = null
    private val titleBinding get() = requireNotNull(_titleBinding)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFriendsBinding.inflate(layoutInflater, container, false).apply {
            _titleBinding = ViewTitleBinding.bind(titleView)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        titleBinding.apply {
            titleTv.text = getString(getTitleResId())
            leftIb.setOnClickListener {
                binding.searchEt.hideKeyboard()
                activity?.onBackPressed()
            }
        }
        binding.apply {
            friendsRv.adapter = adapter
            lifecycleScope.launch {
                users = getFriends()
            }

            searchEt.addTextChangedListener(
                object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                    override fun afterTextChanged(s: Editable) {
                        keyWord = s.toString()
                        adapter.filter = keyWord
                        userCallback.filter = keyWord
                    }
                }
            )
        }
    }

    private fun dataChange() {
        adapter.submitList(
            if (keyWord.isNotBlank()) {
                users.filter {
                    it.fullName?.contains(keyWord, true) == true ||
                        it.identityNumber.contains(keyWord, true)
                }.sortedByDescending { it.fullName == keyWord || it.identityNumber == keyWord }
            } else {
                users
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _titleBinding = null
    }

    abstract fun getTitleResId(): Int
    abstract suspend fun getFriends(): List<User>
}
