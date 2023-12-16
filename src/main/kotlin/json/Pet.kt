package json

import java.time.LocalDate
import java.util.*

sealed interface Pet {
    val id: UUID
    val name: String
    val dateOfBirth: LocalDate

    fun doCopy(
        id: UUID = this.id,
        name: String = this.name,
        dateOfBirth: LocalDate = this.dateOfBirth,
    ): Pet
}

class JsonPet(json: Json.Object) : Pet, JsonWrapper(json) {
    override val id: UUID by json.uuid().orThrow()
    override val name: String by json.parseString { it }.orThrow()
    override val dateOfBirth: LocalDate by json.date().orThrow()

    override fun doCopy(
        id: UUID,
        name: String,
        dateOfBirth: LocalDate,
    ) = JsonPet(json.replace(
        ::id.name to id.json(),
        ::name.name to name.json(),
        ::dateOfBirth.name to dateOfBirth.json(),
    ))
}

@Suppress("unused")
data class DataClassPet(
    override val id: UUID,
    override val name: String,
    override val dateOfBirth: LocalDate,
) : Pet {
    override fun doCopy(
        id: UUID,
        name: String,
        dateOfBirth: LocalDate,
    ): Pet = copy(
        id = id,
        name = name,
        dateOfBirth = dateOfBirth,
    )
}

fun Pet.json(): Json.Object = this.json {
    listOf(
        ::id.json(UUID::json),
        ::name.json(),
        ::dateOfBirth.json(LocalDate::json),
    )
}

class Pets(json: Json.Object) : JsonWrapper(json) {
    val pets: List<JsonPet> by json.array(::JsonPet).map(List<JsonPet?>::filterNotNull).orThrow()
}

class PetWrapper(json: Json.Object) : JsonWrapper(json) {
    val pet: JsonPet by json.obj(::JsonPet).orThrow()
}
