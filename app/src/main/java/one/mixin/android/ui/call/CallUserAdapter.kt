package one.mixin.android.ui.call

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.android.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.ItemCallAddBinding
import one.mixin.android.databinding.ItemCallUserBinding
import one.mixin.android.event.FrameKeyEvent
import one.mixin.android.event.VoiceEvent
import one.mixin.android.extension.dp
import one.mixin.android.extension.round
import one.mixin.android.vo.CallUser

class CallUserAdapter(private val self: CallUser, private val callClicker: (String?) -> Unit) :
    ListAdapter<CallUser, RecyclerView.ViewHolder>(CallUser.DIFF_CALLBACK) {
    var guestsNotConnected: List<String>? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ) =
        if (viewType == 1) {
            CallUserHolder(
                ItemCallUserBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                ),
            )
        } else {
            AddUserHolder(
                ItemCallAddBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                ),
            )
        }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        if (getItemViewType(position) == 1) {
            getItem(position - 1)?.let {
                (holder as CallUserHolder).bind(it, self, guestsNotConnected, callClicker)
            }
        } else {
            (holder as AddUserHolder).bind(callClicker)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            0
        } else {
            1
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + 1
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCurrentListChanged(
        previousList: MutableList<CallUser>,
        currentList: MutableList<CallUser>,
    ) {
        if (previousList != currentList) {
            notifyDataSetChanged()
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        if (holder is CallUserHolder) {
            getItem(holder.layoutPosition - 1)?.let {
                holder.listen(holder.itemView, it.userId)
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        if (holder is CallUserHolder) {
            holder.stopListen()
        }
    }
}

class AddUserHolder(val binding: ItemCallAddBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(callClicker: (String?) -> Unit) {
        itemView.setOnClickListener {
            callClicker(null)
        }
    }
}

class CallUserHolder(val binding: ItemCallUserBinding) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.loading.round(64.dp)
    }

    fun bind(
        user: CallUser,
        self: CallUser,
        guestsNotConnected: List<String>?,
        callClicker: (String?) -> Unit,
    ) {
        itemView.apply {
            binding.avatarView.setInfo(user.fullName, user.avatarUrl, user.userId)
            binding.nameTv.setName(user)
            binding.loading.setAutoRepeat(true)
            binding.loading.setAnimation(R.raw.anim_call_loading, 64.dp, 64.dp)
            binding.loading.playAnimation()
            val vis =
                user.userId != self.userId && guestsNotConnected?.contains(user.userId) == true
            binding.loading.isVisible = vis
            binding.blinkRing.setColor(R.color.call_voice)
            binding.ring.setColor(R.color.colorRed)
            binding.cover.isVisible = vis
            setOnClickListener {
                callClicker(user.userId)
            }
        }
    }

    private var blinkDisposable: Disposable? = null
    private var disposable: Disposable? = null

    fun listen(
        view: View,
        userId: String,
    ) {
        if (blinkDisposable == null) {
            blinkDisposable =
                RxBus.listen(VoiceEvent::class.java)
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(view)
                    .subscribe {
                        if (it.userId == userId) {
                            binding.blinkRing.updateAudioLevel(it.audioLevel)
                            if (it.audioLevel != 0f) {
                                binding.ring.isVisible = false
                            }
                        }
                    }
        }
        if (disposable == null) {
            disposable =
                RxBus.listen(FrameKeyEvent::class.java)
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(view)
                    .subscribe {
                        if (it.userId == userId) {
                            binding.ring.isVisible = !it.hasKey
                            if (!it.hasKey) {
                                binding.blinkRing.isVisible = false
                            }
                        }
                    }
        }
    }

    fun stopListen() {
        blinkDisposable?.dispose()
        blinkDisposable = null
        disposable?.dispose()
        disposable = null
    }
}
