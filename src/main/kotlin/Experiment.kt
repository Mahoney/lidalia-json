import java.time.LocalDate
import java.util.UUID
import kotlin.properties.ReadOnlyProperty

typealias JsonObject = Map<String, Any?>

fun JsonObject.obj(key: String) = get(key) as JsonObject
fun JsonObject.array(key: String) = get(key) as List<JsonObject>
fun JsonObject.string(key: String) = get(key) as String

fun JsonObject.date() = ReadOnlyProperty<Any, LocalDate> { _, prop ->
    LocalDate.parse(string(prop.name))
}

fun JsonObject.uuid() = ReadOnlyProperty<Any, UUID> { _, prop ->
    UUID.fromString(string(prop.name))
}

fun <T> JsonObject.obj(
    mapper: (JsonObject) -> T
) = ReadOnlyProperty<Any, T> { _, prop ->
    mapper(obj(prop.name))
}

fun <T> JsonObject.array(
    mapper: (JsonObject) -> T
) = ReadOnlyProperty<Any, List<T>> { _, prop ->
    array(prop.name).map(mapper)
}

abstract class JsonWrapper(val json: JsonObject) {

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JsonWrapper) return false
        return json == other.json
    }

    final override fun hashCode(): Int = json.hashCode()

    override fun toString(): String = json.toString()
}

class Pet(json: JsonObject) : JsonWrapper(json) {
    val id: UUID by json.uuid()
    val name: String by json
    val dateOfBirth: LocalDate by json.date()
}

class Pets(json: JsonObject) : JsonWrapper(json) {
    val pets: List<Pet> by json.array(::Pet)
}

class PetWrapper(json: JsonObject) : JsonWrapper(json) {
    val pet: Pet by json.obj(::Pet)
}

val aPet = PetWrapper(mapOf(
    "pet" to mapOf(
        "id" to "ba6e5573-06c0-4dd7-99ca-ef481fc2f870",
        "name" to "Rover",
        "dateOfBirth" to "2018-04-25",
    )
))

fun main() {
    println(aPet.pet.dateOfBirth)
}
