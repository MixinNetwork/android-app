package one.mixin.android

import com.google.gson.annotations.SerializedName
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.response.perps.PerpsFavorite
import one.mixin.android.api.response.perps.PerpsMarketCategoryRelation
import one.mixin.android.crypto.Base64
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.Serializable
import java.lang.reflect.Modifier
import java.util.jar.JarFile

class ReleaseKeepRulesTest {
    @Test
    fun nonPublicSerializableTypesAreRuntimeClassesOrExplicitlyKept() {
        val violations = appClasses.filter { type ->
            Serializable::class.java.isAssignableFrom(type) &&
                !Modifier.isPublic(type.modifiers) &&
                !Enum::class.java.isAssignableFrom(type) &&
                !Throwable::class.java.isAssignableFrom(type) &&
                !isRuntimeClass(type)
        }

        assertTrue("Add explicit serialization keep rules for ${violations.map { it.name }}", violations.isEmpty())
        val persistedCallbacks = appClasses
            .filter { Serializable::class.java.isAssignableFrom(it) && !Enum::class.java.isAssignableFrom(it) && !isRuntimeClass(it) }
            .flatMap { it.declaredFields.toList() }
            .filter { field ->
                !Modifier.isStatic(field.modifiers) && !Modifier.isTransient(field.modifiers) &&
                    (kotlin.Function::class.java.isAssignableFrom(field.type) ||
                        kotlin.coroutines.Continuation::class.java.isAssignableFrom(field.type))
            }
        assertTrue("Persisted callbacks require stable names: $persistedCallbacks", persistedCallbacks.isEmpty())
    }

    @Test
    fun apiFieldsHaveStableJsonNamesOrExplicitKeepRules() {
        val retained = setOf(PerpsFavorite::class.java, PerpsMarketCategoryRelation::class.java)
        val violations = appClasses
            .filter { it.name.startsWith("one.mixin.android.api.request.") || it.name.startsWith("one.mixin.android.api.response.") }
            .filterNot { it in retained || Enum::class.java.isAssignableFrom(it) }
            .flatMap { it.declaredFields.toList() }
            .filter { field ->
                !field.isSynthetic && !Modifier.isStatic(field.modifiers) && !Modifier.isTransient(field.modifiers) &&
                    field.getAnnotation(SerializedName::class.java) == null
            }

        assertTrue("API fields need SerializedName or explicit keep rules: $violations", violations.isEmpty())
    }

    private fun isRuntimeClass(type: Class<*>): Boolean =
        generateSequence<Class<*>>(type) { it.superclass }.any {
            it.name in setOf(
                "kotlin.coroutines.jvm.internal.BaseContinuationImpl",
                "kotlin.jvm.internal.CallableReference",
                "kotlin.jvm.internal.AdaptedFunctionReference",
                "kotlin.jvm.internal.Lambda",
            )
        }

    companion object {
        private val appClasses by lazy {
            val names = setOf(TransferRequest::class.java, Base64::class.java)
                .map { File(it.protectionDomain.codeSource.location.toURI()) }
                .distinct()
                .flatMap { source ->
                    if (source.isDirectory) {
                        source.resolve("one/mixin/android").walkTopDown()
                            .filter { it.isFile && it.extension == "class" }
                            .map { it.relativeTo(source).invariantSeparatorsPath }
                            .toList()
                    } else {
                        JarFile(source).use { jar ->
                            jar.entries().asSequence().map { it.name }
                                .filter { it.startsWith("one/mixin/android/") && it.endsWith(".class") }
                                .toList()
                        }
                    }
                }.distinct()
            assertTrue("Expected compiled app classes", names.size > 1000)
            names.map { Class.forName(it.removeSuffix(".class").replace('/', '.'), false, TransferRequest::class.java.classLoader) }
        }
    }
}
