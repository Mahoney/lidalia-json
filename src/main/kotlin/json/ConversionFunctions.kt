package json

import either.Outcome
import java.time.LocalDate
import java.util.UUID
import kotlin.properties.ReadOnlyProperty

fun Json.Object.date(): ReadOnlyProperty<Any, Outcome<JsonInterpretationFailure, LocalDate>> =
  parseString(
    LocalDate::parse,
  )

fun LocalDate.json(): Json.String = toString().json()

fun Json.Object.uuid(): ReadOnlyProperty<Any, Outcome<JsonInterpretationFailure, UUID>> =
  parseString(
    UUID::fromString,
  )

fun UUID.json(): Json.String = toString().json()
