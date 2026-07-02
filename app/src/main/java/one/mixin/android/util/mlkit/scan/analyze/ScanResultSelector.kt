package one.mixin.android.util.mlkit.scan.analyze

sealed class ScanDecision {
    data object Continue : ScanDecision()

    data class Track(val item: BarcodeScanItem) : ScanDecision()

    data class AutoHandle(val text: String) : ScanDecision()

    data class ShowChoices(val items: List<BarcodeScanItem>) : ScanDecision()
}

class ScanResultSelector(
    private val stableFrames: Int = 2,
) {
    private var lastText: String? = null
    private var stableCount = 0

    fun select(items: List<BarcodeScanItem>): ScanDecision {
        return when (items.size) {
            0 -> {
                reset()
                ScanDecision.Continue
            }
            1 -> selectSingle(items[0])
            else -> {
                reset()
                ScanDecision.ShowChoices(items)
            }
        }
    }

    fun reset() {
        lastText = null
        stableCount = 0
    }

    private fun selectSingle(item: BarcodeScanItem): ScanDecision {
        stableCount =
            if (item.text == lastText) {
                stableCount + 1
            } else {
                1
            }
        lastText = item.text
        return if (stableCount >= stableFrames) {
            ScanDecision.AutoHandle(item.text)
        } else {
            ScanDecision.Track(item)
        }
    }
}
