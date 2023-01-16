package one.mixin.android.startup

import android.content.Context
import androidx.startup.Initializer
import com.google.mlkit.nl.entityextraction.EntityExtractor
import one.mixin.android.util.mlkit.entityInitialize

class MlkitInitializer : Initializer<EntityExtractor> {
    override fun create(context: Context): EntityExtractor {
        return entityInitialize()
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}
