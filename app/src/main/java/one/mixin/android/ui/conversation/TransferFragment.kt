package one.mixin.android.ui.conversation

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_transfer.*
import kotlinx.android.synthetic.main.item_transfer_type.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.android.synthetic.main.view_wallet_transfer_type_bottom.view.*
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.putString
import one.mixin.android.extension.showKeyboard
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAssetsJob
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.common.itemdecoration.SpaceItemDecoration
import one.mixin.android.ui.conversation.tansfer.TransferBottomSheetDialogFragment
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.User
import one.mixin.android.widget.BottomSheet
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.UUID
import javax.inject.Inject

@SuppressLint("InflateParams")
class TransferFragment : BaseFragment() {
    companion object {
        const val TAG = "TransferFragment"
        const val ASSERT_PREFERENCE = "TRANSFER_ASSERT"

        fun newInstance(user: User): TransferFragment {
            val fragment = TransferFragment()
            val b = Bundle()
            b.putParcelable(Constants.ARGS_USER, user)
            fragment.arguments = b
            return fragment
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var jobManager: MixinJobManager

    private val chatViewModel: ConversationViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ConversationViewModel::class.java)
    }

    private var assets = listOf<AssetItem>()
    private var currentAsset: AssetItem? = null
        set(value) {
            field = value
            adapter.currentAsset = value
            activity?.defaultSharedPreferences!!.putString(ASSERT_PREFERENCE, value?.assetId)
        }

    private val adapter by lazy {
        TypeAdapter()
    }

    private val user: User by lazy {
        val u = arguments!!.getParcelable(Constants.ARGS_USER) as User
        u
    }

    private val assetsView: View by lazy {
        val view = View.inflate(context, R.layout.view_wallet_transfer_type_bottom, null)
        view.type_rv.addItemDecoration(SpaceItemDecoration())
        view.type_rv.adapter = adapter
        view
    }

