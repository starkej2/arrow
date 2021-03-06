package arrow.typeclasses.suspended

import arrow.Kind
import arrow.typeclasses.Monad
import arrow.typeclasses.MonadError

interface MonadErrorSyntax<F, E> : MonadSyntax<F>, ApplicativeErrorSyntax<F, E>,MonadError<F, E> {

  fun <A> ensure(fa: suspend () -> A, error: () -> E, predicate: (A) -> Boolean): Kind<F, A> =
    run<Monad<F>, Kind<F, A>> { fa.effect().ensure(error, predicate) }

}