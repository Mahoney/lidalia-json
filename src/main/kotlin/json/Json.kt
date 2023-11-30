package json

import io.mocklab.host.either.*
import kotlin.properties.ReadOnlyProperty

sealed interface JsonInterpretationFailure
sealed interface FailedLookupResult : JsonInterpretationFailure
data class UnexpectedType(val key: String, val actualValue: Json<*>): FailedLookupResult
data class MissingKey(val key: String): FailedLookupResult
data class NullValue(val key: String): FailedLookupResult


fun <T> readOnlyProperty(
    f: (propName: String) -> T
) = ReadOnlyProperty<Any, T> { _, prop -> f(prop.name) }

//fun <V1, V2> ReadOnlyProperty<Any, V1>.map(f: (V1) -> V2): ReadOnlyProperty<Any, V2> =
//    ReadOnlyProperty { any, name -> f(getValue(any, name)) }

fun <V1, V2> ReadOnlyProperty<Any, Outcome<FailedLookupResult, V1>>.map(f: (V1) -> V2): ReadOnlyProperty<Any, Outcome<FailedLookupResult, V2>> =
    ReadOnlyProperty { any, name -> getValue(any, name).map(f) }

sealed interface Json<T> {
    val value: T
    @JvmInline value class Boolean(override val value: kotlin.Boolean) : Json<kotlin.Boolean>
    @JvmInline value class Number(override val value: kotlin.Number) : Json<kotlin.Number>
    @JvmInline value class String(override val value: kotlin.String) : Json<kotlin.String>
    data class Object(
        override val value: List<Pair<kotlin.String, Json<*>?>>
    ) : List<Pair<kotlin.String, Json<*>?>> by value, Map<kotlin.String, Json<*>?>, Json<List<Pair<kotlin.String, Json<*>?>>> {
        private val asMap = value.toMap()
        override val entries: Set<Map.Entry<kotlin.String, Json<*>?>> = asMap.entries
        override val keys: Set<kotlin.String> = asMap.keys
        override val values: Collection<Json<*>?> = asMap.values
        override fun containsKey(key: kotlin.String): kotlin.Boolean = asMap.containsKey(key)
        override fun containsValue(value: Json<*>?): kotlin.Boolean = asMap.containsValue(value)
        override operator fun get(key: kotlin.String): Json<*>? = asMap[key]

        fun obj(key: kotlin.String): Outcome<FailedLookupResult, Object> = fetch<Object>(key)
        fun obj() = readOnlyProperty { prop -> obj(prop) }

        fun array(key: kotlin.String): Outcome<FailedLookupResult, Array> = fetch<Array>(key)
        fun array() = readOnlyProperty { prop -> array(prop) }

        fun boolean(key: kotlin.String): Outcome<FailedLookupResult, Boolean> = fetch<Boolean>(key)
        fun boolean() = readOnlyProperty { prop -> boolean(prop) }

        fun number(key: kotlin.String): Outcome<FailedLookupResult, Number> = fetch<Number>(key)
        fun number() = readOnlyProperty { prop -> number(prop) }

        fun string(key: kotlin.String): Outcome<FailedLookupResult, String> = fetch<String>(key)
        fun string() = readOnlyProperty { prop -> string(prop) }

        private inline fun <reified T : Json<*>> fetch(key: kotlin.String): Outcome<FailedLookupResult, T> =
            if (containsKey(key)) {
                when (val candidate = get(key)) {
                    is T -> candidate.success()
                    null -> NullValue(key).failure()
                    else -> UnexpectedType(key, candidate).failure()
                }
            } else {
                MissingKey(key).failure()
            }

        fun <T> obj(mapper: (Object) -> T) = obj().map(mapper)

        fun <T> array(
            mapper: (Object) -> T
        ) = readOnlyProperty { prop ->
            array(prop).map { jsonArray ->
                jsonArray.map { json ->
                    if (json is Object) mapper(json) else null }
                }
        }
    }

    @JvmInline value class Array(override val value: List<Json<*>?>) : List<Json<*>?> by value,
        Json<List<Json<*>?>>
}

interface ToJson {
    fun json(): Json.Object
}

fun Boolean.json() = Json.Boolean(this)
fun Number.json() = Json.Number(this)
fun CharSequence.json() = Json.String(this.toString())

fun Map<String, Json<*>?>.json(): Json.Object =
    Json.Object(this.entries.map { (k, v) -> k to v })

@JvmName("jsonStringToJson?")
fun Map<String, ToJson?>.json(): Json.Object = mapValues { (_, value) -> value?.json() }.json()
@JvmName("jsonStringBoolean?")
fun Map<String, Boolean?>.json(): Json.Object = mapValues { (_, value) -> value?.json() }.json()
@JvmName("jsonStringCharSequence?")
fun Map<String, CharSequence?>.json(): Json.Object = mapValues { (_, value) -> value?.json() }.json()
@JvmName("jsonStringString?")
fun Map<String, String?>.json(): Json.Object = mapValues { (_, value) -> value?.json() }.json()

fun List<Json<*>?>.json(): Json.Array = Json.Array(this)
@JvmName("jsonToJson?")
fun List<ToJson?>.json(): Json.Array = map { it?.json() }.json()
@JvmName("jsonBoolean?")
fun List<Boolean?>.json(): Json.Array = map { it?.json() }.json()
@JvmName("jsonBigDecimal?")
fun List<Number?>.json(): Json.Array = map { it?.json() }.json()
@JvmName("jsonString?")
fun List<CharSequence?>.json(): Json.Array = map { it?.json() }.json()

fun json(vararg pairs: Pair<String, Json<*>?>): Json.Object = mapOf(*pairs).json()
@JvmName("json1")
fun json(vararg pairs: Pair<String, ToJson?>): Json.Object = mapOf(*pairs).json()


fun json(vararg xs: Json<*>?): Json.Array = listOf(*xs).json()
@JvmName("json1")
fun json(vararg xs: ToJson?): Json.Array = listOf(*xs).json()
