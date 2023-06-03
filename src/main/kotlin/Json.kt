sealed interface Json {
    @JvmInline value class Boolean(val value: kotlin.Boolean) : Json
    @JvmInline value class Number(val value: kotlin.Number) : Json
    @JvmInline value class String(val value: kotlin.String) : Json
    @JvmInline value class Object(private val value: Map<kotlin.String, Json?>) : Map<kotlin.String, Json?> by value, Json
    @JvmInline value class Array(private val value: List<Json?>) : List<Json?> by value, Json
}

interface ToJson {
    fun json(): Json.Object
}

fun Boolean.json() = Json.Boolean(this)
fun Number.json() = Json.Number(this)
fun CharSequence.json() = Json.String(this.toString())

fun Map<String, Json?>.json(): Json.Object = Json.Object(this)
@JvmName("jsonStringToJson?")
fun Map<String, ToJson?>.json(): Json.Object = mapValues { (_, value) -> value?.json() }.json()
@JvmName("jsonStringBoolean?")
fun Map<String, Boolean?>.json(): Json.Object = mapValues { (_, value) -> value?.json() }.json()
@JvmName("jsonStringCharSequence?")
fun Map<String, CharSequence?>.json(): Json.Object = mapValues { (_, value) -> value?.json() }.json()
@JvmName("jsonStringString?")
fun Map<String, String?>.json(): Json.Object = mapValues { (_, value) -> value?.json() }.json()

fun List<Json?>.json(): Json.Array = Json.Array(this)
@JvmName("jsonToJson?")
fun List<ToJson?>.json(): Json.Array = map { it?.json() }.json()
@JvmName("jsonBoolean?")
fun List<Boolean?>.json(): Json.Array = map { it?.json() }.json()
@JvmName("jsonBigDecimal?")
fun List<Number?>.json(): Json.Array = map { it?.json() }.json()
@JvmName("jsonString?")
fun List<CharSequence?>.json(): Json.Array = map { it?.json() }.json()

fun json(vararg pairs: Pair<String, Json?>): Json.Object = mapOf(*pairs).json()
@JvmName("json1")
fun json(vararg pairs: Pair<String, ToJson?>): Json.Object = mapOf(*pairs).json()


fun json(vararg xs: Json?): Json.Array = listOf(*xs).json()
@JvmName("json1")
fun json(vararg xs: ToJson?): Json.Array = listOf(*xs).json()
