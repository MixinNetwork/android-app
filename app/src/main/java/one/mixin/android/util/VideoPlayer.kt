package one.mixin.android.util

import one.mixin.android.util.video.MixinPlayer

class VideoPlayer private constructor() {
    companion object {
        @Synchronized
        fun getInstance(): VideoPlayer {
            if (instance == null) {
                instance =
                    VideoPlayer()
            }
            return instance as VideoPlayer
        }

        fun player(): MixinPlayer {
            return getInstance().player
        }

        fun destroy() {
            instance?.player?.release()
            instance = null
        }

        private var instance: VideoPlayer? = null
    }

    private val player: MixinPlayer = MixinPlayer(false)
}
