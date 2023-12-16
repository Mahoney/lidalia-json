package json

import either.Outcome
import either.failure
import either.map
import either.orThrow
import either.success
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty0

typealias AnyReadOnlyProperty<V> = ReadOnlyProperty<Any, V>
typealias OutcomeReadOnlyProperty<F, S> = AnyReadOnlyProperty<Outcome<F, S>>

sealed interface JsonInterpretationFailure

@Suppress("unused")
data class FailedParse(val key: String, val actualValue: Json<*>) : JsonInterpretationFailure

sealed interface FailedLookupResult : JsonInterpretationFailure

data class UnexpectedType(val key: String, val actualValue: Json<*>) : FailedLookupResult

data class MissingKey(val key: String) : FailedLookupResult

data class NullValue(val key: String) : FailedLookupResult

fun <T> readOnlyProperty(f: (propName: String) -> T) =
  ReadOnlyProperty<Any, T> { _, prop -> f(prop.name) }

fun <V1, V2> OutcomeReadOnlyProperty<FailedLookupResult, V1>.map(
  f: (V1) -> V2,
): ReadOnlyProperty<Any, Outcome<FailedLookupResult, V2>> =
  ReadOnlyProperty { any, name -> getValue(any, name).map(f) }

fun <V> OutcomeReadOnlyProperty<JsonInterpretationFailure, V>.orThrow(): ReadOnlyProperty<Any, V> =
  ReadOnlyProperty {
      any,
      name,
    ->
    getValue(any, name).orThrow {
        f ->
      if (f is Throwable) f else Exception(f.toString())
    }
  }

@Suppress("MemberVisibilityCanBePrivate")
sealed interface Json<T> {
  val value: T

  @JvmInline value class Boolean(override val value: kotlin.Boolean) : Json<kotlin.Boolean>

  @JvmInline value class Number(override val value: kotlin.Number) : Json<kotlin.Number>

  @JvmInline value class String(override val value: kotlin.String) : Json<kotlin.String>

  @Suppress("unused")
  data class Object(
    override val value: List<Pair<kotlin.String, Json<*>?>>,
  ) : List<Pair<kotlin.String, Json<*>?>> by value,
    Map<kotlin.String, Json<*>?>,
    Json<List<Pair<kotlin.String, Json<*>?>>> {

    private val asMap = value.toMap()
    override val entries: Set<Map.Entry<kotlin.String, Json<*>?>> = asMap.entries
    override val keys: Set<kotlin.String> = asMap.keys
    override val values: Collection<Json<*>?> = asMap.values

    override fun containsKey(key: kotlin.String): kotlin.Boolean = asMap.containsKey(key)

    override fun containsValue(value: Json<*>?): kotlin.Boolean = asMap.containsValue(value)

    override operator fun get(key: kotlin.String): Json<*>? = asMap[key]

    fun obj(key: kotlin.String): Outcome<FailedLookupResult, Object> = fetch<Object>(key)

    fun obj() = readOnlyProperty { prop -> obj(prop) }

    fun <T> parseObject(
      f: (List<Pair<kotlin.String, Json<*>?>>) -> T,
    ): ReadOnlyProperty<Any, Outcome<JsonInterpretationFailure, T>> = obj().map {
      f(it.value)
    }

    fun array(key: kotlin.String): Outcome<FailedLookupResult, Array> = fetch<Array>(key)

    fun array() = readOnlyProperty { prop -> array(prop) }

    fun <T> parseArray(
      f: (List<Json<*>?>) -> T,
    ): ReadOnlyProperty<Any, Outcome<JsonInterpretationFailure, T>> = array().map {
      f(it.value)
    }

    fun boolean(key: kotlin.String): Outcome<FailedLookupResult, Boolean> = fetch<Boolean>(key)

    fun boolean() = readOnlyProperty { prop -> boolean(prop) }

    fun <T> parseBoolean(
      f: (kotlin.Boolean) -> T,
    ): ReadOnlyProperty<Any, Outcome<JsonInterpretationFailure, T>> = boolean().map {
      f(it.value)
    }

    fun number(key: kotlin.String): Outcome<FailedLookupResult, Number> = fetch<Number>(key)

    fun number() = readOnlyProperty { prop -> number(prop) }

    fun <T> parseNumber(
      f: (kotlin.Number) -> T,
    ): ReadOnlyProperty<Any, Outcome<JsonInterpretationFailure, T>> = number().map {
      f(it.value)
    }

    fun string(key: kotlin.String): Outcome<FailedLookupResult, String> = fetch<String>(key)

    fun string(): ReadOnlyProperty<Any, Outcome<FailedLookupResult, String>> =
      readOnlyProperty { prop ->
        string(
          prop,
        )
      }

    fun <T> parseString(
      f: (kotlin.String) -> T,
    ): ReadOnlyProperty<Any, Outcome<JsonInterpretationFailure, T>> = string().map {
      f(it.value)
    }

    fun parseString(): ReadOnlyProperty<Any, Outcome<JsonInterpretationFailure, kotlin.String>> =
      parseString(
        ::identity,
      )

    private inline fun <reified T : Json<*>> fetch(
      key: kotlin.String,
    ): Outcome<FailedLookupResult, T> = if (containsKey(key)) {
      when (val candidate = get(key)) {
        is T -> candidate.success()
        null -> NullValue(key).failure()
        else -> UnexpectedType(key, candidate).failure()
      }
    } else {
      MissingKey(key).failure()
    }

    fun <T> obj(mapper: (Object) -> T) = obj().map(mapper)

    fun <T> array(mapper: (Object) -> T) = readOnlyProperty { prop ->
      array(prop).map { jsonArray ->
        jsonArray.map { json ->
          if (json is Object) mapper(json) else null
        }
      }
    }

    fun replace(vararg entries: Pair<kotlin.String, Json<*>?>): Object = replace(entries.toList())

    @Suppress("UNUSED_PARAMETER")
    fun replace(entries: List<Pair<kotlin.String, Json<*>?>>): Object = TODO()
  }

