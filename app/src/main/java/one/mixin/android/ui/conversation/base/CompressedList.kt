package one.mixin.android.ui.conversation.base

import java.util.AbstractList

class CompressedList<E> : AbstractList<E?> {
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
}
