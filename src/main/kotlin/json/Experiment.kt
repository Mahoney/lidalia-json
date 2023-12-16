package json

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
