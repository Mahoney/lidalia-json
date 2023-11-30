package json

import io.mocklab.host.either.Either
import java.time.LocalDate
import java.util.UUID
import kotlin.properties.ReadOnlyProperty

fun Json.Object.date(): ReadOnlyProperty<Any, Either<FailedLookupResult, LocalDate>> = string().map { LocalDate.parse(it.value) }

fun Json.Object.uuid(): ReadOnlyProperty<Any, Either<FailedLookupResult, UUID>> = string().map { UUID.fromString(it.value) }


abstract class JsonWrapper(val json: Json.Object) {

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JsonWrapper) return false
        return json == other.json
    }

    final override fun hashCode(): Int = json.hashCode()

    override fun toString(): String = json.toString()
}

class Pet(json: Json.Object) : JsonWrapper(json) {
    val id: UUID by json.uuid()
    val name: String by json.string()
    val dateOfBirth: LocalDate by json.date()
}

class Pets(json: Json.Object) : JsonWrapper(json) {
    val pets: List<Pet> by json.array(::Pet).map { it.filterNotNull() }
}

class PetWrapper(json: Json.Object) : JsonWrapper(json) {
    val pet: Pet by json.obj(::Pet)
}

val aPet = PetWrapper(mapOf(
    "pet" to mapOf(
        "id" to "ba6e5573-06c0-4dd7-99ca-ef481fc2f870".json(),
        "name" to "Rover".json(),
        "dateOfBirth" to "2018-04-25".json(),
    ).json()
).json())

val pets = Pets(mapOf(
    "pets" to listOf(
        mapOf(
            "id" to "ba6e5573-06c0-4dd7-99ca-ef481fc2f870".json(),
            "name" to "Rover".json(),
            "dateOfBirth" to "2018-04-25".json(),
        ).json()
    ).json()
).json())

fun main() {
    println(aPet.pet.dateOfBirth)
    println(pets.pets.first().dateOfBirth)
}
