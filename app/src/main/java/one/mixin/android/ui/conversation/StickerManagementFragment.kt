package one.mixin.android.ui.conversation

import android.Manifest
import android.app.Activity
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import com.bugsnag.android.Bugsnag
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.fragment_sticker_management.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.REQUEST_GALLERY
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.displaySize
import one.mixin.android.extension.openGallery
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.StickerFragment.Companion.ARGS_ALBUM_ID
import one.mixin.android.ui.conversation.StickerFragment.Companion.PADDING
import one.mixin.android.ui.conversation.adapter.StickerSpacingItemDecoration
import one.mixin.android.vo.Sticker
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.dip
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.textColor
import org.jetbrains.anko.uiThread
import javax.inject.Inject

class StickerManagementFragment : BaseFragment() {
    companion object {
        const val TAG = "StickerManagementFragment"
        const val COLUMN = 4

        fun newInstance(id: String) = StickerManagementFragment().apply {
            arguments = bundleOf(ARGS_ALBUM_ID to id)
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val stickerViewModel: ConversationViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ConversationViewModel::class.java)
    }

    private val padding: Int by lazy { context!!.dip(PADDING) }

    private val albumId: String by lazy { arguments!!.getString(ARGS_ALBUM_ID) }

    private val stickers = mutableListOf<Sticker>()
    private val stickerAdapter: StickerAdapter by lazy {
        StickerAdapter(stickers)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_sticker_management, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.right_tv.textColor = Color.BLACK
        title_view.right_animator.setOnClickListener {
            if (stickerAdapter.editing) {
                title_view.right_tv.text = getString(R.string.done)
                //TODO
            } else {
                title_view.right_tv.text = getString(R.string.conversation_delete)
            }
            stickerAdapter.editing = !stickerAdapter.editing
            stickerAdapter.notifyDataSetChanged()
        }
        sticker_rv.layoutManager = GridLayoutManager(context, COLUMN)
        sticker_rv.addItemDecoration(StickerSpacingItemDecoration(COLUMN, padding, true))
        stickerAdapter.size = (requireContext().displaySize().x - COLUMN * padding) / COLUMN
        sticker_rv.adapter = stickerAdapter
        stickerAdapter.setOnStickerListener(object : StickerListener {
            override fun onAddClick() {
                RxPermissions(activity!!)
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                    .subscribe({ granted ->
                        if (granted) {
                            openGallery()
                        } else {
                            context?.openPermissionSetting()
                        }
                    }, {
                        Bugsnag.notify(it)
                    })
            }
        })

        doAsync {
            val list = stickerViewModel.getStickers(albumId)
            uiThread { updateStickers(list) }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_GALLERY && resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                requireActivity().addFragment(this@StickerManagementFragment,
                    StickerAddFragment.newInstance(it), StickerAddFragment.TAG)
            }
        }
    }

    @Synchronized
    private fun updateStickers(list: List<Sticker>) {
        if (!isAdded) return
        stickers.clear()
        stickers.addAll(list)
        stickerAdapter.notifyDataSetChanged()
    }

    private class StickerAdapter(private val stickers: List<Sticker>) : RecyclerView.Adapter<StickerViewHolder>() {
        private var listener: StickerListener? = null
        var size: Int = 0
        var editing = false
        val checkedList = arrayListOf<String>()

        override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
            val s = if (stickers.isNotEmpty()) {
                stickers[position]
            } else {
                null
            }
            val v= holder.itemView
            val ctx = v.context

            val params = v.layoutParams
            params.height = size
            v.layoutParams = params
            val imageView = (v as ViewGroup).getChildAt(0) as ImageView
            val cover = v.getChildAt(1)
            val cb = v.getChildAt(2) as CheckBox
            if (editing) {
                cb.visibility = VISIBLE
                if (position == itemCount && v.visibility == VISIBLE) {
                    v.visibility = GONE
                }
            } else {
                cb.visibility = GONE
                if (position == itemCount && v.visibility == GONE) {
                    v.visibility = VISIBLE
                }
            }
            if (s != null && checkedList.contains(s.stickerId)) {
                cb.isChecked = true
                cover.visibility = VISIBLE
            } else {
                cb.isChecked = false
                cover.visibility = GONE
            }
            if (position == itemCount) {
                imageView.setImageResource(R.drawable.ic_add_stikcer)
                imageView.setOnClickListener { listener?.onAddClick() }
            } else {
                if (s != null) {
                    Glide.with(ctx).load(s.assetUrl).apply(
                        if (size <= 0) RequestOptions().dontAnimate().override(Target.SIZE_ORIGINAL)
                        else RequestOptions().dontAnimate().override(size, size))
                        .into(imageView)
                    imageView.setOnClickListener {
                        if (!editing) {
                            if (cb.isChecked) {
                                checkedList.remove(s.stickerId)
                            } else {
                                checkedList.add(s.stickerId)
                            }
                        }
                    }
                }
            }
        }

        override fun getItemCount(): Int = stickers.size + 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sticker_management, parent, false)
            return StickerViewHolder(view)
        }

        fun setOnStickerListener(onStickerListener: StickerListener) {
            listener = onStickerListener
        }
    }

    private class StickerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    interface StickerListener {
        fun onAddClick()
    }
}