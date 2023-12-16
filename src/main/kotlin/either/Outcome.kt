@file:Suppress("NOTHING_TO_INLINE", "unused")

package either

typealias Outcome<F, S> = Either<F, S>

typealias Success<S> = Right<S>

inline fun <S> S.success(): Success<S> = right()

val SUCCESS: Success<Unit> = Unit.success()
val NULL_SUCCESS: Success<Nothing?> = null.success()

typealias Failure<F> = Left<F>

inline fun <F> F.failure(): Failure<F> = left()

val FAILURE: Failure<Unit> = Unit.failure()
val NULL_FAILURE: Failure<Nothing?> = null.failure()

inline val <F, S> Outcome<F, S>.isSuccess: Boolean get() = isRight
inline val <F, S> Outcome<F, S>.isFailure: Boolean get() = isLeft

inline fun <F, S, S2> Outcome<F, S>.map(noinline f: (S) -> S2): Outcome<F, S2> = mapRight(f)

inline fun <F, F2, S> Outcome<F, S>.mapFailure(noinline f: (F) -> F2): Outcome<F2, S> = mapLeft(f)

inline fun <F, F2, S, S2> Outcome<F, S>.flatMap(
  f: (S) -> Outcome<F2, S2>,
): Outcome<F2, S2> where F : F2 = flatMapRight(f)

inline fun <F, F2, S, S2> Outcome<F, S>.flatMapFailure(
  f: (F) -> Outcome<F2, S2>,
): Outcome<F2, S2> where S : S2 = flatMapLeft(f)

inline fun <F, S> Outcome<F, S>.orNull(): S? = rightOrNull()

inline fun <F, T, S : T, F2 : T> Outcome<F, S>.or(noinline f: (F) -> F2): T = mapFailure(f).join()

inline fun <F, S> Outcome<F, S>.peek(noinline f: (S) -> Unit): Outcome<F, S> = peekRight(f)

inline fun <F, S> Outcome<F, S>.peekFailure(noinline f: (F) -> Unit): Outcome<F, S> = peekLeft(f)

inline fun <F, F1 : F, F2 : F, R> Outcome<F1, Outcome<F2, R>>.flatten(): Outcome<F, R> =
  flattenLeft()

fun <F : Throwable, S> Outcome<F, S>.orThrow(): S = when (this) {
  is Failure -> throw value
  is Success -> value
}

fun <F, S> Outcome<F, S>.orThrow(f: (F) -> Throwable): S = mapFailure(f).orThrow()

inline fun <reified F, S> catching(action: () -> S): Outcome<F, S> = try {
  action().success()
} catch (t: Throwable) {
  if (t is F) {
    t.failure()
  } else {
    throw t
  }
}

inline fun <S> outcomeOf(action: () -> S): Outcome<Exception, S> = try {
  action().success()
} catch (t: Throwable) {
  when (t) {
    is InterruptedException -> throw t
    is StackOverflowError -> Exception(t.message, t).failure()
    is Exception -> t.failure()
    else -> throw t
  }
}

fun <F, S, S1, S2> ifAllSuccess(
  outcome1: Outcome<F, S1>,
  outcome2: Outcome<F, S2>,
  f: (S1, S2) -> Outcome<F, S>,
): Outcome<F, S> = if (outcome1 is Success && outcome2 is Success) {
  f(outcome1.value, outcome2.value)
} else {
  listOf(outcome1, outcome2).firstNotNullOf { it as? Failure }
}
