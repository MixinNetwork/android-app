package one.mixin.android.ui.address.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemAddressBinding
import one.mixin.android.extension.fullDate
import one.mixin.android.vo.Address
import one.mixin.android.vo.displayAddress

class AddressAdapter : RecyclerView.Adapter<AddressAdapter.ItemHolder>() {
    var addresses: MutableList<Address>? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private var addrListener: AddressListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
        ItemHolder(ItemAddressBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        if (addresses == null || addresses!!.isEmpty()) {
            return
        }
        val addr = addresses!![position]
        holder.itemBinding.apply {
            nameTv.text = addr.label
            addrTv.text = addr.displayAddress()
            createdTv.text = addr.updatedAt.fullDate()
        }
        holder.itemView.setOnClickListener { addrListener?.onAddrClick(addr) }
        holder.itemView.setOnLongClickListener {
            addrListener?.onAddrLongClick(holder.itemView, addr)
            return@setOnLongClickListener true
        }
    }

    override fun getItemCount(): Int = addresses?.size ?: 0

    fun removeItem(pos: Int): Address? {
        val addr = addresses?.removeAt(pos)
        notifyItemRemoved(pos)
        return addr
    }

    fun restoreItem(item: Address, pos: Int) {
        addresses?.add(pos, item)
        notifyItemInserted(pos)
    }

    fun setAddrListener(listener: AddressListener) {
        addrListener = listener
    }

    interface AddressListener {
        fun onAddrClick(addr: Address)

        fun onAddrLongClick(view: View, addr: Address)
    }

    open class SimpleAddressListener : AddressListener {
        override fun onAddrClick(addr: Address) {}
        override fun onAddrLongClick(view: View, addr: Address) {}
    }

    class ItemHolder(val itemBinding: ItemAddressBinding) : RecyclerView.ViewHolder(itemBinding.root)
}
