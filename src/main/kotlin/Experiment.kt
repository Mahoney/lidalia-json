import java.time.LocalDate
import java.util.UUID
import kotlin.properties.ReadOnlyProperty

typealias JsonObject = Map<String, Any?>

fun JsonObject.obj(key: String) = get(key) as JsonObject
fun JsonObject.array(key: String) = get(key) as List<JsonObject>
fun JsonObject.string(key: String) = get(key) as String

private fun <T> readOnlyProperty(
    f: (propName: String) -> T
) = ReadOnlyProperty<Any, T> { _, prop -> f(prop.name) }

fun JsonObject.date() = readOnlyProperty { prop ->
    LocalDate.parse(string(prop))
}

fun JsonObject.uuid() = readOnlyProperty { prop ->
    UUID.fromString(string(prop))
}

fun <T> JsonObject.obj(
    mapper: (JsonObject) -> T
) = readOnlyProperty { prop ->
    mapper(obj(prop))
}

fun <T> JsonObject.array(
    mapper: (JsonObject) -> T
) = readOnlyProperty { prop ->
    array(prop).map(mapper)
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
