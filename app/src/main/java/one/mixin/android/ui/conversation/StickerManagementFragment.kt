package one.mixin.android.ui.conversation

import android.Manifest
import android.app.Activity
import android.arch.lifecycle.Observer
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
import androidx.core.view.updateLayoutParams
import com.bugsnag.android.Bugsnag
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.fragment_sticker_management.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.REQUEST_GALLERY
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.displaySize
import one.mixin.android.extension.loadSticker
import one.mixin.android.extension.openGalleryFromSticker
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.StickerFragment.Companion.ARGS_ALBUM_ID
import one.mixin.android.ui.conversation.StickerFragment.Companion.PADDING
import one.mixin.android.ui.conversation.adapter.StickerSpacingItemDecoration
import one.mixin.android.vo.Sticker
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.dip
import org.jetbrains.anko.textColor
import javax.inject.Inject

class StickerManagementFragment : BaseFragment() {
    companion object {
        const val TAG = "StickerManagementFragment"
        const val COLUMN = 3

        fun newInstance(id: String?) = StickerManagementFragment().apply {
            arguments = bundleOf(ARGS_ALBUM_ID to id)
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val stickerViewModel: ConversationViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ConversationViewModel::class.java)
    }

    private val padding: Int by lazy { context!!.dip(PADDING) }

    private val albumId: String? by lazy { arguments!!.getString(ARGS_ALBUM_ID) }

    private val stickers = mutableListOf<Sticker>()
    private val stickerAdapter: StickerAdapter by lazy {
        StickerAdapter(stickers)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_sticker_management, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener { requireActivity().onBackPressed() }
        title_view.right_tv.textColor = Color.BLACK
        title_view.right_animator.setOnClickListener {
            if (stickerAdapter.editing) {
                title_view.right_tv.text = getString(R.string.select)
                if (stickerAdapter.checkedList.isNotEmpty()) {
                    stickerViewModel.removeStickers(stickerAdapter.checkedList)
                }
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
                            openGalleryFromSticker()
                        } else {
                            context?.openPermissionSetting()
                        }
                    }, {
                        Bugsnag.notify(it)
                    })
            }

            override fun onDelete() {
                title_view.right_tv.text = getString(R.string.conversation_delete)
            }
        })

        if (albumId == null) { // not add any personal sticker yet
            stickerViewModel.observePersonalStickers().observe(this, Observer {
                it?.let { updateStickers(it) }
            })
        } else {
            stickerViewModel.observeStickers(albumId!!).observe(this, Observer {
                it?.let { updateStickers(it) }
            })
        }
    }

    override fun onBackPressed(): Boolean {
        if (stickerAdapter.editing) {
            stickerAdapter.editing = !stickerAdapter.editing
            stickerAdapter.checkedList.clear()
            stickerAdapter.notifyDataSetChanged()
            title_view.right_tv.text = getString(R.string.select)

            return true
        }
        return super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_GALLERY && resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                requireActivity().addFragment(this@StickerManagementFragment,
                    StickerAddFragment.newInstance(it.toString(), true), StickerAddFragment.TAG)
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
            val s = if (stickers.isNotEmpty() && position != stickers.size) {
                stickers[position]
            } else {
                null
            }
            val v = holder.itemView
            val ctx = v.context

            val params = v.layoutParams
            params.height = size
            params.width = size
            v.layoutParams = params
            val imageView = (v as ViewGroup).getChildAt(0) as ImageView
            val cover = v.getChildAt(1)
            val cb = v.getChildAt(2) as CheckBox
            if (editing) {
                cb.visibility = VISIBLE
                if (position == stickers.size) {
                    v.visibility = GONE
                } else {
                    v.visibility = VISIBLE
                }
            } else {
                cb.visibility = GONE
                v.visibility = VISIBLE
            }
            if (s != null && checkedList.contains(s.stickerId)) {
                cb.isChecked = true
                cover.visibility = VISIBLE
            } else {
                cb.isChecked = false
                cover.visibility = GONE
            }
            if (position == stickers.size) {
                imageView.setImageResource(R.drawable.ic_add_stikcer)
                imageView.setOnClickListener { listener?.onAddClick() }
                imageView.updateLayoutParams<ViewGroup.LayoutParams> {
                    width = size - ctx.dip(50)
                    height = size - ctx.dip(50)
                }
            } else {
                imageView.updateLayoutParams<ViewGroup.LayoutParams> {
                    width = size
                    height = size
                }
                if (s != null) {
                    imageView.loadSticker(s.assetUrl, s.assetType)
                    imageView.setOnClickListener {
                        handleChecked(cb, cover, s.stickerId)
                    }
                    imageView.setOnLongClickListener {
                        editing = !editing
                        notifyDataSetChanged()
                        listener?.onDelete()
                        return@setOnLongClickListener true
                    }
                }
            }
        }

        private fun handleChecked(cb: CheckBox, cover: View, stickerId: String) {
            if (editing) {
                cb.isChecked = !cb.isChecked
                if (cb.isChecked) {
                    checkedList.add(stickerId)
                    cover.visibility = VISIBLE
                } else {
                    checkedList.remove(stickerId)
                    cover.visibility = GONE
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
        fun onDelete()
    }
}