    private val assetsBottomSheet: BottomSheet by lazy {
        val builder = BottomSheet.Builder(context!!)
        val bottomSheet = builder.create()
        builder.setCustomView(assetsView)
        bottomSheet.setOnDismissListener {
            if (isAdded) {
                transfer_amount.post { transfer_amount.showKeyboard() }
            }
        }
        bottomSheet
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_transfer, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        jobManager.addJobInBackground(RefreshAssetsJob())
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        title_view.avatar_iv.visibility = View.VISIBLE
        title_view.avatar_iv.setTextSize(16f)
        title_view.avatar_iv.setInfo(if (!user.fullName.isNullOrEmpty()) user.fullName!![0] else ' ',
            user.avatarUrl, user.identityNumber)
        title_view.avatar_iv.setOnClickListener {
            UserBottomSheetDialogFragment.newInstance(user).show(fragmentManager, UserBottomSheetDialogFragment.TAG)
        }
        title_view.setSubTitle(getString(R.string.conversation_status_transfer), getString(R.string.to, user.fullName))
        transfer_amount.addTextChangedListener(mWatcher)
        asset_rl.setOnClickListener {
            transfer_amount.hideKeyboard()
            context?.let {
                adapter.coins = assets
                adapter.setTypeListener(object : OnTypeClickListener {
                    override fun onTypeClick(asset: AssetItem) {
                        currentAsset = asset
                        asset_name.text = asset.name
                        asset_desc.text = asset.balance.numberFormat()
                        asset_avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
                        asset_avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
                        adapter.notifyDataSetChanged()
                        assetsBottomSheet.dismiss()
                    }
                })

                assetsView.type_cancel.setOnClickListener {
                    assetsBottomSheet.dismiss()
                }
                assetsBottomSheet.show()

                if (assets.size > 3) {
                    assetsBottomSheet.setCustomViewHeight(it.dpToPx(300f))
                }
            }
        }
        continue_animator.setOnClickListener {
            if (!isAdded) return@setOnClickListener

            transfer_amount.hideKeyboard()
            val bottom = TransferBottomSheetDialogFragment
                .newInstance(user, transfer_amount.text.toString(), currentAsset!!.toAsset(), UUID.randomUUID().toString(),
                    transfer_memo.text.toString())
            bottom.show(fragmentManager, TransferBottomSheetDialogFragment.TAG)
            bottom.setCallback(object : TransferBottomSheetDialogFragment.Callback {
                override fun onSuccess() {
                    this@TransferFragment.activity?.onBackPressed()
                }
            })
        }

        chatViewModel.assetItemsWithBalance().observe(this, Observer { r: List<AssetItem>? ->
            if (r != null && r.isNotEmpty()) {
                assets = r
                adapter.coins = r
                expand_iv.visibility = VISIBLE
                asset_rl.isEnabled = true

                notNullElse(r.find {
                    it.assetId == activity?.defaultSharedPreferences!!.getString(ASSERT_PREFERENCE, "")
                }, { a ->
                    asset_avatar.bg.loadImage(a.iconUrl, R.drawable.ic_avatar_place_holder)
                    asset_avatar.badge.loadImage(a.chainIconUrl, R.drawable.ic_avatar_place_holder)
                    asset_name.text = a.name
                    asset_desc.text = a.balance.numberFormat()
                    currentAsset = a
                }, {
                    val a = assets[0]
                    asset_avatar.bg.loadImage(a.iconUrl, R.drawable.ic_avatar_place_holder)
                    asset_avatar.badge.loadImage(a.chainIconUrl, R.drawable.ic_avatar_place_holder)
                    asset_name.text = a.name
                    asset_desc.text = a.balance.numberFormat()
                    currentAsset = a
                })
            } else {
                expand_iv.visibility = GONE
                asset_rl.isEnabled = false

                doAsync {
                    val xin = chatViewModel.getXIN()
                    uiThread {
                        if (!isAdded) return@uiThread

                        notNullElse(xin, {
                            asset_avatar.bg.loadImage(it.iconUrl, R.drawable.ic_avatar_place_holder)
                            asset_avatar.badge.loadImage(it.chainIconUrl, R.drawable.ic_avatar_place_holder)
                            asset_name.text = it.name
                            asset_desc.text = it.balance.numberFormat()
                        }, {
                            asset_avatar.bg.setImageResource(R.drawable.ic_avatar_place_holder)
                            asset_name.text = getString(R.string.app_name)
                            asset_desc.text = "0"
                        })
                    }
                }
            }
        })

        transfer_amount.post { transfer_amount.showKeyboard() }
    }

    private val mWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable) {
            if (s.isNotEmpty() && asset_rl.isEnabled) {
                transfer_amount.textSize = 26f
                continue_animator.visibility = VISIBLE
            } else {
                transfer_amount.textSize = 16f
                continue_animator.visibility = GONE
            }
        }
    }

    class TypeAdapter : RecyclerView.Adapter<ItemHolder>() {
        var coins: List<AssetItem>? = null
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        private var typeListener: OnTypeClickListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
            ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_transfer_type, parent, false))

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            if (coins == null || coins!!.isEmpty()) {
                return
            }
            val itemAssert = coins!![position]
            holder.itemView.type_avatar.bg.loadImage(itemAssert.iconUrl, R.drawable.ic_avatar_place_holder)
            holder.itemView.type_avatar.badge.loadImage(itemAssert.chainIconUrl, R.drawable.ic_avatar_place_holder)
            holder.itemView.asset_name.text = itemAssert.name
            holder.itemView.value.text = itemAssert.balance
            currentAsset?.let {
                holder.itemView.check_iv.visibility = if (itemAssert.assetId == currentAsset?.assetId) VISIBLE else GONE
            }
            holder.itemView.setOnClickListener {
                typeListener?.onTypeClick(itemAssert)
            }
        }

        override fun getItemCount(): Int = notNullElse(coins, { it.size }, 0)

        fun setTypeListener(listener: OnTypeClickListener) {
            typeListener = listener
        }

        var currentAsset: AssetItem? = null
    }

    interface OnTypeClickListener {
        fun onTypeClick(asset: AssetItem)
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
