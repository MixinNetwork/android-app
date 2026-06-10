package one.mixin.android.startup

import android.content.Context
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.startup.Initializer

class EmojiAppInitializer : Initializer<EmojiCompat> {
    override fun create(context: Context): EmojiCompat {
        return EmojiCompat.init(BundledEmojiCompatConfig(context))
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}
