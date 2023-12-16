@file:Suppress("unused")

package either

sealed class Either<out L, out R> {
  abstract val isLeft: Boolean
  val isRight: Boolean get() = !isLeft

  abstract fun <L2> mapLeft(f: (L) -> L2): Either<L2, R>

  abstract fun <R2> mapRight(f: (R) -> R2): Either<L, R2>

//  abstract fun <L2, R2> flatMapLeft(f: (L) -> Either<L2, R2>): Either<L2, R2>
//  abstract fun <L2, R2> flatMapRight(f: (R) -> Either<L2, R2>): Either<L2, R2>

  abstract fun leftOrNull(): L?

  abstract fun rightOrNull(): R?

  abstract fun <C> fold(ifLeft: (L) -> C, ifRight: (R) -> C): C

  abstract fun peekLeft(f: (L) -> Unit): Either<L, R>

  abstract fun peekRight(f: (R) -> Unit): Either<L, R>
}

fun <A> identity(a: A): A = a

data class Left<out L>(val value: L) : Either<L, Nothing>() {
  override val isLeft = true

  override fun <L2> mapLeft(f: (L) -> L2): Left<L2> = Left(f(value))

  override fun <R2> mapRight(f: (Nothing) -> R2): Left<L> = this

// cannot compile, see https://youtrack.jetbrains.com/issue/KT-209
//  override fun <L2, R2> flatMapLeft(f: (L) -> Either<L2, R2>): Either<L2, R2> = f(value)

  override fun leftOrNull() = value

  override fun rightOrNull() = null

  override fun <C> fold(ifLeft: (L) -> C, ifRight: (Nothing) -> C): C = mapLeft(ifLeft).join()

  override fun peekLeft(f: (L) -> Unit): Either<L, Nothing> {
    f(value)
    return this
  }

  override fun peekRight(f: (Nothing) -> Unit): Either<L, Nothing> = this
}

fun <L> L.left() = Left(this)

data class Right<out R>(val value: R) : Either<Nothing, R>() {
  override val isLeft = false

  override fun <L2> mapLeft(f: (Nothing) -> L2): Right<R> = this

  override fun <R2> mapRight(f: (R) -> R2): Right<R2> = Right(f(value))

// cannot compile, see https://youtrack.jetbrains.com/issue/KT-209
//  override fun <L2, R2> flatMapRight(f: (R) -> Either<L2, R2>): Either<L2, R2> = f(value)

  override fun leftOrNull() = null

  override fun rightOrNull() = value

  override fun <C> fold(ifLeft: (Nothing) -> C, ifRight: (R) -> C): C = mapRight(ifRight).join()

  override fun peekLeft(f: (Nothing) -> Unit): Either<Nothing, R> = this

  override fun peekRight(f: (R) -> Unit): Either<Nothing, R> {
    f(value)
    return this
  }
}

fun <R> R.right() = Right(this)

inline fun <L, L2, R, R2> Either<L, R>.flatMapLeft(
  f: (L) -> Either<L2, R2>,
): Either<L2, R2> where R : R2 = when (this) {
  is Left -> f(value)
  is Right -> this
}

/**
 * This CANNOT be turned into an abstract member function of `Either` because `L` is covariant, and
 * we cannot (yet) declare that `L : L2`. See https://youtrack.jetbrains.com/issue/KT-209 .
 */
inline fun <L, L2, R, R2> Either<L, R>.flatMapRight(
  f: (R) -> Either<L2, R2>,
): Either<L2, R2> where L : L2 = when (this) {
  is Left -> this
  is Right -> f(value)
}

fun <A> Either<A, A>.join(): A = when (this) {
  is Left<A> -> value
  is Right<A> -> value
}

fun <L, L1 : L, L2 : L, R> Either<L1, Either<L2, R>>.flattenLeft(): Either<L, R> = when (this) {
  is Left -> value.left()
  is Right -> value
}

fun <L, R, R1 : R, R2 : R> Either<Either<L, R1>, R2>.flattenRight(): Either<L, R> = when (this) {
  is Left -> value
  is Right -> value.right()
}
