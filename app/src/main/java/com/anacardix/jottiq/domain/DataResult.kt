package com.anacardix.jottiq.domain

import kotlinx.coroutines.CancellationException
import java.io.IOException

/** Typed failure the data layer maps every caught exception to — see [runCatchingDataResult]. */
sealed interface DataError {
    data object Io : DataError
    data object Unknown : DataError
}

/** Result of a data-layer write operation; ViewModels map [Failure] to a string-res message id. */
sealed interface DataResult<out T> {
    data class Success<out T>(val value: T) : DataResult<T>
    data class Failure(val error: DataError) : DataResult<Nothing>
}

/**
 * Runs [block], catching and mapping failures into a [DataResult] the way repository
 * implementations are required to (see CLAUDE.md's "Errors" rule). [CancellationException] is
 * always rethrown so cancellation keeps propagating structured concurrency correctly.
 */
@Suppress("TooGenericExceptionCaught", "SwallowedException")
// Catching (and deliberately not rethrowing) every non-cancellation exception is the entire
// point of this function: every repository write must map to a typed DataResult, never throw.
suspend fun <T> runCatchingDataResult(block: suspend () -> T): DataResult<T> = try {
    DataResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: IOException) {
    DataResult.Failure(DataError.Io)
} catch (e: Exception) {
    DataResult.Failure(DataError.Unknown)
}
