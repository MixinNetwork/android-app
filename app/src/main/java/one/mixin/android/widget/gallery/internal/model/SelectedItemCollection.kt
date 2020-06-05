package one.mixin.android.widget.gallery.internal.model

import android.content.Context
import android.net.Uri
import android.os.Bundle
import one.mixin.android.extension.getFilePath
import one.mixin.android.widget.gallery.internal.entity.IncapableCause
import one.mixin.android.widget.gallery.internal.entity.Item
import one.mixin.android.widget.gallery.internal.entity.SelectionSpec
import one.mixin.android.widget.gallery.internal.utils.PhotoMetadataUtils
import java.util.ArrayList
import java.util.LinkedHashSet

class SelectedItemCollection(private val mContext: Context) {
    private var mItems: MutableSet<Item>? = null
    var collectionType = COLLECTION_UNDEFINED
        private set

    val dataWithBundle: Bundle
        get() {
            val bundle = Bundle()
            bundle.putParcelableArrayList(STATE_SELECTION, ArrayList(mItems!!))
            bundle.putInt(STATE_COLLECTION_TYPE, collectionType)
            return bundle
        }

    val isEmpty: Boolean
        get() = mItems == null || mItems!!.isEmpty()

    fun onCreate(bundle: Bundle?) {
        if (bundle == null) {
            mItems = LinkedHashSet()
        } else {
            val saved = bundle.getParcelableArrayList<Item>(STATE_SELECTION)
            mItems = LinkedHashSet(saved!!)
            collectionType = bundle.getInt(STATE_COLLECTION_TYPE, COLLECTION_UNDEFINED)
        }
    }

    fun setDefaultSelection(uris: List<Item>) {
        mItems!!.addAll(uris)
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(STATE_SELECTION, ArrayList(mItems!!))
        outState.putInt(STATE_COLLECTION_TYPE, collectionType)
    }

    fun add(item: Item): Boolean {
        if (typeConflict(item)) {
            throw IllegalArgumentException("Can't select images and videos at the same time.")
        }
        val added = mItems!!.add(item)
        if (added) {
            if (collectionType == COLLECTION_UNDEFINED) {
                if (item.isImage) {
                    collectionType = COLLECTION_IMAGE
                } else if (item.isVideo) {
                    collectionType = COLLECTION_VIDEO
                }
            } else if (collectionType == COLLECTION_IMAGE) {
                if (item.isVideo) {
                    collectionType = COLLECTION_MIXED
                }
            } else if (collectionType == COLLECTION_VIDEO) {
                if (item.isImage) {
                    collectionType = COLLECTION_MIXED
                }
            }
        }
        return added
    }

    fun remove(item: Item): Boolean {
        val removed = mItems!!.remove(item)
        if (removed) {
            if (mItems!!.size == 0) {
                collectionType = COLLECTION_UNDEFINED
            } else {
                if (collectionType == COLLECTION_MIXED) {
                    refineCollectionType()
                }
            }
        }
        return removed
    }

    fun overwrite(items: ArrayList<Item>, collectionType: Int) {
        if (items.size == 0) {
            this.collectionType = COLLECTION_UNDEFINED
        } else {
            this.collectionType = collectionType
        }
        mItems!!.clear()
        mItems!!.addAll(items)
    }

    fun asList(): List<Item> {
        return ArrayList(mItems!!)
    }

    fun asListOfUri(): List<Uri> {
        val uris = ArrayList<Uri>()
        for (item in mItems!!) {
            uris.add(item.contentUri)
        }
        return uris
    }

    fun asListOfString(): List<String> {
        val paths = ArrayList<String>()
        for (item in mItems!!) {
            val path = item.contentUri.getFilePath() ?: continue
            paths.add(path)
        }
        return paths
    }

    fun isSelected(item: Item): Boolean {
        return mItems!!.contains(item)
    }

    fun isAcceptable(item: Item): IncapableCause? {
        return PhotoMetadataUtils.isAcceptable(mContext, item)
    }

    fun maxSelectableReached(): Boolean {
        return mItems!!.size == currentMaxSelectable()
    }

    // depends
    private fun currentMaxSelectable(): Int {
        val spec = SelectionSpec.getInstance()
        return if (spec.maxSelectable > 0) {
            spec.maxSelectable
        } else if (collectionType == COLLECTION_IMAGE) {
            spec.maxImageSelectable
        } else if (collectionType == COLLECTION_VIDEO) {
            spec.maxVideoSelectable
        } else {
            spec.maxSelectable
        }
    }

    private fun refineCollectionType() {
        var hasImage = false
        var hasVideo = false
        for (i in mItems!!) {
            if (i.isImage && !hasImage) hasImage = true
            if (i.isVideo && !hasVideo) hasVideo = true
        }
        if (hasImage && hasVideo) {
            collectionType = COLLECTION_MIXED
        } else if (hasImage) {
            collectionType = COLLECTION_IMAGE
        } else if (hasVideo) {
            collectionType = COLLECTION_VIDEO
        }
    }

    private fun typeConflict(item: Item): Boolean {
        return SelectionSpec.getInstance().mediaTypeExclusive && (item.isImage && (collectionType == COLLECTION_VIDEO || collectionType == COLLECTION_MIXED) || item.isVideo && (collectionType == COLLECTION_IMAGE || collectionType == COLLECTION_MIXED))
    }

    fun count(): Int {
        return mItems!!.size
    }

    companion object {

        val STATE_SELECTION = "state_selection"
        val STATE_COLLECTION_TYPE = "state_collection_type"
        val COLLECTION_UNDEFINED = 0x00
        val COLLECTION_IMAGE = 0x01
        val COLLECTION_VIDEO = 0x01 shl 1
        val COLLECTION_MIXED = COLLECTION_IMAGE or COLLECTION_VIDEO
    }
}
