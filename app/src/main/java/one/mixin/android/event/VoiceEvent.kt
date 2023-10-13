package one.mixin.android.event

import androidx.annotation.FloatRange
import kotlin.math.max
import kotlin.math.min

class VoiceEvent(val userId: String, audioLevel: Double) {
    @FloatRange(from = 0.0, to = 1.0)
    val audioLevel: Float

    init {
        if (audioLevel <= 0.002) {
            this.audioLevel = 0f
        } else {
            this.audioLevel = max(min((audioLevel / 0.0375f).toFloat(), 1f), 0.1875f)
        }
    }
}
