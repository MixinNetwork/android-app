package one.mixin.android.ui.conversation.chat

import java.util.AbstractList

class CompressedList<E : Any> : AbstractList<E?> {
    private val wrapped: MutableList<E?>

    constructor(source: List<E>) {
        wrapped = ArrayList(source)
    }

    constructor(totalSize: Int) {
        wrapped = ArrayList(totalSize)
        for (i in 0 until totalSize) {
            wrapped.add(null)
        }
    }

    override fun get(index: Int): E? {
        return wrapped[index]
    }

    override val size: Int
        get() = wrapped.size

    override fun set(globalIndex: Int, element: E?): E? {
        return wrapped.set(globalIndex, element)
    }

    override fun add(index: Int, element: E?) {
        wrapped.add(index, element)
    }

    fun append(elements: Collection<E>) {
        wrapped.addAll(elements)
    }

    fun prepend(elements: Collection<E>) {
        wrapped.addAll(0, elements)
    }

    fun first(): E? = wrapped.first()

    fun last(): E? = wrapped.last()
}
