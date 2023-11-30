package io.mocklab.host.either

sealed class Either<out L, out R> {

  abstract val isLeft: Boolean
  val isRight: Boolean get() = !isLeft

  abstract fun <L2> mapLeft(f: (L) -> L2): Either<L2, R>
  abstract fun <R2> mapRight(f: (R) -> R2): Either<L, R2>

//  abstract fun <L2, R2> flatMapLeft(f: (L) -> Either<L2, R2>): Either<L2, R2>
//  abstract fun <L2, R2> flatMapRight(f: (R) -> Either<L2, R2>): Either<L2, R2>

  abstract fun leftOrNull(): L?
  abstract fun rightOrNull(): R?
}

data class Left<out L>(val value: L) : Either<L, Nothing>() {

  override val isLeft = true

  override fun <L2> mapLeft(f: (L) -> L2): Left<L2> = Left(f(value))
  override fun <R2> mapRight(f: (Nothing) -> R2): Left<L> = this

//  override fun <L2, R2> flatMapLeft(f: (L) -> Either<L2, R2>): Either<L2, R2> = f(value)
  // cannot compile, see https://youtrack.jetbrains.com/issue/KT-209
//  override fun <L2, R2> flatMapRight(f: (Nothing) -> Either<L2, R2>): Left<L> where L : L2 = this

  override fun leftOrNull() = value
  override fun rightOrNull() = null
}

fun <L> L.left() = Left(this)

data class Right<out R>(val value: R) : Either<Nothing, R>() {

  override val isLeft = false

  override fun <L2> mapLeft(f: (Nothing) -> L2): Right<R> = this
  override fun <R2> mapRight(f: (R) -> R2): Right<R2> = Right(f(value))

  // cannot compile, see https://youtrack.jetbrains.com/issue/KT-209
//  override fun <L2, R2> flatMapLeft(f: (Nothing) -> Either<L2, R2>): Right<R> where R : R2 = this
//  override fun <L2, R2> flatMapRight(f: (R) -> Either<L2, R2>): Either<L2, R2> = f(value)

  override fun leftOrNull() = null
  override fun rightOrNull() = value
}

fun <R> R.right() = Right(this)

inline fun <L, L2, R, R2> Either<L, R>.flatMapLeft(
  f: (L) -> Either<L2, R2>
): Either<L2, R2> where R : R2 = when (this) {
  is Right -> this
  is Left -> f(value)
}

/**
 * This CANNOT be turned into an abstract member function of `Either` because `L` is covariant, and
 * we cannot (yet) declare that `L : L2`. See https://youtrack.jetbrains.com/issue/KT-209 .
 */
inline fun <L, L2, R, R2> Either<L, R>.flatMapRight(
  f: (R) -> Either<L2, R2>
): Either<L2, R2> where L : L2 = when (this) {
  is Right -> f(value)
  is Left -> this
}

@Suppress("unused")
fun <A> Either<A, A>.join(): A = when (this) {
  is Left<A> -> value
  is Right<A> -> value
}
