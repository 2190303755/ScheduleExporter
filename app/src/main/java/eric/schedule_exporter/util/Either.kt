package eric.schedule_exporter.util


fun <T> Either<T, T>.unbox(): T = when (this) {
    is Either.Left -> this.value
    is Either.Right -> this.value
}

sealed interface Either<out L, out R> {
    @JvmInline
    value class Left<L>(val value: L) : Either<L, Nothing> {
        override fun <T> mapLeft(mapping: (L) -> T) = Left(mapping(this.value))

        override fun <T> mapRight(mapping: (Nothing) -> T) = this
    }

    @JvmInline
    value class Right<R>(val value: R) : Either<Nothing, R> {
        override fun <T> mapLeft(mapping: (Nothing) -> T) = this

        override fun <T> mapRight(mapping: (R) -> T) = Right(mapping(this.value))
    }

    fun <T> mapLeft(mapping: (L) -> T): Either<T, R>
    fun <T> mapRight(mapping: (R) -> T): Either<L, T>
}