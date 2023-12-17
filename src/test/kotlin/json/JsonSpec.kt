package json

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.util.UUID

class JsonSpec : StringSpec({

  "parses json to object" {
    val aPet = PetWrapper(
      mapOf(
        "pet" to mapOf(
          "id" to "ba6e5573-06c0-4dd7-99ca-ef481fc2f870".json(),
          "name" to "Scamper".json(),
          "dateOfBirth" to "2018-04-26".json(),
        ).json(),
      ).json(),
    )

    aPet.pet.id shouldBe UUID.fromString("ba6e5573-06c0-4dd7-99ca-ef481fc2f870")
    aPet.pet.name shouldBe "Scamper"
    aPet.pet.dateOfBirth shouldBe LocalDate.of(2018, 4, 26)
  }

  "parses json to list of objects" {
    val pets = Pets(
      mapOf(
        "pets" to listOf(
          mapOf(
            "id" to "ba6e5573-06c0-4dd7-99ca-ef481fc2f870".json(),
            "name" to "Scamper".json(),
            "dateOfBirth" to "2018-04-25".json(),
          ).json(),
        ).json(),
      ).json(),
    )

    pets.pets.first().id shouldBe UUID.fromString("ba6e5573-06c0-4dd7-99ca-ef481fc2f870")
    pets.pets.first().name shouldBe "Scamper"
    pets.pets.first().dateOfBirth shouldBe LocalDate.of(2018, 4, 25)
  }
})