  @JvmInline value class Array(override val value: List<Json<*>?>) :
    List<Json<*>?> by value,
    Json<List<Json<*>?>>
}

interface ToJson<J : Json<J>> {
  fun json(): J
}

fun Boolean.json() = Json.Boolean(this)

fun Number.json() = Json.Number(this)

fun CharSequence.json() = Json.String(this.toString())

fun Map<String, Json<*>?>.json(): Json.Object = Json.Object(this.entries.map { (k, v) -> k to v })

@JvmName("jsonStringToJson?")
fun Map<String, ToJson<*>?>.json(): Json.Object = mapValues { (_, value) -> value?.json() }.json()

@JvmName("jsonStringBoolean?")
fun Map<String, Boolean?>.json(): Json.Object = mapValues { (_, value) -> value?.json() }.json()

@JvmName("jsonStringCharSequence?")
fun Map<String, CharSequence?>.json(): Json.Object = mapValues {
    (_, value) ->
  value?.json()
}.json()

@JvmName("jsonStringString?")
fun Map<String, String?>.json(): Json.Object = mapValues { (_, value) -> value?.json() }.json()

fun List<Json<*>?>.json(): Json.Array = Json.Array(this)

@JvmName("jsonToJson?")
fun List<ToJson<*>?>.json(): Json.Array = map { it?.json() }.json()

@JvmName("jsonBoolean?")
fun List<Boolean?>.json(): Json.Array = map { it?.json() }.json()

@JvmName("jsonBigDecimal?")
fun List<Number?>.json(): Json.Array = map { it?.json() }.json()

@JvmName("jsonString?")
fun List<CharSequence?>.json(): Json.Array = map { it?.json() }.json()

fun json(vararg pairs: Pair<String, Json<*>?>): Json.Object = mapOf(*pairs).json()

@JvmName("json1")
fun json(vararg pairs: Pair<String, ToJson<*>?>): Json.Object = mapOf(*pairs).json()

fun json(vararg xs: Json<*>?): Json.Array = listOf(*xs).json()

@JvmName("json1")
fun json(vararg xs: ToJson<*>?): Json.Array = listOf(*xs).json()

fun <T> KProperty0<T>.json(f: T.() -> Json<*>): Pair<String, Json<*>> = name to get().f()

fun KProperty0<String>.json(): Pair<String, Json<*>> = json(CharSequence::json)

abstract class JsonWrapper(val json: Json.Object) {

  final override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is JsonWrapper) return false
    return json == other.json
  }

  final override fun hashCode(): Int = json.hashCode()

  override fun toString(): String = json.toString()
}

fun <T : Any> T.json(json: T.() -> List<Pair<String, Json<*>?>>): Json.Object = when (this) {
  is JsonWrapper -> this.json
  else -> Json.Object(this.json())
}

fun <T> identity(x: T): T = x
