package one.mixin.android

import android.app.Application
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.SerializedName
import one.mixin.android.ui.wallet.alert.vo.AlertUpdateRequest
import one.mixin.android.util.GsonHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.http.Body
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [33])
class ApiJsonContractTest {
    @Test
    fun exportAllApiJsonContracts() {
        val contracts = JsonArray()
        val models = linkedSetOf<Class<*>>()
        var endpoints = 0
        ReleaseKeepRulesTest.appClasses.filter { it.isInterface }.sortedBy { it.name }.forEach { service ->
            service.declaredMethods.filter { method ->
                method.annotations.any { it.annotationClass.java.packageName == "retrofit2.http" }
            }.sortedBy { it.toGenericString() }.forEach { method ->
                endpoints++
                val custom = service.simpleName !in setOf("RouteService", "GiphyService", "FoursquareService", "CashService", "EarnService", "ReferralService")
                val gson = if (custom) GsonHelper.customGson else Gson()
                val types = method.genericParameterTypes.filterIndexed { index, _ ->
                    method.parameterAnnotations[index].any { it is Body }
                } + unwrap(method.genericParameterTypes.lastOrNull()?.takeIf {
                    it is ParameterizedType && it.rawType == kotlin.coroutines.Continuation::class.java
                } ?: method.genericReturnType)
                types.forEachIndexed { index, type ->
                    val input = sample(type, custom, emptyMap(), emptySet(), models)
                    val value = gson.fromJson<Any>(input, type)
                    val expected = gson.toJsonTree(value, type)
                    contracts.add(JsonObject().apply {
                        addProperty("endpoint", "${service.simpleName}.${method.name}:$index")
                        addProperty("customGson", custom)
                        add("type", typeDescription(type))
                        addProperty("input", input.toString())
                        add("expected", expected)
                    })
                }
            }
        }
        assertTrue("No Retrofit endpoints found", endpoints > 0)
        assertTrue("Price alert update must be discovered through Retrofit", AlertUpdateRequest::class.java in models)
        val keptFields = Regex("""-keepclassmembers,allowoptimization class ([^{]+)\{\s*!transient !static(?: !synthetic)? <fields>;\s*}""")
            .findAll(File("proguard-rules.pro").readText())
            .map { it.groupValues[1].split(',').map(String::trim) }
            .toList()
        fun matches(name: String, selector: String) = if (selector.endsWith(".**")) name.startsWith(selector.removeSuffix("**")) else name == selector
        val unstable = models.flatMap(::jsonFields).filter { field ->
            field.getAnnotation(SerializedName::class.java) == null && keptFields.none { selectors ->
                selectors.any { !it.startsWith('!') && matches(field.declaringClass.name, it) } &&
                    selectors.none { it.startsWith('!') && matches(field.declaringClass.name, it.drop(1)) }
            }
        }
        assertTrue("API fields need SerializedName or explicit field keep rules: $unstable", unstable.isEmpty())
        val fields = JsonArray()
        models.sortedBy { it.name }.forEach { model ->
            jsonFields(model).forEach { field ->
                fields.add(JsonObject().apply {
                    addProperty("owner", field.declaringClass.name)
                    addProperty("name", field.name)
                    addProperty("type", field.genericType.typeName)
                })
            }
        }
        val report = JsonObject().apply {
            addProperty("endpoints", endpoints)
            addProperty("models", models.size)
            add("fields", fields)
            add("contracts", contracts)
        }
        File("build/reports/api-json-contracts.json").apply {
            parentFile.mkdirs()
            writeText(Gson().toJson(report))
        }
        println("API JSON contracts: $endpoints endpoints, ${models.size} models, ${fields.size()} fields, ${contracts.size()} payloads")
    }

    @Test
    fun priceAlertActionsUseServerJsonNames() {
        val gson = Gson()
        for (action in listOf("delete", "pause", "resume")) {
            assertEquals("{\"action\":\"$action\"}", gson.toJson(AlertUpdateRequest(action = action)))
        }
        assertEquals(
            "{\"type\":\"price_reached\",\"frequency\":\"once\",\"value\":\"123.45\",\"action\":\"update\"}",
            gson.toJson(AlertUpdateRequest("price_reached", "once", "123.45", "update")),
        )
    }

