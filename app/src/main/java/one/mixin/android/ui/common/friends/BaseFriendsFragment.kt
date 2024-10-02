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
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.equalsIgnoreCase
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFriendsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.titleTv.setTextOnly(getTitleResId())
            titleView.leftIb.setOnClickListener {
                binding.searchEt.hideKeyboard()
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            friendsRv.adapter = adapter
            lifecycleScope.launch {
                users = getFriends()
            }

            searchEt.addTextChangedListener(
                object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int,
                    ) {}

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int,
                    ) {}

                    override fun afterTextChanged(s: Editable) {
                        keyWord = s.toString()
                        adapter.filter = keyWord
                        userCallback.filter = keyWord
                    }
                },
            )
        }
    }

    private fun dataChange() {
        adapter.submitList(
            if (keyWord.isNotBlank()) {
                users.filter {
                    it.fullName?.containsIgnoreCase(keyWord) == true ||
                        it.identityNumber.containsIgnoreCase(keyWord)
                }.sortedByDescending { it.fullName.equalsIgnoreCase(keyWord) || it.identityNumber.equalsIgnoreCase(keyWord) }
            } else {
                users
            },
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    abstract fun getTitleResId(): Int

    abstract suspend fun getFriends(): List<User>
}