    private fun unwrap(type: Type): Type = when (type) {
        is WildcardType -> unwrap((type.lowerBounds + type.upperBounds).first())
        is ParameterizedType -> if ((type.rawType as Class<*>).name in setOf(
                "kotlin.coroutines.Continuation", "kotlinx.coroutines.Deferred", "retrofit2.Call", "retrofit2.Response",
                "io.reactivex.Observable", "io.reactivex.Single", "io.reactivex.Flowable", "androidx.lifecycle.LiveData",
            )
        ) unwrap(type.actualTypeArguments.single()) else type
        else -> type
    }

    private fun typeDescription(type: Type): JsonObject = when (type) {
        is WildcardType -> typeDescription((type.lowerBounds + type.upperBounds).first())
        is ParameterizedType -> typeDescription(type.rawType).apply {
            add("arguments", JsonArray().apply { type.actualTypeArguments.forEach { add(typeDescription(it)) } })
        }
        is GenericArrayType -> JsonObject().apply { add("component", typeDescription(type.genericComponentType)) }
        is Class<*> -> if (type.isArray) JsonObject().apply { add("component", typeDescription(type.componentType)) }
            else JsonObject().apply { addProperty("name", type.name) }
        else -> error("Unsupported API type: $type")
    }

    private fun sample(type: Type, custom: Boolean, bindings: Map<TypeVariable<*>, Type>, path: Set<Type>, models: MutableSet<Class<*>>): JsonElement {
        if (type in path) return JsonNull.INSTANCE
        val next = path + type
        if (type is WildcardType) return sample((type.lowerBounds + type.upperBounds).first(), custom, bindings, path, models)
        if (type is TypeVariable<*>) return sample(bindings[type] ?: Any::class.java, custom, bindings, path, models)
        if (type is GenericArrayType) return JsonArray().apply { add(sample(type.genericComponentType, custom, bindings, next, models)) }
        val raw = (if (type is ParameterizedType) type.rawType else type) as Class<*>
        val arguments = (type as? ParameterizedType)?.actualTypeArguments.orEmpty()
        val resolved = bindings + raw.typeParameters.zip(arguments)
        if (raw == ByteArray::class.java && custom) return JsonPrimitive("AQID")
        if (raw.isArray) return JsonArray().apply { add(sample(raw.componentType, custom, resolved, next, models)) }
        if (Collection::class.java.isAssignableFrom(raw)) return JsonArray().apply {
            add(sample(arguments.firstOrNull() ?: Any::class.java, custom, resolved, next, models))
        }
        if (Map::class.java.isAssignableFrom(raw)) return JsonObject().apply {
            add("sample", sample(arguments.getOrNull(1) ?: Any::class.java, custom, resolved, next, models))
        }
        if (raw == String::class.java || raw == Any::class.java || raw == Char::class.java || raw == java.lang.Character::class.java) return JsonPrimitive("x")
        if (raw == Boolean::class.java || raw == java.lang.Boolean::class.java) return JsonPrimitive(true)
        if (raw.isPrimitive || Number::class.java.isAssignableFrom(raw)) return JsonPrimitive(7)
        if (raw == Void::class.java || raw == Unit::class.java) return JsonNull.INSTANCE
        if (JsonElement::class.java.isAssignableFrom(raw)) return if (raw == JsonArray::class.java) JsonArray().apply { add("sample") }
            else JsonObject().apply { addProperty("sample", "value") }
        if (raw.isEnum) {
            val constant = raw.enumConstants.first() as Enum<*>
            if (custom && raw.name == "one.mixin.android.vo.WithdrawalMemoPossibility") return JsonPrimitive("negative")
            return JsonPrimitive(raw.getField(constant.name).getAnnotation(SerializedName::class.java)?.value ?: constant.name)
        }
        require(raw.name.startsWith("one.mixin.android.")) { "Add an API fixture for ${raw.name}" }
        models.add(raw)
        return JsonObject().apply {
            jsonFields(raw).forEach { field ->
                add(field.getAnnotation(SerializedName::class.java)?.value ?: field.name, sample(field.genericType, custom, resolved, next, models))
            }
        }
    }

    private fun jsonFields(type: Class<*>): List<Field> = generateSequence(type) { it.superclass }
        .flatMap { it.declaredFields.asSequence() }
        .filter { !it.isSynthetic && !Modifier.isStatic(it.modifiers) && !Modifier.isTransient(it.modifiers) }
        .toList()
}
